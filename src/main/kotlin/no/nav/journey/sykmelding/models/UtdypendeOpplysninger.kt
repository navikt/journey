package no.nav.journey.sykmelding.models

data class SporsmalSvar(
    val sporsmal: String?, val svar: String, val restriksjoner: List<SvarRestriksjon>
)

enum class SvarRestriksjon(
) {
    SKJERMET_FOR_ARBEIDSGIVER, SKJERMET_FOR_PASIENT, SKJERMET_FOR_NAV,
}
