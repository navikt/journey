package no.nav.tsm.pdf

import java.awt.Desktop
import java.io.File
import java.time.LocalDate
import kotlin.test.Test
import no.nav.tsm.sykmelding.input.core.model.*
import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import testUtils.digital
import testUtils.xml

class TypstClientTest {
    private val typstClient =
        TypstClient(
            typstBinaryPath = "typst-pdf/typst",
            templatePath = "typst-pdf/sykmelding.typ",
            fontPath = "typst-pdf/fonts",
        )

    @Test
    fun updateTypstTestData() {
        /** no-op test that simply updates the test-data for typst-pdf generation */
        val payload =
            buildTypstPayload(
                xml.record.copy(
                    sykmelding = xml.sykmelding.copy(id = "f29c5569-2ff6-40c8-a919-37fbd76958be")
                )
            )
        val stringied = typstClient.objectMapper.writeValueAsString(payload)

        File("typst-pdf/test-data/sykmelding.json").writeText(stringied)
    }

    @Test
    fun `generate pdf for sykmelding flere arbeidsgivere`() {
        val recordMedFlereArbeidsgivere =
            xml.record.copy(
                sykmelding =
                    xml.sykmelding.copy(
                        arbeidsgiver =
                            ArbeidsgiverInfo.Flere("Coop", "Butikkmedarbeider", 80, null, null)
                    )
            )
        val pdfBytes = typstClient.createPdf(buildTypstPayload(recordMedFlereArbeidsgivere))

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
        val pdfBytes = typstClient.createPdf(buildTypstPayload(sykmeldingRecord))

        val fil = File("build/test.pdf")
        fil.writeBytes(pdfBytes)
        val tekst = extractTextFromPdf(fil)
        assert(tekst.contains("Gi en kort medisinsk oppsummering av tilstanden")) {
            "Mangler 'MEDISINSK_OPPSUMMERING'"
        }
        assert(
            tekst.contains(
                "Beskriv kort hvilke helsemessige begrensninger som gjør det vanskelig å jobbe gradert"
            )
        ) {
            "Mangler 'UTFORDRINGER_MED_GRADERT_ARBEID'"
        }
        assert(tekst.contains("hensyn på arbeidsplassen?")) { "Mangler 'HENSYN_PA_ARBEIDSPLASSEN'" }
        assert(tekst.contains("svar 6.3.1"))
        assert(tekst.contains("svar 6.3.2"))
        assert(tekst.contains("svar 6.3.3"))
    }

    @Test
    fun `generate pdf for digital sykmelding with utdypende sporsmal med tekst`() {
        val sykmeldingRecord =
            digital.record.copy(
                sykmelding =
                    digital.sykmelding.copy(
                        utdypendeSporsmal =
                            listOf(
                                UtdypendeSporsmal(
                                    svar = "svar 6.3.1",
                                    Sporsmalstype.MEDISINSK_OPPSUMMERING,
                                    true,
                                    "sporsmal 6.3.1",
                                ),
                                UtdypendeSporsmal(
                                    svar = "svar 6.3.2",
                                    Sporsmalstype.UTFORDRINGER_MED_GRADERT_ARBEID,
                                    true,
                                    "sporsmal 6.3.2",
                                ),
                                UtdypendeSporsmal(
                                    svar = "svar 6.3.3",
                                    Sporsmalstype.HENSYN_PA_ARBEIDSPLASSEN,
                                    true,
                                    "sporsmal 6.3.3",
                                ),
                            )
                    )
            )
        val pdfBytes = typstClient.createPdf(buildTypstPayload(sykmeldingRecord))

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
    fun `generate pdf for sykmelding med prognose ER_I_ARBEID`() {
        val record =
            xml.record.copy(
                sykmelding =
                    xml.sykmelding.copy(
                        metadata = xml.sykmelding.metadata.copy(regelsettVersjon = "2"),
                        prognose =
                            Prognose(
                                arbeidsforEtterPeriode = true,
                                hensynArbeidsplassen = "Trenger tilrettelagt arbeidsplass",
                                arbeid =
                                    IArbeid.ErIArbeid(
                                        egetArbeidPaSikt = true,
                                        annetArbeidPaSikt = false,
                                        arbeidFOM = LocalDate.of(2026, 8, 1),
                                        vurderingsdato = LocalDate.of(2026, 8, 15),
                                    ),
                            ),
                    )
            )
        val pdfBytes = typstClient.createPdf(buildTypstPayload(record))

        val fil = File("build/test.pdf")
        fil.writeBytes(pdfBytes)
        val tekst = extractTextFromPdf(fil)
        assert(tekst.contains("Pasient med arbeidsgiver: Utdypende opplysninger ved 7 uker")) {
            "Mangler 'ER_I_ARBEID'-overskrift"
        }
        assert(tekst.contains("Beskriv eventuelle hensyn som må tas på arbeidsplassen")) {
            "Mangler 5.1.1-etikett"
        }
        assert(tekst.contains("Trenger tilrettelagt arbeidsplass")) { "Mangler 5.1.1-verdi" }
        assert(tekst.contains("Jeg antar at pasienten på sikt kan komme tilbake til samme")) {
            "Mangler 5.2.1"
        }
        assert(tekst.contains("Anslå når du tror dette kan skje")) { "Mangler arbeidFOM-etikett" }
        assert(tekst.contains("01.08.2026")) { "Mangler arbeidFOM-verdi" }
        assert(tekst.contains("Jeg antar at pasienten på sikt kan komme i arbeid hos annen")) {
            "Mangler 5.2.2"
        }
        assert(tekst.contains("Hvis usikker: Når antar du å kunne gi tilbakemelding på dette?")) {
            "Mangler 5.2.3"
        }
        assert(tekst.contains("15.08.2026")) { "Mangler vurderingsdato-verdi" }

        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(fil)
        }
    }

