package no.nav.tsm

import arrow.core.left
import arrow.core.right
import io.kotest.matchers.equals.shouldEqual
import io.ktor.server.plugins.di.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.util.*
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import no.nav.tsm.pdf.TypstClient
import no.nav.tsm.sykmelding.dokarkiv.DokarkivClient
import no.nav.tsm.sykmelding.journalpost.JournalpostResponse
import no.nav.tsm.sykmelding.kafka.JournalpostOpprettetRecord
import no.nav.tsm.utils.*
import org.testcontainers.kafka.ConfluentKafkaContainer
import testUtils.WithKafka
import testUtils.consumeUntil
import testUtils.produce

class ApplicationTest : WithKafka() {

    @BeforeTest
    fun setup() {
        recreateTopics()
    }

    @Test
    fun `should consume, create PDF, update dokarkiv and produce journalpost record`() = testApplication {
        val mockedDokarkiv = mockk<DokarkivClient>()

        application {
            dependencies {
                provide<Environment>() { createIntegrationEnvironment(kafka) }
                provide<TypstClient>() {
                    TypstClient(
                        typstBinaryPath = "typst-pdf/typst",
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

        kafka.produce(
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
        val mockedDokarkiv = mockk<DokarkivClient>()

        application {
            dependencies {
                provide<Environment>() { createIntegrationEnvironment(kafka) }
                provide<TypstClient>() {
                    TypstClient(
                        typstBinaryPath = "typst-pdf/typst",
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

        kafka.produce(
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

private fun createIntegrationEnvironment(kafka: ConfluentKafkaContainer) =
    Environment(
        runtime = Runtime(env = RuntimeEnvironments.DEV, name = "test-app"),
        kafka =
            KafkaConfig(
                config = Properties().apply { this["bootstrap.servers"] = kafka.bootstrapServers },
                sykmeldingConsumer =
                    KafkaSykmeldingConsumer(
                        longPoll = 1000.milliseconds,
                        retryDelay = 1000.milliseconds,
                    ),
            ),
        external = { mockk() },
        bucket = "fake-bucket",
    )
