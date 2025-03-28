package no.nav.journey.sykmelding.models.journalpost

data class Vedlegg(
    val content: Content,
    val type: String,
    val description: String,
)

data class Content(val contentType: String, val content: String)

