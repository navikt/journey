package no.nav.tsm

import arrow.core.left
import arrow.core.right
import io.kotest.matchers.equals.shouldEqual
import io.ktor.server.plugins.di.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.Duration
import java.util.*
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import no.nav.tsm.ktor.kafka.test.KafkaContainer
import no.nav.tsm.ktor.kafka.test.send
import no.nav.tsm.ktor.nais.RuntimeCluster
import no.nav.tsm.pdf.TypstClient
import no.nav.tsm.pdf.getTypstBinaryPath
import no.nav.tsm.sykmelding.dokarkiv.DokarkivClient
import no.nav.tsm.sykmelding.journalpost.JournalpostResponse
import no.nav.tsm.sykmelding.kafka.JournalpostOpprettetRecord
import no.nav.tsm.utils.Environment
import no.nav.tsm.utils.KafkaConfig
import no.nav.tsm.utils.KafkaSykmeldingConsumer
import no.nav.tsm.utils.Runtime
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.StringDeserializer
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue

class ApplicationTest {

    val kafka = KafkaContainer(createTopics = listOf("teamsykmelding.oppgave-journal-opprettet", "tsm.sykmeldinger"))
    val producer = kafka.createAnythingProducer()

    @Test
    fun `should consume, create PDF, update dokarkiv and produce journalpost record`() = testApplication {
        kafka.configureKafka(this)
        val mockedDokarkiv = mockk<DokarkivClient>()

        application {
            dependencies {
                provide<Environment>() { createIntegrationEnvironment() }
                provide<TypstClient>() {
                    TypstClient(
                        typstBinaryPath = getTypstBinaryPath(),
                        templatePath = "typst-pdf/sykmelding.typ",
                        fontPath = "typst-pdf/fonts",
                    )
                }
                provide<DokarkivClient>() { mockedDokarkiv }
            }

            module()
        }

        startApplication()

        coEvery { mockedDokarkiv.createJournalpost(any()) } answers
            {
                JournalpostResponse(
                        dokumenter = emptyList(),
                        journalpostId = "123",
                        journalpostferdigstilt = true,
                        journalstatus = null,
                        melding = null,
                    )
                    .right()
            }

        producer.send(
            "tsm.sykmeldinger",
            "22dfdd7e-7f78-43c7-b5fa-0329db943bfb",
            getFullDigitalSykmeldingExample(),
        )

        val record =
            kafka.consumeUntil<JournalpostOpprettetRecord>(
                "teamsykmelding.oppgave-journal-opprettet",
                want = { it.journalpostId == "123" },
                timeout = java.time.Duration.ofSeconds(20),
            )

        coVerify(exactly = 1) { mockedDokarkiv.createJournalpost(any()) }

        record.journalpostId shouldEqual "123"
        record.journalpostKilde shouldEqual "AS36"
    }

    @Test
    fun `failing to create journalpost should not commit and gracefully retry later`() = testApplication {
        kafka.configureKafka(this)
        val mockedDokarkiv = mockk<DokarkivClient>()

        application {
            dependencies {
                provide<Environment>() { createIntegrationEnvironment() }
                provide<TypstClient>() {
                    TypstClient(
                        typstBinaryPath = getTypstBinaryPath(),
                        templatePath = "typst-pdf/sykmelding.typ",
                        fontPath = "typst-pdf/fonts",
                    )
                }
                provide<DokarkivClient>() { mockedDokarkiv }
            }

            module()
        }

        startApplication()

        coEvery { mockedDokarkiv.createJournalpost(any()) } answers
            {
                DokarkivClient.JournalpostError.PERSON_NOT_FOUND.left()
            } andThen
            JournalpostResponse(
                    dokumenter = emptyList(),
                    journalpostId = "999",
                    journalpostferdigstilt = true,
                    journalstatus = null,
                    melding = null,
                )
                .right()

        producer.send(
            "tsm.sykmeldinger",
            "22dfdd7e-7f78-43c7-b5fa-0329db943bfb",
            getFullDigitalSykmeldingExample(),
        )

        val record =
            kafka.consumeUntil<JournalpostOpprettetRecord>(
                "teamsykmelding.oppgave-journal-opprettet",
                want = { it.journalpostId == "999" },
                timeout = java.time.Duration.ofSeconds(20),
            )

        coVerify(exactly = 2) { mockedDokarkiv.createJournalpost(any()) }

        record.journalpostId shouldEqual "999"
        record.journalpostKilde shouldEqual "AS36"
    }
}

private fun getFullDigitalSykmeldingExample() =
    object {}.javaClass.getResourceAsStream("/digital-full.json")!!.readBytes()

private fun createIntegrationEnvironment() =
    Environment(
        runtime = Runtime(env = RuntimeCluster.DEV, name = "test-app"),
        kafka =
            KafkaConfig(
                sykmeldingConsumer =
                    KafkaSykmeldingConsumer(
                        longPoll = 1000.milliseconds,
                        retryDelay = 1000.milliseconds,
                    )
            ),
        external = { mockk() },
        bucket = "fake-bucket",
    )

private suspend inline fun <reified T> KafkaContainer.consumeUntil(
    topic: String,
    crossinline want: (record: T) -> Boolean,
    timeout: Duration = Duration.ofSeconds(10),
): T {
    val consumerObjectMapper = jacksonObjectMapper()

    return withContext(Dispatchers.IO) {
        val props =
            this@consumeUntil.config +
                mapOf(
                    ConsumerConfig.GROUP_ID_CONFIG to "test-${UUID.randomUUID()}",
                    ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
                )

        KafkaConsumer(props, StringDeserializer(), ByteArrayDeserializer()).use { consumer ->
            consumer.subscribe(listOf(topic))
            val deadline = System.nanoTime() + timeout.toNanos()
            while (System.nanoTime() < deadline) {
                val records = runInterruptible { consumer.poll(Duration.ofMillis(200)) }
                for (record in records) {
                    val value: T = consumerObjectMapper.readValue<T>(record.value())
                    val doWeWant = want(value)

                    if (doWeWant) return@withContext value
                }
            }
            throw AssertionError("Timed out waiting for message on topic=$topic")
        }
    }
}
