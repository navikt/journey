package no.nav.journey.sykmelding.models.journalpost


data class JournalpostResponse(
    val dokumenter: List<DokumentInfo>,
    val journalpostId: String,
    val journalpostferdigstilt: Boolean,
    val journalstatus: String?,
    val melding: String?,
)

data class DokumentInfo(
    val brevkode: String?,
    val dokumentInfoId: String?,
    val tittel: String?,
)
