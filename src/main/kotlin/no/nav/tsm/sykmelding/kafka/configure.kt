package no.nav.tsm.sykmelding.kafka

import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import no.nav.tsm.ktor.kafka.producer.KafkaRecordProducer
import no.nav.tsm.ktor.kafka.producer.createProducer
import no.nav.tsm.ktor.kafka.sykmeldinger.SykmeldingerConsumer
import no.nav.tsm.ktor.logger
import no.nav.tsm.sykmelding.services.SykmeldingService
import no.nav.tsm.utils.Environment

fun Application.configureSykmeldingKafka() {
    val logger = logger()
    val config: Environment by dependencies
    val service: SykmeldingService by dependencies

    install(SykmeldingerConsumer) {
        clientId = config.runtime.name
        groupId = "journey-consumer"
        pollDuration = config.kafka.sykmeldingConsumer.longPoll
        retryDuration = config.kafka.sykmeldingConsumer.retryDelay
        onTombstone = { key ->
            logger.info("Mottok en sykmelding tombstone for ID $key, hopper over")
        }
        onRecord = { record, _ ->
            service.handleSykmelding(record)
        }
    }

    dependencies {
        provide<KafkaRecordProducer<JournalpostOpprettetRecord>> {
            this@configureSykmeldingKafka.createProducer(topic = "teamsykmelding.oppgave-journal-opprettet")
        }
    }
}
