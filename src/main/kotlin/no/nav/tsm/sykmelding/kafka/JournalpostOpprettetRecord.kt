package no.nav.tsm.sykmelding.kafka

data class JournalpostOpprettetRecord(
    val messageId: String,
    val journalpostId: String,
    val journalpostKilde: String,
)
