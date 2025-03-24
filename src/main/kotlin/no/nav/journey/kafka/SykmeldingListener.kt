package no.nav.journey.kafka

import no.nav.journey.utils.applog
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component


@Component
class SykmeldingListener {

    val logger = applog()

    @KafkaListener(
        topics = ["\${sykmelding.topic}"],
        groupId = "journey-consumer",
        properties = ["auto.offset.reset = none"],
        containerFactory = "aivenKafkaListenerContainerFactory",
    )
    fun listen(cr: ConsumerRecord<String, String>, acknowledgment: Acknowledgment) {
        logger.info("Received sykmelding with key ${cr.key()}")
    }



}