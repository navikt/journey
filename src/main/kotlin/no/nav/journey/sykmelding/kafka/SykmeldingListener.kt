package no.nav.journey.sykmelding.kafka

import no.nav.journey.sykmelding.models.SykmeldingRecord
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
        containerFactory = "containerFactory",
    )
    fun listen(cr: ConsumerRecord<String, String>, acknowledgment: Acknowledgment) {
        logger.info(">>>> INNE I LYTTER: key=${cr.key()}, offset=${cr.offset()}, payload=${cr.value()}")
    }

}