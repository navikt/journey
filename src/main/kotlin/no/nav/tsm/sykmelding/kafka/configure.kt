package no.nav.tsm.sykmelding.kafka

import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import no.nav.tsm.ktor.kafka.sykmeldinger.SykmeldingerConsumer
import no.nav.tsm.ktor.logger
import no.nav.tsm.sykmelding.services.SykmeldingService
import no.nav.tsm.utils.Environment

fun Application.configureSykmeldingKafkaConsumer() {
    val logger = logger()
    val config: Environment by dependencies
    val service: SykmeldingService by dependencies

    install(SykmeldingerConsumer) {
        groupId = "journey-consumer"
        pollDuration = config.kafka.sykmeldingConsumer.longPoll
        retryDuration = config.kafka.sykmeldingConsumer.retryDelay
        onTombstone = { key ->
            logger.info("Mottok en sykmelding tombstone for ID $key, hopper over")
        }
        onRecord = {
            service.handleSykmelding(it)
        }
    }
}
