package no.nav.tsm.plugins

import dev.hayden.KHealth
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.core.instrument.Metrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

fun Application.configureMonitoring() {
    configureMicrometer()

    install(KHealth) {
        healthChecks { healthCheckPath = "/internal/health/alive" }
        readyChecks { readyCheckPath = "/internal/health/ready" }
    }
    install(ShutDownUrl.ApplicationCallPlugin) {
        shutDownUrl = "/internal/shutdown"
        exitCodeSupplier = { 0 }
    }
}

private fun Application.configureMicrometer() {
    val appRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    Metrics.addRegistry(appRegistry)

    install(MicrometerMetrics) { registry = appRegistry }
    routing { get("/internal/metrics") { call.respond(appRegistry.scrape()) } }
}
