package no.nav.journey.sykmelding

import no.nav.journey.pdf.PdfService
import no.nav.journey.testUtils.TestUtils.Companion.januar
import no.nav.journey.testUtils.extractTextFromPdf
import no.nav.journey.testUtils.sykmeldingRecord
import no.nav.pdfgen.core.Environment
import no.nav.pdfgen.core.PDFGenCore
import no.nav.tsm.sykmelding.input.core.model.AnnenFravarArsakType
import no.nav.tsm.sykmelding.input.core.model.AnnenFraverArsak
import no.nav.tsm.sykmelding.input.core.model.Behandlingsdager
import no.nav.tsm.sykmelding.input.core.model.BistandNav
import no.nav.tsm.sykmelding.input.core.model.DiagnoseInfo
import no.nav.tsm.sykmelding.input.core.model.DiagnoseSystem
import no.nav.tsm.sykmelding.input.core.model.FlereArbeidsgivere
import no.nav.tsm.sykmelding.input.core.model.LegacyMedisinskVurdering
import no.nav.tsm.sykmelding.input.core.model.MedisinskVurdering
import no.nav.tsm.sykmelding.input.core.model.Sporsmalstype
import no.nav.tsm.sykmelding.input.core.model.UtdypendeSporsmal
import no.nav.tsm.sykmelding.input.core.model.Yrkesskade
import no.nav.tsm.sykmelding.input.core.model.metadata.Digital
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.verapdf.gf.foundry.VeraGreenfieldFoundryProvider
import java.awt.Desktop
import java.io.File
import kotlin.collections.listOf
import kotlin.test.Ignore

class PdfGenTest {
    private val pdfService = PdfService()

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
    fun `generate pdf for sykmelding flere arbeidsgivere`() {
        val recordMedFlereArbeidsgivere = sykmeldingRecord {
            arbeidsgiver = FlereArbeidsgivere("Coop", "Butikkmedarbeider", 80, null, null)
        }
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
        val sykmeldingRecord = sykmeldingRecord {
            sykmelding = createDigitalSykmelding()
            metadata = Digital("123456789")
        }
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
        val sykmeldingRecord = sykmeldingRecord {
            sykmelding = createDigitalSykmelding().copy(utdypendeSporsmal = listOf(
                UtdypendeSporsmal(svar = "svar 6.3.1", Sporsmalstype.MEDISINSK_OPPSUMMERING, true, "sporsmal 6.3.1"),
                UtdypendeSporsmal(svar = "svar 6.3.2", Sporsmalstype.UTFORDRINGER_MED_GRADERT_ARBEID, true, "sporsmal 6.3.2"),
                UtdypendeSporsmal(svar = "svar 6.3.3" , Sporsmalstype.HENSYN_PA_ARBEIDSPLASSEN, true, "sporsmal 6.3.3"),
            ))
            metadata = Digital("123456789")
        }
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
        val record = sykmeldingRecord {
            medisinskVurdering = LegacyMedisinskVurdering(
                hovedDiagnose = DiagnoseInfo( DiagnoseSystem.ICPC2B, "R74.0001", "diagnosebeskrivelse"),
                biDiagnoser = listOf(
                    hovedDiagnose, DiagnoseInfo(
                        DiagnoseSystem.ICPC2B, "R74.0002", "annen diagnosebeskrivelse"
                    )
                ),
                svangerskap = true,
                yrkesskade = null,
                skjermetForPasient = true,
                syketilfelletStartDato = sykmeldtFom,
                annenFraversArsak = null
            )
            arbeidsgiver = FlereArbeidsgivere("Coop", "Butikkmedarbeider", 80, null, null)
        }
        val pdfBytes = pdfService.createPdf(record)!!

        val fil = File("build/test.pdf")
        fil.writeBytes(pdfBytes)
        val tekst = extractTextFromPdf(fil)
        assert(tekst.contains("ICPC-2B")) { "Mangler 'diagnosesystem'" }
        assert(tekst.contains("R74.0002")) { "Mangler 'diagnosekode'" }
        assert(tekst.contains("R74.0001")) { "Mangler 'diagnosekode'" }

    }
    @Test
    @Ignore
    fun `generate pdf for sykmelding med hoveddiagnose og bidiagnoser`() {
        val sykmeldingRecord = sykmeldingRecord {
            sykmelding = createDigitalSykmelding().copy(
                utdypendeSporsmal = listOf(
                    UtdypendeSporsmal("Answer1", Sporsmalstype.MEDISINSK_OPPSUMMERING, sporsmal = "sporsmal 1"),
            UtdypendeSporsmal("Answer2", Sporsmalstype.UTFORDRINGER_MED_ARBEID, sporsmal = "sporsmal 2"),
            UtdypendeSporsmal("Answer3", Sporsmalstype.UTFORDRINGER_MED_GRADERT_ARBEID, sporsmal = "sporsmal 3"),
            UtdypendeSporsmal("Answer4", Sporsmalstype.HENSYN_PA_ARBEIDSPLASSEN, sporsmal = "sporsmal 4"),
            UtdypendeSporsmal("Answer5", Sporsmalstype.BEHANDLING_OG_FREMTIDIG_ARBEID, sporsmal = "sporsmal 5"),
            UtdypendeSporsmal("Answer6", Sporsmalstype.UAVKLARTE_FORHOLD, sporsmal = "sporsmal 6"),
            UtdypendeSporsmal("Answer7", Sporsmalstype.FORVENTET_HELSETILSTAND_UTVIKLING, sporsmal = "sporsmal 7"),
            UtdypendeSporsmal("Answer8", Sporsmalstype.MEDISINSKE_HENSYN, sporsmal = "sporsmal 8"),
            ))
            metadata = Digital("123456789")
        }
        val pdfBytes = pdfService.createPdf(sykmeldingRecord)!!

        val fil = File("build/test.pdf")
        fil.writeBytes(pdfBytes)
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(fil)
        }
    }
}
