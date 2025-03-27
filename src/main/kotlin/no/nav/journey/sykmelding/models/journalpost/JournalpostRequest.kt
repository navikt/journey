package no.nav.journey.sykmelding.models.journalpost

data class JournalpostRequest(
    val avsenderMottaker: AvsenderMottaker?,
    val behandlingsTema: String,
    val bruker: Bruker?,
    val dokumenter: List<Dokument>?,
    val eksternReferanseId: String,
    val journalfoerendeEnhet: String?,
    val journalpostType: String,
    val kanal: String?,
    val sak: Sak?,
    val tema: String?,
    val tittel: String?,
)

data class AvsenderMottaker(
    val id: String,
    val idType: String,
    val navn: String,
)

data class Bruker(
    val id: String,
    val idType: String,
)

data class Dokument(
    val brevkode: String,
    val dokumentKategori: String,
    val dokumentvarianter: List<Dokumentvarianter>,
    val tittel: String,
)

data class Dokumentvarianter(
    val filnavn: String,
    val filtype: String,
    val fysiskDokument: ByteArray,
    val variantformat: String,
)

data class Sak(
    val sakstype: String,
)
