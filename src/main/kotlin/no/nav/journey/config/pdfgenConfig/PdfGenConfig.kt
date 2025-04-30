package no.nav.journey.config.pdfgenConfig

import jakarta.annotation.PostConstruct
import no.nav.pdfgen.core.Environment
import no.nav.pdfgen.core.PDFGenCore
import org.springframework.context.annotation.Configuration
import org.verapdf.gf.foundry.VeraGreenfieldFoundryProvider


@Configuration
class PdfGenConfig {

    @PostConstruct
    fun init() {
        VeraGreenfieldFoundryProvider.initialise()
        val coreEnvironment = Environment()
        PDFGenCore.init(coreEnvironment)
    }
}
