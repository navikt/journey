package no.nav.journey.sykmelding

import no.nav.journey.pdf.PdfService
import no.nav.journey.pdf.typst.TypstClient
import no.nav.journey.pdf.typst.buildTypstPayload
import no.nav.journey.testUtils.digital
import no.nav.journey.testUtils.extractTextFromPdf
import no.nav.journey.testUtils.xml
import no.nav.pdfgen.core.Environment
import no.nav.pdfgen.core.PDFGenCore
import no.nav.pdfgen.core.objectMapper
import no.nav.tsm.sykmelding.input.core.model.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.verapdf.gf.foundry.VeraGreenfieldFoundryProvider
import java.awt.Desktop
import java.io.File

class PdfGenTest {
    private val pdfService = PdfService(
        typstClient = TypstClient(
            typstBinaryPath = "typst-pdf/typst",
            templatePath = "typst-pdf/sykmelding.typ",
            fontPath = "typst-pdf/fonts",
        )
    )

    companion object {
        @JvmStatic
        @BeforeAll
        fun setup(): Unit {
            VeraGreenfieldFoundryProvider.initialise()
            val coreEnvironment = Environment()
            PDFGenCore.init(coreEnvironment)
        }
    }

    @Test
    fun genTestData() {
        val payload = buildTypstPayload(xml.record)
        val stringied = objectMapper.writeValueAsString(payload)

        // update typst-pdf/test-data/sykmelding.json

        val testDataFile = File("typst-pdf/test-data/sykmelding.json")
        testDataFile.writeText(stringied)
    }

    @Test
    fun `generate pdf for sykmelding flere arbeidsgivere`() {
        val recordMedFlereArbeidsgivere = xml.record.copy(
            sykmelding = xml.sykmelding.copy(
                arbeidsgiver = ArbeidsgiverInfo.Flere("Coop", "Butikkmedarbeider", 80, null, null)
            )
        )
        val pdfBytes = pdfService.createPdf(recordMedFlereArbeidsgivere)!!

        val fil = File("build/test.pdf")
        fil.writeBytes(pdfBytes)
        val tekst = extractTextFromPdf(fil)
        assert(tekst.contains("Flere arbeidsgivere")) { "Mangler 'Flere arbeidsgivere'" }
        assert(tekst.contains("Coop")) { "Mangler arbeidsgivernavn 'Coop'" }
        assert(tekst.contains("Butikkmedarbeider")) { "Mangler stillingstittel" }
        assert(tekst.contains("80")) { "Mangler stillingsprosent '80'" }
    }

    @Test
    fun `generate pdf for digital sykmelding`() {
        val sykmeldingRecord = digital.record
        val pdfBytes = pdfService.createPdf(sykmeldingRecord)!!

        val fil = File("build/test.pdf")
        fil.writeBytes(pdfBytes)
        val tekst = extractTextFromPdf(fil)
        assert(tekst.contains("Gi en kort medisinsk oppsummering av tilstanden")) { "Mangler 'MEDISINSK_OPPSUMMERING'" }
        assert(tekst.contains("Beskriv kort hvilke helsemessige begrensninger som gjør det vanskelig å jobbe gradert")) { "Mangler 'UTFORDRINGER_MED_GRADERT_ARBEID'" }
        assert(tekst.contains("hensyn på arbeidsplassen?")) { "Mangler 'HENSYN_PA_ARBEIDSPLASSEN'" }
        assert(tekst.contains("svar 6.3.1"))
        assert(tekst.contains("svar 6.3.2"))
        assert(tekst.contains("svar 6.3.3"))


    }

