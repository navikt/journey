package no.nav.tsm.sykmelding.dokarkiv

import arrow.core.Either
import arrow.core.right
import no.nav.tsm.sykmelding.journalpost.JournalpostRequest
import no.nav.tsm.sykmelding.journalpost.JournalpostResponse
import no.nav.tsm.utils.logger

class DokarkivLocalClient : DokarkivClient {
    private val logger = logger()

    override suspend fun createJournalpost(
        journalpostRequest: JournalpostRequest
    ): Either<DokarkivClient.JournalpostError, JournalpostResponse> {
        logger.info("Local Dokarkiv mock, creating fake JournalRequest response")

        return JournalpostResponse(
                dokumenter = emptyList(),
                journalpostId = "EL8PV",
                journalpostferdigstilt = false,
                journalstatus = null,
                melding = null,
            )
            .right()
    }
}
