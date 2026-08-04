package no.nav.tsm.utils

import io.ktor.server.config.ApplicationConfig
import io.ktor.server.config.getAs
import kotlin.time.Duration
import no.nav.tsm.ktor.nais.RuntimeCluster
import no.nav.tsm.ktor.nais.getRuntimeCluster

class Runtime(
    val env: RuntimeCluster,
    val name: String,
)

class ExternalApis(val dokarkiv: String)

class KafkaSykmeldingConsumer(
    val longPoll: Duration,
    val retryDelay: Duration,
)

class KafkaConfig(val sykmeldingConsumer: KafkaSykmeldingConsumer)

class Environment(
    val runtime: Runtime,
    val external: () -> ExternalApis,
    val kafka: KafkaConfig,
    val bucket: String,
)

fun initializeEnvironment(config: ApplicationConfig): Environment {
    val kafkaProperties =
        KafkaConfig(
            sykmeldingConsumer =
                KafkaSykmeldingConsumer(
                    longPoll = config.property("kafka.sykmeldingConsumer.longPoll").getAs(),
                    retryDelay = config.property("kafka.sykmeldingConsumer.retryDelay").getAs(),
                )
        )

    return Environment(
        kafka = kafkaProperties,
        runtime =
            Runtime(
                env = getRuntimeCluster(),
                name = config.property("app.name").getString(),
            ),
        external = { ExternalApis(dokarkiv = config.property("external.dokarkiv").getString()) },
        bucket = config.property("tsm.bucket").getString(),
    )
}
