package no.nav.journey.config.kafkaConfig

import no.nav.journey.utils.JacksonKafkaSerializer
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig.ACKS_CONFIG
import org.apache.kafka.clients.producer.ProducerConfig.COMPRESSION_TYPE_CONFIG
import org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG
import org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory

@Configuration
@EnableConfigurationProperties
class KafkaConfig(
    @Value("\${KAFKA_BROKERS}") private val kafkaBrokers: String,
    @Value("\${KAFKA_SECURITY_PROTOCOL:SSL}") private val kafkaSecurityProtocol: String,
    @Value("\${KAFKA_TRUSTSTORE_PATH}") private val kafkaTruststorePath: String,
    @Value("\${KAFKA_CREDSTORE_PASSWORD}") private val kafkaCredstorePassword: String,
    @Value("\${KAFKA_KEYSTORE_PATH}") private val kafkaKeystorePath: String,
    @Value("\${aiven-kafka.auto-offset-reset}") private val kafkaAutoOffsetReset: String,
) {

    private val javaKeystore = "JKS"
    private val pkcs12 = "PKCS12"

    @Bean
    fun containerFactory(
        props: KafkaProperties,
        errorHandler: KafkaErrorHandler
    ): ConcurrentKafkaListenerContainerFactory<String, ByteArray?> {
        val consumerFactory = DefaultKafkaConsumerFactory(
            props.buildConsumerProperties(null).apply {
                put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")
                put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1)
                put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true)
            }, StringDeserializer(), ByteArrayDeserializer()
        )

        val factory = ConcurrentKafkaListenerContainerFactory<String, ByteArray?>()
        factory.consumerFactory = consumerFactory
        factory.setCommonErrorHandler(errorHandler)
        return factory
    }

    @Bean
    fun journalpostOpprettetProducer(): KafkaProducer<String, JournalKafkaMessage> {
        val configs =
            mapOf(
                KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
                VALUE_SERIALIZER_CLASS_CONFIG to JacksonKafkaSerializer::class.java,
                ACKS_CONFIG to "all",
                COMPRESSION_TYPE_CONFIG to "gzip"
            ) + commonConfig()
        return KafkaProducer<String, JournalKafkaMessage>(configs)
    }

    fun commonConfig() =
        mapOf(
            BOOTSTRAP_SERVERS_CONFIG to kafkaBrokers,
        ) + securityConfig()

    private fun securityConfig() =
        mapOf(
            CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to kafkaSecurityProtocol,
            SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG to "",
            // Disable server host name verification
            SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG to javaKeystore,
            SslConfigs.SSL_KEYSTORE_TYPE_CONFIG to pkcs12,
            SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG to kafkaTruststorePath,
            SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG to kafkaCredstorePassword,
            SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG to kafkaKeystorePath,
            SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG to kafkaCredstorePassword,
            SslConfigs.SSL_KEY_PASSWORD_CONFIG to kafkaCredstorePassword,
        )
}

data class JournalKafkaMessage(
    val messageId: String,
    val journalpostId: String,
    val journalpostKilde: String,
)
