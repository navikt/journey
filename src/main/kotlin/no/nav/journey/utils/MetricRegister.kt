package no.nav.journey.utils

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component

const val METRICS_NS = "tsm"

@Component
class MetricRegister(private val registry: MeterRegistry) {
    val reg = registry

    fun storageDownloadCounter(label: String): Counter {
        return Counter.builder("${METRICS_NS}_sykmelding_bucket_download")
            .tag("type", label)
            .register(registry)
    }
}