    @Test
    fun `generate pdf for digital sykmelding with utdypende sporsmal med tekst`() {
        val sykmeldingRecord = digital.record.copy(
            sykmelding = digital.sykmelding.copy(
                utdypendeSporsmal = listOf(
                    UtdypendeSporsmal(
                        svar = "svar 6.3.1",
                        Sporsmalstype.MEDISINSK_OPPSUMMERING,
                        true,
                        "sporsmal 6.3.1"
                    ),
                    UtdypendeSporsmal(
                        svar = "svar 6.3.2",
                        Sporsmalstype.UTFORDRINGER_MED_GRADERT_ARBEID,
                        true,
                        "sporsmal 6.3.2"
                    ),
                    UtdypendeSporsmal(
                        svar = "svar 6.3.3",
                        Sporsmalstype.HENSYN_PA_ARBEIDSPLASSEN,
                        true,
                        "sporsmal 6.3.3"
                    ),
                )
            )
        )
        val pdfBytes = pdfService.createPdf(sykmeldingRecord)!!

        val fil = File("build/test.pdf")
        fil.writeBytes(pdfBytes)
        val tekst = extractTextFromPdf(fil)
        assert(tekst.contains("sporsmal 6.3.1")) { "Mangler 'MEDISINSK_OPPSUMMERING'" }
        assert(tekst.contains("sporsmal 6.3.2")) { "Mangler 'UTFORDRINGER_MED_GRADERT_ARBEID'" }
        assert(tekst.contains("sporsmal 6.3.3")) { "Mangler 'HENSYN_PA_ARBEIDSPLASSEN'" }
        assert(tekst.contains("svar 6.3.1"))
        assert(tekst.contains("svar 6.3.2"))
        assert(tekst.contains("svar 6.3.3"))

    }

    @Test
    fun `generate pdf for ICPC-2B`() {
        val record = xml.record.copy(
            sykmelding = xml.sykmelding.copy(
                medisinskVurdering = xml.sykmelding.medisinskVurdering.copy(
                    hovedDiagnose = DiagnoseInfo(DiagnoseSystem.ICPC2B, "R74.0001", "diagnosebeskrivelse"),
                    biDiagnoser = listOf(
                        DiagnoseInfo(
                            DiagnoseSystem.ICPC2B, "R74.0002", "annen diagnosebeskrivelse"
                        )

                    ),
                ),
                arbeidsgiver = ArbeidsgiverInfo.Flere("Coop", "Butikkmedarbeider", 80, null, null)
            )
        )
        val pdfBytes = pdfService.createPdf(record)!!

        val fil = File("build/test.pdf")
        fil.writeBytes(pdfBytes)
        val tekst = extractTextFromPdf(fil)
        assert(tekst.contains("ICPC-2B")) { "Mangler 'diagnosesystem'" }
        assert(tekst.contains("R74.0002")) { "Mangler 'diagnosekode'" }
        assert(tekst.contains("R74.0001")) { "Mangler 'diagnosekode'" }

    }

    @Test
    fun `generate pdf for sykmelding med hoveddiagnose og bidiagnoser`() {
        val sykmeldingRecord = digital.record.copy(
            sykmelding = digital.sykmelding.copy(
                utdypendeSporsmal = listOf(
                    UtdypendeSporsmal("Answer1", Sporsmalstype.MEDISINSK_OPPSUMMERING, sporsmal = "sporsmal 1"),
                    UtdypendeSporsmal("Answer2", Sporsmalstype.UTFORDRINGER_MED_ARBEID, sporsmal = "sporsmal 2"),
                    UtdypendeSporsmal(
                        "Answer3",
                        Sporsmalstype.UTFORDRINGER_MED_GRADERT_ARBEID,
                        sporsmal = "sporsmal 3"
                    ),
                    UtdypendeSporsmal("Answer4", Sporsmalstype.HENSYN_PA_ARBEIDSPLASSEN, sporsmal = "sporsmal 4"),
                    UtdypendeSporsmal("Answer5", Sporsmalstype.BEHANDLING_OG_FREMTIDIG_ARBEID, sporsmal = "sporsmal 5"),
                    UtdypendeSporsmal("Answer6", Sporsmalstype.UAVKLARTE_FORHOLD, sporsmal = "sporsmal 6"),
                    UtdypendeSporsmal(
                        "Answer7",
                        Sporsmalstype.FORVENTET_HELSETILSTAND_UTVIKLING,
                        sporsmal = "sporsmal 7"
                    ),
                    UtdypendeSporsmal("Answer8", Sporsmalstype.MEDISINSKE_HENSYN, sporsmal = "sporsmal 8"),
                )
            )
        )
        val pdfBytes = pdfService.createPdf(sykmeldingRecord)!!

        val fil = File("build/test.pdf")
        fil.writeBytes(pdfBytes)
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(fil)
        }
    }
}
