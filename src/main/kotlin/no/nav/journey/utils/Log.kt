package no.nav.journey.utils

import jakarta.annotation.PostConstruct
import no.nav.journey.sykmelding.models.Sykmelding
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.bridge.SLF4JBridgeHandler
import org.springframework.context.annotation.Configuration
import java.util.logging.LogManager

inline fun <reified T> T.applog(): Logger {
    return LoggerFactory.getLogger(T::class.java)
}
inline fun <reified T> T.securelog(): Logger {
    return LoggerFactory.getLogger("securelog")
}

@Configuration
class LoggingBridgeConfig {

    @PostConstruct
    fun redirectJulToSlf4j() {
        LogManager.getLogManager().reset()
        SLF4JBridgeHandler.removeHandlersForRootLogger()
        SLF4JBridgeHandler.install()
    }
}