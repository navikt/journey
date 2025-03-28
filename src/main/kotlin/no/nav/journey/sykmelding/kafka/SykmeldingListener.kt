package no.nav.journey.sykmelding.kafka

import no.nav.journey.sykmelding.models.SykmeldingRecord
import no.nav.journey.sykmelding.services.SykmeldingService
import no.nav.journey.utils.applog
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component


@Component
class SykmeldingListener(
    private val sykmeldingService: SykmeldingService,
) {

    val logger = applog()

    @KafkaListener(
        topics = ["\${spring.kafka.topics.sykmeldinger-output}"],
        groupId = "journey-consumer",
        containerFactory = "containerFactory",
    )
    fun listen(cr: ConsumerRecord<String, SykmeldingRecord>) {
        logger.info("key=${cr.key()}, offset=${cr.offset()}")
        sykmeldingService.handleSykmelding(cr.value())
    }

}