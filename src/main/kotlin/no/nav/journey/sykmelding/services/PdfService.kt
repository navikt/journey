package no.nav.journey.sykmelding.services

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.journey.sykmelding.models.Aktivitet
import no.nav.journey.sykmelding.models.Aktivitetstype
import no.nav.journey.sykmelding.models.Papirsykmelding
import no.nav.journey.sykmelding.models.SykmeldingRecord
import no.nav.journey.sykmelding.models.UtenlandskSykmelding
import no.nav.journey.sykmelding.models.XmlSykmelding
import no.nav.pdfgen.core.pdf.createHtml
import no.nav.pdfgen.core.pdf.createPDFA
import org.springframework.stereotype.Service

@Service
class PdfService {


    fun createPdf(sykmeldingRecord: SykmeldingRecord): ByteArray? {
        val pdfPayload = buildPdfPayload(sykmeldingRecord)
        val objectMapper = jacksonObjectMapper().registerModule(JavaTimeModule()).registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        val test = createHtml("sm", "sm", objectMapper.valueToTree(pdfPayload))?.let { document ->
            createPDFA(document)
        }
        return test

    }

    fun buildPdfPayload(sykmeldingRecord: SykmeldingRecord): SykmeldingRecord {
        val sorterteAktiviteter = sykmeldingRecord.sykmelding.aktivitet.sorter().groupBy { it.type }

        val enrichedAndSortedSykmelding = when (sykmeldingRecord.sykmelding) {
            is UtenlandskSykmelding -> sykmeldingRecord.sykmelding.copy(
                aktiviteter = sorterteAktiviteter
            )
            is Papirsykmelding -> sykmeldingRecord.sykmelding.copy(
                aktiviteter = sorterteAktiviteter
            )
            is XmlSykmelding -> sykmeldingRecord.sykmelding.copy(
                aktiviteter = sorterteAktiviteter
            )
        }

        return sykmeldingRecord.copy(
            sykmelding = enrichedAndSortedSykmelding
        )
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