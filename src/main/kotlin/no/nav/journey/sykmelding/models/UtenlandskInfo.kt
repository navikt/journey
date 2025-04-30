package no.nav.journey.sykmelding.models

data class UtenlandskInfo(
    val land: String,
    val folkeRegistertAdresseErBrakkeEllerTilsvarende: Boolean,
    val erAdresseUtland: Boolean?,
)
