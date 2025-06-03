package no.nav.journey.sykmelding.services

import no.nav.journey.config.kafkaConfig.JournalKafkaMessage
import no.nav.journey.sykmelding.models.SykmeldingRecord
import no.nav.journey.sykmelding.models.XmlSykmelding
import no.nav.journey.sykmelding.models.metadata.EDIEmottak
import no.nav.journey.sykmelding.models.metadata.EmottakEnkel
import no.nav.journey.sykmelding.models.metadata.MetadataType
import no.nav.journey.sykmelding.models.validation.RuleType
import no.nav.journey.utils.applog
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SykmeldingService(
    @Value("\${teamsykmelding.topic.journalOpprettet}") private val journalOpprettetTopic: String,
    val journalpostService: JournalpostService,
    private val journalpostOpprettetProducer: KafkaProducer<String, JournalKafkaMessage>,
) {
    val log = applog()
    fun handleSykmelding(sykmelding: SykmeldingRecord){

        val metadataType = sykmelding.metadata.type
        if (metadataType != MetadataType.EMOTTAK){
            log.info("Oppretter ikke ny pdf for sykmelding ${sykmelding.sykmelding.id} fordi metadataType er: $metadataType")
            return
        }
        if (sykmelding.validation.status == RuleType.PENDING){
            log.info("Oppretter ikke ny pdf for sykmelding ${sykmelding.sykmelding.id} fordi validation status er: ${RuleType.PENDING}")
            return
        }

        val journalpostId = journalpostService.createJournalpost(sykmelding)
        if (journalpostId != null) {
            val kafkaMessage = JournalKafkaMessage(
                messageId = UUID.randomUUID().toString(),
                journalpostId = journalpostId,
                journalpostKilde = "AS36"
            )
            try {
                journalpostOpprettetProducer.send(
                    ProducerRecord(journalOpprettetTopic, sykmelding.sykmelding.id, kafkaMessage),
                ).get()
                log.info(
                    "Sykmelding sendt to kafka topic {} sykmelding id {}",
                    journalOpprettetTopic,
                    sykmelding.sykmelding.id,
                )
            } catch (exception: Exception) {
                log.error("failed to send sykmelding to kafka result for sykmeldingId: {}", sykmelding.sykmelding.id)
                throw exception
            } 
        }
    }
}