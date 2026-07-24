package testUtils

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.time.Duration
import java.util.Properties
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.testcontainers.kafka.ConfluentKafkaContainer

abstract class WithKafka {
    companion object {
        private val topics = listOf("teamsykmelding.oppgave-journal-opprettet", "tsm.sykmeldinger")

        val kafka =
            ConfluentKafkaContainer("confluentinc/cp-kafka:8.1.0").apply {
                start()
                createTopics()
            }

        private fun ConfluentKafkaContainer.createTopics() {
            val props =
                Properties().apply {
                    this[AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
                }
            AdminClient.create(props).use { admin ->
                admin.createTopics(topics.map { NewTopic(it, 1, 1) }).all().get()
            }
        }

        fun recreateTopics() {
            val props =
                Properties().apply {
                    this[AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG] = kafka.bootstrapServers
                }

            AdminClient.create(props).use { admin ->
                admin.deleteTopics(topics).all().get()
                admin.createTopics(topics.map { NewTopic(it, 1, 1) }).all().get()
            }
        }
    }
}

suspend fun ConfluentKafkaContainer.produce(topic: String, key: String, value: ByteArray?) {
    withContext(Dispatchers.IO) {
        val props =
            Properties().apply {
                this["bootstrap.servers"] = bootstrapServers
            }
        KafkaProducer(props, StringSerializer(), ByteArraySerializer()).use { producer ->
            producer.send(ProducerRecord(topic, key, value)).get()
        }
    }
}

suspend inline fun <reified T> ConfluentKafkaContainer.consumeUntil(
    topic: String,
    crossinline want: (record: T) -> Boolean,
    timeout: Duration = Duration.ofSeconds(10),
): T {
    return withContext(Dispatchers.IO) {
        val props =
            Properties().apply {
                this[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
                this[ConsumerConfig.GROUP_ID_CONFIG] = "test-${UUID.randomUUID()}"
                this[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
            }
        KafkaConsumer(props, StringDeserializer(), ByteArrayDeserializer()).use { consumer ->
            consumer.subscribe(listOf(topic))
            val deadline = System.nanoTime() + timeout.toNanos()
            while (System.nanoTime() < deadline) {
                val records = runInterruptible { consumer.poll(Duration.ofMillis(200)) }
                for (record in records) {
                    val value: T = consumerObjectMapper.readValue<T>(record.value())
                    val doWeWant = want(value)

                    println("Topic $topic - key ${record.key()}, does consumer want? $doWeWant")

                    if (doWeWant) return@withContext value
                }
            }
            throw AssertionError("Timed out waiting for message on topic=$topic")
        }
    }
}

val consumerObjectMapper =
    jacksonObjectMapper().apply {
        registerModule(JavaTimeModule())
        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    }
