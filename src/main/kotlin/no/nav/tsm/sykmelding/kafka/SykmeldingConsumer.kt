package no.nav.tsm.sykmelding.kafka

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.time.Duration
import java.util.*
import kotlin.time.toJavaDuration
import no.nav.tsm.sykmelding.input.core.model.SykmeldingModule
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import no.nav.tsm.utils.Environment
import no.nav.tsm.utils.logger
import no.nav.tsm.utils.teamLogger
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.StringDeserializer

class SykmeldingConsumer(environment: Environment) {
    private val logger = logger()
    private val teamLog = teamLogger()

    private val topicName = "tsm.sykmeldinger"
    private val groupId = "journey-consumer"

    private val duration: Duration = environment.kafka.sykmeldingConsumer.longPoll.toJavaDuration()
    private val consumer: KafkaConsumer<String, ByteArray?>

    init {
        val kafkaProperties = Properties()

        kafkaProperties.apply {
            environment.kafka.config.forEach { (key, value) -> this[key] = value }
            this[ConsumerConfig.GROUP_ID_CONFIG] = groupId
            this[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
            this[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = "true"
        }

        consumer = KafkaConsumer(kafkaProperties, StringDeserializer(), ByteArrayDeserializer())
    }

    fun poll(): List<Pair<String, SykmeldingRecord?>> {
        val records = consumer.poll(duration)
        if (records.isEmpty) return emptyList()

        logger.debug("Sykmelding consumer polled ${records.count()} records from $topicName")
        return records.map { tryParse(it) }
    }

    fun subscribe() {
        logger.info("Subscribing $topicName")
        consumer.subscribe(listOf(topicName))
    }

    fun unsubscribe() {
        logger.info("Unsubscribing $topicName")
        consumer.unsubscribe()
    }

    private fun tryParse(
        record: ConsumerRecord<String, ByteArray?>
    ): Pair<String, SykmeldingRecord?> =
        try {
            return record.key() to record.value()?.let { parseAndMapSykmelding(it) }
        } catch (ex: Exception) {
            record.value()?.let {
                teamLog.warn("Full failing sykmelding JSON: ${String(bytes = it)}")
            }

            throw IllegalStateException(
                "Got exception during record deserialization for record with key ${record.key()} and offset ${record.offset()} size (${record.value()?.size ?: "empty"})",
                ex,
            )
        }

    private fun parseAndMapSykmelding(bytes: ByteArray): SykmeldingRecord {
        return recordObjectMapper.readValue<SykmeldingRecord>(bytes)
    }

    private val recordObjectMapper =
        jacksonObjectMapper().apply {
            registerModule(SykmeldingModule())
            registerModule(JavaTimeModule())
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        }
}
