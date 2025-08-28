package no.nav.journey.sykmelding.services

import no.nav.journey.config.kafkaConfig.JournalKafkaMessage
import no.nav.journey.utils.applog
import no.nav.tsm.sykmelding.input.core.model.RuleType
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SykmeldingService(
    @param:Value("\${teamsykmelding.topic.journalOpprettet}") private val journalOpprettetTopic: String,
    val journalpostService: JournalpostService,
    private val journalpostOpprettetProducer: KafkaProducer<String, JournalKafkaMessage>,
) {
    val log = applog()
    fun handleSykmelding(sykmelding: SykmeldingRecord){
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
                log.info("Sykmelding sendt to kafka topic $journalOpprettetTopic sykmelding id ${sykmelding.sykmelding.id}, sykmelding type ${sykmelding.sykmelding.type}, journalpostId: $journalpostId")
            } catch (exception: Exception) {
                log.error("failed to send sykmelding to kafka result for sykmeldingId: ${sykmelding.sykmelding.id}")
                throw exception
            }
        } else {
            log.warn("Kunne ikke opprette journalpost for sykmelding could not find journalpostID for ${sykmelding.sykmelding.type} sykmeldingID: ${sykmelding.sykmelding.id}")
        }
    }
}
