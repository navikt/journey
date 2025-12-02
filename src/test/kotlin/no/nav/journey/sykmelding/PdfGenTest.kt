package no.nav.journey.sykmelding

import no.nav.journey.pdf.PdfService
import no.nav.journey.testUtils.TestUtils.Companion.januar
import no.nav.journey.testUtils.extractTextFromPdf
import no.nav.journey.testUtils.sykmeldingRecord
import no.nav.pdfgen.core.Environment
import no.nav.pdfgen.core.PDFGenCore
import no.nav.tsm.sykmelding.input.core.model.Behandlingsdager
import no.nav.tsm.sykmelding.input.core.model.BistandNav
import no.nav.tsm.sykmelding.input.core.model.FlereArbeidsgivere
import no.nav.tsm.sykmelding.input.core.model.metadata.Digital
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.verapdf.gf.foundry.VeraGreenfieldFoundryProvider
import java.awt.Desktop
import java.io.File
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
        assert(tekst.contains("Hvilke utfordringer har pasienten med å utføre gradert arbeid?")) { "Mangler 'UTFORDRINGER_MED_GRADERT_ARBEID'" }
        assert(tekst.contains("Hvilke hensyn må være på plass for at pasienten kan prøves i det nåværende arbeidet?")) { "Mangler 'HENSYN_PA_ARBEIDSPLASSEN'" }
        assert(tekst.contains("svar 6.3.1"))
        assert(tekst.contains("svar 6.3.2"))
        assert(tekst.contains("svar 6.3.3"))

    }

    @Test
    @Ignore
    fun `generate pdf for sykmelding med hoveddiagnose og bidiagnoser`() {
        val record = sykmeldingRecord {
            arbeidsgiver = FlereArbeidsgivere("Coop", "Butikkmedarbeider", 80, null, null)
        }
        val pdfBytes = pdfService.createPdf(record)!!

        val fil = File("build/test.pdf")
        fil.writeBytes(pdfBytes)
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(fil)
        }
    }
}
