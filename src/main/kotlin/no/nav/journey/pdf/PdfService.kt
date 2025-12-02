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

    private val spmUke7Mapping = mapOf<Sporsmalstype, Pair<String, String>>(
        Sporsmalstype.MEDISINSK_OPPSUMMERING to ("$uke7Prefix.1" to "Gi en kort medisinsk oppsummering av tilstanden (sykehistorie, hovedsymptomer, pågående/planlagt behandling)"),
        Sporsmalstype.UTFORDRINGER_MED_GRADERT_ARBEID to ("$uke7Prefix.2" to "Hvilke utfordringer har pasienten med å utføre gradert arbeid?"),
        Sporsmalstype.HENSYN_PA_ARBEIDSPLASSEN to ("$uke7Prefix.3" to "Hvilke hensyn må være på plass for at pasienten kan prøves i det nåværende arbeidet? (ikke obligatorisk)"),
    )

    fun toUtdypendeOpplysninger(sporsmal: List<UtdypendeSporsmal>?) : Map<String, Map<String, SporsmalSvar>> {
        if(sporsmal.isNullOrEmpty()) {
            return emptyMap()
        }

        val uke7 = sporsmal.asSequence().map { spm ->
            val (key, sporsmal) = spmUke7Mapping[spm.type] ?: throw IllegalArgumentException("Ugyldig sporsmalstype ${spm.type}")
            key to SporsmalSvar(
                sporsmal = sporsmal,
                restriksjoner = listOf(SvarRestriksjon.SKJERMET_FOR_ARBEIDSGIVER),
                svar = spm.svar
            )
        }.toMap()

        return mapOf(uke7Prefix to uke7)
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
