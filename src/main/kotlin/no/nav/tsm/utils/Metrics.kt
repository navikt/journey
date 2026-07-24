package no.nav.tsm.utils

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics

const val METRICS_NS = "tsm"

object Metrics {
    val storageDownloadCounter: Counter =
        Counter.builder("${METRICS_NS}_sykmelding_bucket_download")
            .tag("type", "download")
            .register(Metrics.globalRegistry)

    val storageNotFoundCounter: Counter =
        Counter.builder("${METRICS_NS}_sykmelding_bucket_download")
            .tag("type", "not_found")
            .register(Metrics.globalRegistry)
}
