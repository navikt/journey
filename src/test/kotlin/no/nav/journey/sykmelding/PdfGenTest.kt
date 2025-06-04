package no.nav.journey.sykmelding

import no.nav.journey.sykmelding.models.FlereArbeidsgivere
import no.nav.journey.sykmelding.services.PdfService
import no.nav.journey.testUtils.extractTextFromPdf
import no.nav.journey.testUtils.sykmeldingRecord
import no.nav.pdfgen.core.Environment
import no.nav.pdfgen.core.PDFGenCore
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.verapdf.gf.foundry.VeraGreenfieldFoundryProvider
import java.awt.Desktop
import java.io.File

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
    @Disabled("generates pdf")
    fun `generate pdf for sykmelding med hoveddiagnose og bidiagnoser`() {
        val record = sykmeldingRecord {}
        val pdfBytes = pdfService.createPdf(record)!!

        val fil = File("build/test.pdf")
        fil.writeBytes(pdfBytes)
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(fil)
        }
    }
}