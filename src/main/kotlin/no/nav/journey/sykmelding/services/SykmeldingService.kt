package no.nav.journey.sykmelding.services

import no.nav.journey.config.kafkaConfig.JournalKafkaMessage
import no.nav.journey.sykmelding.models.SykmeldingRecord
import no.nav.journey.sykmelding.models.XmlSykmelding
import no.nav.journey.sykmelding.models.metadata.EDIEmottak
import no.nav.journey.sykmelding.models.metadata.EmottakEnkel
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
            val msgId = when (val metadata = sykmelding.metadata) {
                is EmottakEnkel -> metadata.msgInfo.msgId
                is EDIEmottak -> metadata.msgInfo.msgId
                //TODO: inkluder digital
                else -> throw IllegalArgumentException("Ugyldig metadata-type for sykmeldingId=${sykmelding.sykmelding.id}")
            }


            val kafkaMessage = JournalKafkaMessage(
                messageId = msgId,
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