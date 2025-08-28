package no.nav.journey.sykmelding.kafka

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.journey.sykmelding.services.SykmeldingService
import no.nav.journey.utils.applog
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import no.nav.tsm.sykmelding.input.core.model.sykmeldingObjectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import java.nio.charset.Charset


@Component
class SykmeldingListener(
    private val sykmeldingService: SykmeldingService,
) {

    val logger = applog()

    @KafkaListener(
        topics = ["\${spring.kafka.topics.sykmeldinger}"],
        groupId = "journey-consumer",
        containerFactory = "containerFactory",
    )
    fun listen(cr: ConsumerRecord<String, ByteArray>) {

        val sykmeldingValue = cr.value()
            ?.toString(Charset.defaultCharset())
            ?.replace("\uFEFF", "")
            ?.let { sykmeldingObjectMapper.readValue<SykmeldingRecord>(it) }

        if (sykmeldingValue == null) {
            logger.info("Mottok en tombstone på topic ${cr.topic()}, offset ${cr.offset()}")
            return
        }
        sykmeldingService.handleSykmelding(sykmeldingValue)
    }
}
