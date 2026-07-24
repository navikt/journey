package no.nav.tsm.sykmelding.kafka

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.util.Properties
import kotlin.collections.set
import no.nav.tsm.utils.Environment
import no.nav.tsm.utils.logger
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.Serializer
import org.apache.kafka.common.serialization.StringSerializer

data class JournalpostOpprettetRecord(
    val messageId: String,
    val journalpostId: String,
    val journalpostKilde: String,
)

class JournalOpprettetProducer(environment: Environment) {
    private val logger = logger()
    private val topicName = "teamsykmelding.oppgave-journal-opprettet"

    private val producer: KafkaProducer<String, JournalpostOpprettetRecord>

    init {
        val kafkaProperties = Properties()

        kafkaProperties.apply {
            environment.kafka.config.forEach { (key, value) -> this[key] = value }
            this[ProducerConfig.CLIENT_ID_CONFIG] = "${environment.runtime.name}-producer"
            this[ProducerConfig.ACKS_CONFIG] = "all"
            this[ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG] = "true"
            this[ProducerConfig.COMPRESSION_TYPE_CONFIG] = "gzip"
        }

        println("Kafka producer properties: $kafkaProperties")

        producer = KafkaProducer(kafkaProperties, StringSerializer(), JournalKafkaMessageSerializer())
    }

    fun opprettJournalpostRecord(journalOpprettet: JournalpostOpprettetRecord) {
        val record = ProducerRecord(topicName, journalOpprettet.messageId, journalOpprettet)
        val result = producer.send(record).get()
        logger.debug(
            "Producer journal opprettet with ID ${journalOpprettet.messageId}, journalpostId ${journalOpprettet.journalpostId} to topic '$topicName' on partition ${result.partition()} offset ${result.offset()}"
        )
    }
}

private val journalKafkaMessageObjectMapper =
    jacksonObjectMapper().apply {
        registerModule(JavaTimeModule())
        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    }

internal class JournalKafkaMessageSerializer : Serializer<JournalpostOpprettetRecord> {
    override fun serialize(topic: String, record: JournalpostOpprettetRecord): ByteArray? =
        journalKafkaMessageObjectMapper.writeValueAsBytes(record)
}
