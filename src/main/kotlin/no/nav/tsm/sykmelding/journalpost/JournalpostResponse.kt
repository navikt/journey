package no.nav.tsm.sykmelding.journalpost

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
