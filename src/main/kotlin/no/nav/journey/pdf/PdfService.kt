package no.nav.journey.pdf

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.pdfgen.core.pdf.createHtml
import no.nav.pdfgen.core.pdf.createPDFA
import no.nav.tsm.sykmelding.input.core.model.Aktivitet
import no.nav.tsm.sykmelding.input.core.model.Aktivitetstype
import no.nav.tsm.sykmelding.input.core.model.DigitalSykmelding
import no.nav.tsm.sykmelding.input.core.model.SporsmalSvar
import no.nav.tsm.sykmelding.input.core.model.Sporsmalstype
import no.nav.tsm.sykmelding.input.core.model.SvarRestriksjon
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import no.nav.tsm.sykmelding.input.core.model.UtdypendeSporsmal
import no.nav.tsm.sykmelding.input.core.model.XmlSykmelding
import org.springframework.stereotype.Service
import kotlin.Pair
import kotlin.String

@Service
class PdfService {
    private val uke7Prefix = "6.3"
    private val uke17Prefix = "6.4"
    private val uke39Prefix = "6.5"

    fun spmMapping(prefix: String) : Map<Sporsmalstype, Pair<String, String>> =
        mapOf<Sporsmalstype, Pair<String, String>>(
            Sporsmalstype.MEDISINSK_OPPSUMMERING to ("$prefix.1" to "Gi en kort medisinsk oppsummering av tilstanden (sykehistorie, hovedsymptomer, behandling)"),
            Sporsmalstype.UTFORDRINGER_MED_ARBEID to ("$prefix.2" to "Beskriv kort hvilke utfordringer helsetilstanden gir i arbeidssituasjonen nå. Oppgi også kort hva pasienten likevel kan mestre"),
            Sporsmalstype.UTFORDRINGER_MED_GRADERT_ARBEID to ("${uke7Prefix}.2" to "Beskriv kort hvilke helsemessige begrensninger som gjør det vanskelig å jobbe gradert"),
            Sporsmalstype.HENSYN_PA_ARBEIDSPLASSEN to ("${uke7Prefix}.3" to "Beskriv eventuelle medisinske forhold som bør ivaretas ved eventuell tilbakeføring til nåværende arbeid (ikke obligatorisk)"),
            Sporsmalstype.BEHANDLING_OG_FREMTIDIG_ARBEID to ("$uke17Prefix.3" to "Beskriv pågående og planlagt utredning/behandling, og om dette forventes å påvirke muligheten for økt arbeidsdeltakelse fremover"),
            Sporsmalstype.UAVKLARTE_FORHOLD to ("$uke17Prefix.4" to "Er det forhold som fortsatt er uavklarte eller hindrer videre arbeidsdeltakelse, som Nav bør være kjent med i sin oppfølging?"),
            Sporsmalstype.FORVENTET_HELSETILSTAND_UTVIKLING to ("$uke39Prefix.3" to "Hvordan forventes helsetilstanden å utvikle seg de neste 3-6 månedene med tanke på mulighet for økt arbeidsdeltakelse?"),
            Sporsmalstype.MEDISINSKE_HENSYN to ("$uke39Prefix.4" to "Er det medisinske hensyn eller avklaringsbehov Nav bør kjenne til i videre oppfølging?")
        )

    fun toUtdypendeOpplysninger(sporsmal: List<UtdypendeSporsmal>?) : Map<String, Map<String, SporsmalSvar>> {
        if(sporsmal.isNullOrEmpty()) {
            return emptyMap()
        }


        val prefix = when {
            sporsmal.any { it.type == Sporsmalstype.MEDISINSKE_HENSYN } -> uke39Prefix
            sporsmal.any { it.type == Sporsmalstype.BEHANDLING_OG_FREMTIDIG_ARBEID } -> uke17Prefix
            sporsmal.any { it.type == Sporsmalstype.UTFORDRINGER_MED_GRADERT_ARBEID } -> uke7Prefix
            else -> throw IllegalArgumentException("Utdypende sporsmal does not have correct prefix ${sporsmal.first().type}")
        }

        val mappings = spmMapping(prefix)
        val sporsmals = sporsmal.mapNotNull { spm ->
            mappings[spm.type]?.let { (key, ss) ->
                key to SporsmalSvar(
                    sporsmal = spm.sporsmal ?: ss,
                    restriksjoner = listOf(SvarRestriksjon.SKJERMET_FOR_ARBEIDSGIVER),
                    svar = spm.svar
                )
            }
        }


        val grouped = sporsmals.groupBy {
            when {
                it.first.startsWith(uke39Prefix) -> uke39Prefix
                it.first.startsWith(uke17Prefix) -> uke17Prefix
                it.first.startsWith(uke7Prefix) -> uke7Prefix
                else -> {
                    throw IllegalArgumentException("Sporsmal does not have correct prefix ${it.first}")
                }
            }
        }.mapValues { it.value.toMap() }

        return grouped
    }


    private val objectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    fun createPdf(sykmeldingRecord: SykmeldingRecord): ByteArray? {
        val pdfPayload = buildPdfPayload(sykmeldingRecord)

        val pdf = createHtml("sm", "sm", objectMapper.valueToTree(pdfPayload))?.let { document ->
            createPDFA(document)
        }
        return pdf

    }

    fun buildPdfPayload(sykmeldingRecord: SykmeldingRecord): PdfPayload {
        return when (val sykmelding = sykmeldingRecord.sykmelding) {
            is XmlSykmelding -> {
                val sorterteAktiviteter = sykmelding.aktivitet.sorter().groupBy { it.type }
                PdfPayload(
                    metadata = sykmeldingRecord.metadata,
                    sykmelding = sykmelding,
                    validation = sykmeldingRecord.validation,
                    aktiviteter = sorterteAktiviteter,
                    utdypendeOpplysninger = sykmelding.utdypendeOpplysninger
                )
            }
            is DigitalSykmelding -> {
                val sorterteAktiviteter = sykmelding.aktivitet.sorter().groupBy { it.type }
                PdfPayload(
                    metadata = sykmeldingRecord.metadata,
                    sykmelding = sykmelding,
                    validation = sykmeldingRecord.validation,
                    aktiviteter = sorterteAktiviteter,
                    utdypendeOpplysninger = toUtdypendeOpplysninger(sykmelding.utdypendeSporsmal)
                )
            }
            else -> throw IllegalArgumentException("Kan ikke bygge pdf payload for type ${sykmelding::class.simpleName}")
        }
    }

    private fun List<Aktivitet>.sorter(): List<Aktivitet> {
        val ønsketRekkefølge = listOf(
            Aktivitetstype.AVVENTENDE,
            Aktivitetstype.GRADERT,
            Aktivitetstype.AKTIVITET_IKKE_MULIG,
            Aktivitetstype.BEHANDLINGSDAGER,
            Aktivitetstype.REISETILSKUDD,
        )
        return this.sortedWith(compareBy { ønsketRekkefølge.indexOf(it.type) })
    }
}
