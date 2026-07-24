package no.nav.tsm.sykmelding.dokarkiv

import arrow.core.Either
import no.nav.tsm.sykmelding.journalpost.JournalpostRequest
import no.nav.tsm.sykmelding.journalpost.JournalpostResponse

interface DokarkivClient {
    enum class JournalpostError {
        MALFORMED_CONFLICT,
        PERSON_NOT_FOUND,
        UNKNOWN_ERROR,
    }

    suspend fun createJournalpost(journalpostRequest: JournalpostRequest): Either<JournalpostError, JournalpostResponse>
}
