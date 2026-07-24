package no.nav.tsm.sykmelding.kafka

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.plugins.di.dependencies
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import no.nav.tsm.sykmelding.services.SykmeldingService
import no.nav.tsm.utils.logger

fun Application.configureSykmeldingKafkaConsumer() {
    val logger = logger()
    val consumer: SykmeldingConsumer by dependencies
    val service: SykmeldingService by dependencies

    monitor.subscribe(ApplicationStarted) {
        launch(Dispatchers.IO) {
            consumer.subscribe()
            try {
                while (isActive) {
                    val records = consumer.poll()
                    for ((key, record) in records) {
                        if (record == null) {
                            logger.info("Mottok en sykmelding tombstone for ID $key, hopper over")
                            continue
                        }

                        service.handleSykmelding(record)
                    }
                }
            } finally {
                withContext(NonCancellable) { consumer.unsubscribe() }
            }
        }
    }
}
