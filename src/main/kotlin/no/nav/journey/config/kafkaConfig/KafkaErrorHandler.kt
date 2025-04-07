package no.nav.journey.config.kafkaConfig

import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.slf4j.LoggerFactory
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.kafka.listener.MessageListenerContainer
import org.springframework.stereotype.Component
import org.springframework.util.backoff.FixedBackOff
import org.springframework.util.backoff.FixedBackOff.UNLIMITED_ATTEMPTS

@Component
class KafkaErrorHandler : DefaultErrorHandler(
    null,
    FixedBackOff(BACKOFF_INTERVAL, UNLIMITED_ATTEMPTS)
) {
    companion object {
        private const val BACKOFF_INTERVAL = 60_000L
    }

    private val log = LoggerFactory.getLogger(KafkaErrorHandler::class.java)

    override fun handleOne(
        thrownException: Exception,
        record: ConsumerRecord<*, *>,
        consumer: Consumer<*, *>,
        container: MessageListenerContainer
    ): Boolean {
        logDetailedError(thrownException, record)
        return super.handleOne(thrownException, record, consumer, container)
    }

    override fun handleRemaining(
        thrownException: Exception,
        records: MutableList<ConsumerRecord<*, *>>,
        consumer: Consumer<*, *>,
        container: MessageListenerContainer
    ) {
        if (records.isEmpty()) {
            log.error("KafkaErrorHandler: Feil i listener uten noen records", thrownException)
        } else {
            records.forEach { logDetailedError(thrownException, it) }
        }
        super.handleRemaining(thrownException, records, consumer, container)
    }

    override fun handleBatch(
        thrownException: Exception,
        data: ConsumerRecords<*, *>,
        consumer: Consumer<*, *>,
        container: MessageListenerContainer,
        invokeListener: Runnable
    ) {
        if (data.isEmpty) {
            log.error("KafkaErrorHandler: Feil i listener uten noen records", thrownException)
        } else {
            data.forEach { logDetailedError(thrownException, it) }
        }
        super.handleBatch(thrownException, data, consumer, container, invokeListener)
    }

    private fun logDetailedError(thrownException: Exception, record: ConsumerRecord<*, *>) {
        log.error(
            """
            KafkaErrorHandler: Feil i prosesseringen av record
            Topic: ${record.topic()}
            Partition: ${record.partition()}
            Offset: ${record.offset()}
            Key: ${record.key()}
            Timestamp: ${record.timestamp()}
            Exception Type: ${thrownException::class.simpleName}
            Exception Message: ${thrownException.message}
            Stacktrace:
            """.trimIndent(),
            thrownException
        )
    }
}