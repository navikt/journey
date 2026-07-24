package no.nav.tsm.sykmelding.services

import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.tsm.ktor.otel.failSpan
import java.util.UUID
import no.nav.tsm.sykmelding.input.core.model.RuleType
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import no.nav.tsm.sykmelding.kafka.JournalOpprettetProducer
import no.nav.tsm.sykmelding.kafka.JournalpostOpprettetRecord
import no.nav.tsm.utils.logger

class SykmeldingService(
    private val journalpostService: JournalpostService,
    private val producer: JournalOpprettetProducer,
) {
    private val logger = logger()

    @WithSpan
    suspend fun handleSykmelding(sykmelding: SykmeldingRecord) {
        val span = Span.current()
        span.setAttribute("sykmleding.id", sykmelding.sykmelding.id)
        span.setAttribute("sykmelding.validation.status", sykmelding.validation.status.name)

        if (sykmelding.validation.status == RuleType.PENDING) {
            logger.info(
                "Oppretter ikke ny pdf for sykmelding ${sykmelding.sykmelding.id} fordi validation status er: ${RuleType.PENDING}"
            )
            return
        }
        val journalpostId =
            journalpostService.createJournalpost(sykmelding).fold({ error ->
                logger.error(
                    "Creating journalpost failed for sykmelding ${sykmelding.sykmelding.id} with error: ${error.name}"
                )
                throw Exception(
                    "Creating journalpost failed for sykmelding ${sykmelding.sykmelding.id} with error: ${error.name}"
                )
            }) {
                it
            }

        span.setAttribute("journalpost.id", journalpostId)

        try {
            producer.opprettJournalpostRecord(
                JournalpostOpprettetRecord(
                    messageId = UUID.randomUUID().toString(),
                    journalpostId = journalpostId,
                    journalpostKilde = "AS36",
                )
            )
        } catch (exception: Exception) {
            logger.error(
                "failed to send sykmelding to kafka result for sykmeldingId: ${sykmelding.sykmelding.id}"
            )
            throw exception.failSpan()
        }
    }
}
