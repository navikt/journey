package no.nav.journey.config.pdfgenConfig

import com.openhtmltopdf.slf4j.Slf4jLogger
import com.openhtmltopdf.util.XRLog
import jakarta.annotation.PostConstruct
import no.nav.journey.utils.applog
import no.nav.pdfgen.core.Environment
import no.nav.pdfgen.core.PDFGenCore
import org.springframework.context.annotation.Configuration
import org.verapdf.gf.foundry.VeraGreenfieldFoundryProvider


@Configuration
class PdfGenConfig {
    val log = applog()
    @PostConstruct
    fun init() {
        XRLog.setLoggerImpl(Slf4jLogger())
        try {
            log.info("Initializing VeraGreenfieldFoundryProvider")
            VeraGreenfieldFoundryProvider.initialise()
            log.info("VeraGreenfieldFoundryProvider initialized succesfully")
            log.info("Initlializing PDFGenCore with coreEnvironment")
            val coreEnvironment = Environment()
            PDFGenCore.init(coreEnvironment)
            log.info("PdfGenCore initialized successfully")
        } catch (e: Exception) {
            log.error("Failed to initialize PdfGenConfig: ${e.message}", e)
            throw RuntimeException("Failed to initialize PdfGenConfig: ${e.message}", e)
        }
    }
}