    @Test
    fun `generate pdf for ICPC-2B`() {
        val record =
            xml.record.copy(
                sykmelding =
                    xml.sykmelding.copy(
                        medisinskVurdering =
                            xml.sykmelding.medisinskVurdering.copy(
                                hovedDiagnose =
                                    DiagnoseInfo(
                                        DiagnoseSystem.ICPC2B,
                                        "R74.0001",
                                        "diagnosebeskrivelse",
                                    ),
                                biDiagnoser =
                                    listOf(
                                        DiagnoseInfo(
                                            DiagnoseSystem.ICPC2B,
                                            "R74.0002",
                                            "annen diagnosebeskrivelse",
                                        )
                                    ),
                            ),
                        arbeidsgiver =
                            ArbeidsgiverInfo.Flere("Coop", "Butikkmedarbeider", 80, null, null),
                    )
            )
        val pdfBytes = typstClient.createPdf(buildTypstPayload(record))

        val fil = File("build/test.pdf")
        fil.writeBytes(pdfBytes)
        val tekst = extractTextFromPdf(fil)
        assert(tekst.contains("ICPC2B")) { "Mangler 'diagnosesystem'" }
        assert(tekst.contains("R74.0002")) { "Mangler 'diagnosekode'" }
        assert(tekst.contains("R74.0001")) { "Mangler 'diagnosekode'" }
    }

    @Test
    fun `generate pdf for sykmelding med hoveddiagnose og bidiagnoser`() {
        val sykmeldingRecord =
            digital.record.copy(
                sykmelding =
                    digital.sykmelding.copy(
                        utdypendeSporsmal =
                            listOf(
                                UtdypendeSporsmal(
                                    "Answer1",
                                    Sporsmalstype.MEDISINSK_OPPSUMMERING,
                                    sporsmal = "sporsmal 1",
                                ),
                                UtdypendeSporsmal(
                                    "Answer2",
                                    Sporsmalstype.UTFORDRINGER_MED_ARBEID,
                                    sporsmal = "sporsmal 2",
                                ),
                                UtdypendeSporsmal(
                                    "Answer3",
                                    Sporsmalstype.UTFORDRINGER_MED_GRADERT_ARBEID,
                                    sporsmal = "sporsmal 3",
                                ),
                                UtdypendeSporsmal(
                                    "Answer4",
                                    Sporsmalstype.HENSYN_PA_ARBEIDSPLASSEN,
                                    sporsmal = "sporsmal 4",
                                ),
                                UtdypendeSporsmal(
                                    "Answer5",
                                    Sporsmalstype.BEHANDLING_OG_FREMTIDIG_ARBEID,
                                    sporsmal = "sporsmal 5",
                                ),
                                UtdypendeSporsmal(
                                    "Answer6",
                                    Sporsmalstype.UAVKLARTE_FORHOLD,
                                    sporsmal = "sporsmal 6",
                                ),
                                UtdypendeSporsmal(
                                    "Answer7",
                                    Sporsmalstype.FORVENTET_HELSETILSTAND_UTVIKLING,
                                    sporsmal = "sporsmal 7",
                                ),
                                UtdypendeSporsmal(
                                    "Answer8",
                                    Sporsmalstype.MEDISINSKE_HENSYN,
                                    sporsmal = "sporsmal 8",
                                ),
                            )
                    )
            )
        val pdfBytes = typstClient.createPdf(buildTypstPayload(sykmeldingRecord))

        val fil = File("build/test.pdf")
        fil.writeBytes(pdfBytes)
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(fil)
        }
    }
}

fun extractTextFromPdf(file: File): String {
    Loader.loadPDF(file).use { document ->
        return PDFTextStripper().getText(document)
    }
}
