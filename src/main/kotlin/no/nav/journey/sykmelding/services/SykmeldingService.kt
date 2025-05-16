package no.nav.journey.sykmelding.services

import no.nav.journey.config.kafkaConfig.JournalKafkaMessage
import no.nav.journey.sykmelding.models.SykmeldingRecord
import no.nav.journey.utils.applog
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class SykmeldingService(
    @Value("\${teamsykmelding.topic.journalOpprettet}") private val journalOpprettetTopic: String,
    val journalpostService: JournalpostService,
    private val journalpostOpprettetProducer: KafkaProducer<String, JournalKafkaMessage>,
) {
    val log = applog()
    fun handleSykmelding(sykmelding: SykmeldingRecord){
        val journalpostId = journalpostService.createJournalpost(sykmelding)
        if (journalpostId != null) {
            val kafkaMessage = JournalKafkaMessage(
                messageId = "AS36",
                journalpostId = sykmelding.sykmelding.id,
                journalpostKilde = journalpostId
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