package no.nav.journey.config

import no.nav.journey.sykmelding.kafka.util.SykmeldingDeserializer
import no.nav.journey.sykmelding.kafka.util.SykmeldingRecordSerializer
import no.nav.journey.sykmelding.models.SykmeldingRecord
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory

@Configuration
@EnableConfigurationProperties
class KafkaConfig {

    @Bean
    fun containerFactory(
        props: KafkaProperties,
        errorHandler: KafkaErrorHandler
    ): ConcurrentKafkaListenerContainerFactory<String, SykmeldingRecord> {
        val consumerFactory = DefaultKafkaConsumerFactory(
            props.buildConsumerProperties(null).apply {
                put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")
                put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1)
                put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true)
            }, StringDeserializer(), SykmeldingDeserializer(SykmeldingRecord::class)
        )

        val factory = ConcurrentKafkaListenerContainerFactory<String, SykmeldingRecord>()
        factory.consumerFactory = consumerFactory
        factory.setCommonErrorHandler(errorHandler)
        return factory
    }
}
