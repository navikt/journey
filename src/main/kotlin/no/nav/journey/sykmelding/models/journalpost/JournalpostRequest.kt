package no.nav.journey.sykmelding.models.journalpost

import com.fasterxml.jackson.annotation.JsonInclude

data class JournalpostRequest(
    val avsenderMottaker: AvsenderMottaker?,
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

@JsonInclude(JsonInclude.Include.NON_NULL)
data class AvsenderMottaker(
    val id: String? = null,
    val idType: String? = null,
    val land: String? = null,
    val navn: String,
)

data class Bruker(
    val id: String,
    val idType: String,
)

data class Dokument(
    val brevkode: String,
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
