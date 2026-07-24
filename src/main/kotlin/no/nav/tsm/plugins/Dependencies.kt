package no.nav.tsm.plugins

import io.ktor.client.HttpClient
import io.ktor.client.engine.apache5.*
import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import no.nav.tsm.ktor.auth.texas.TexasClient
import no.nav.tsm.ktor.di.dynamicDependencies
import no.nav.tsm.pdf.TypstClient
import no.nav.tsm.pdl.PdlCloudClient
import no.nav.tsm.pdl.PdlLocalClient
import no.nav.tsm.sykmelding.dokarkiv.DokarkivCloudClient
import no.nav.tsm.sykmelding.dokarkiv.DokarkivLocalClient
import no.nav.tsm.sykmelding.kafka.JournalOpprettetProducer
import no.nav.tsm.sykmelding.kafka.SykmeldingConsumer
import no.nav.tsm.sykmelding.services.BucketService
import no.nav.tsm.sykmelding.services.JournalpostService
import no.nav.tsm.sykmelding.services.SykmeldingService
import no.nav.tsm.utils.Environment
import no.nav.tsm.utils.initializeEnvironment

fun Application.configureDependencyInjection() {
    val config = environment.config

    dependencies {
        provide<Environment> { initializeEnvironment(config) }
        provide<HttpClient> { configureBaseHttpClient() }
        provide(TexasClient::class)
        provide(TypstClient::class)
        provide(SykmeldingService::class)
        provide(SykmeldingConsumer::class)
        provide(JournalpostService::class)
        provide(JournalOpprettetProducer::class)
        provide(BucketService::class)
    }

    dynamicDependencies {
        local {
            provide(PdlLocalClient::class)
            provide(DokarkivLocalClient::class)
        }
        cloud {
            provide(PdlCloudClient::class)
            provide(DokarkivCloudClient::class)
        }
    }
}

private fun configureBaseHttpClient(): HttpClient = HttpClient(Apache5) {}
