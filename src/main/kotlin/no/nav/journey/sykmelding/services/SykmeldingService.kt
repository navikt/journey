package no.nav.journey.sykmelding.services

import no.nav.journey.sykmelding.models.SykmeldingRecord
import no.nav.journey.utils.applog
import org.springframework.stereotype.Service

@Service
class SykmeldingService(
    val journalpostService: JournalpostService
) {
    fun handleSykmelding(sykmelding: SykmeldingRecord){
        journalpostService.createJournalpost(sykmelding)
    }
}