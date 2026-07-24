package no.nav.tsm.sykmelding.journalpost

data class Vedlegg(
    val content: Content,
    val type: String,
    val description: String,
)

data class Content(val contentType: String, val content: String)

data class GosysVedlegg(val contentType: String, val content: ByteArray, val description: String)
