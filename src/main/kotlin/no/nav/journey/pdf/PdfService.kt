package no.nav.journey.pdf

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.pdfgen.core.pdf.createHtml
import no.nav.pdfgen.core.pdf.createPDFA
import no.nav.tsm.sykmelding.input.core.model.Aktivitet
import no.nav.tsm.sykmelding.input.core.model.Aktivitetstype
import no.nav.tsm.sykmelding.input.core.model.DigitalSykmelding
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import no.nav.tsm.sykmelding.input.core.model.XmlSykmelding
import org.springframework.stereotype.Service

@Service
class PdfService {


    fun createPdf(sykmeldingRecord: SykmeldingRecord): ByteArray? {
        val pdfPayload = buildPdfPayload(sykmeldingRecord)
        val objectMapper = jacksonObjectMapper().registerModule(JavaTimeModule()).registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
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
                    aktiviteter = sorterteAktiviteter
                )
            }
            is DigitalSykmelding -> {
                val sorterteAktiviteter = sykmelding.aktivitet.sorter().groupBy { it.type }
                PdfPayload(
                    metadata = sykmeldingRecord.metadata,
                    sykmelding = sykmelding,
                    validation = sykmeldingRecord.validation,
                    aktiviteter = sorterteAktiviteter
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
