package no.nav.tsm

import io.ktor.server.application.Application
import no.nav.tsm.plugins.configureDependencyInjection
import no.nav.tsm.plugins.configureMonitoring
import no.nav.tsm.plugins.configureSerialization
import no.nav.tsm.sykmelding.kafka.configureSykmeldingKafkaConsumer

fun Application.module() {
    configureDependencyInjection()
    configureMonitoring()
    configureSerialization()

    configureSykmeldingKafkaConsumer()
}
