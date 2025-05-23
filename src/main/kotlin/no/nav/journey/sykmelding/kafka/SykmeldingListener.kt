package no.nav.journey.sykmelding.kafka

import no.nav.journey.sykmelding.models.SykmeldingRecord
import no.nav.journey.sykmelding.services.SykmeldingService
import no.nav.journey.sykmelding.services.util.objectMapper
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
    fun listen(cr: ConsumerRecord<String, ByteArray>) {
        logger.info("sykmeldingRecord from kafka: key=${cr.key()}, offset=${cr.offset()}")
        val headerValue = cr.headers()
            .lastHeader("processing-target")
            ?.value()
            ?.toString(Charsets.UTF_8)

        if (headerValue != "tsm") {
            logger.info("Ignorerer melding fordi processing-target='$headerValue'")
            return
        }

        try {
            val sykmeldingValue = cr.value()?.let { objectMapper.readValue(it, SykmeldingRecord::class.java) }
            if (sykmeldingValue == null) {
                logger.error("Mottok en melding uten verdi på topic ${cr.topic()}, offset ${cr.offset()}")
                return
            }
            sykmeldingService.handleSykmelding(sykmeldingValue)
        } catch (e: Exception) {
            logger.error("Exception caught while handling sykmelding ${e.message}", e)
            throw e
        }
    }
}