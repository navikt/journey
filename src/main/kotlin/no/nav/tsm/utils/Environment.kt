package no.nav.tsm.utils

import io.ktor.server.config.ApplicationConfig
import io.ktor.server.config.getAs
import java.util.Properties
import kotlin.time.Duration

enum class RuntimeEnvironments(val nais: String) {
    LOCAL("local"),
    DEV("dev-gcp"),
    PROD("prod-gcp"),
}

class Runtime(
    val env: RuntimeEnvironments,
    val name: String,
)

class ExternalApis(val dokarkiv: String)

class KafkaSykmeldingConsumer(val longPoll: Duration)

class KafkaConfig(val config: Properties, val sykmeldingConsumer: KafkaSykmeldingConsumer)

class Environment(
    val runtime: Runtime,
    val external: () -> ExternalApis,
    val kafka: KafkaConfig,
    val bucket: String,
)

fun initializeEnvironment(config: ApplicationConfig): Environment {
    val kafkaProperties =
        KafkaConfig(
            config =
                Properties().apply {
                    config.config("kafka.config").toMap().forEach { this[it.key] = it.value }
                },
            sykmeldingConsumer =
                KafkaSykmeldingConsumer(
                    longPoll = config.property("kafka.sykmeldingConsumer.longPoll").getAs()
                ),
        )

    println(config.config("kafka.config").toMap())

    return Environment(
        kafka = kafkaProperties,
        runtime =
            Runtime(
                env = config.inferRuntimeEnvironment(),
                name = config.property("app.name").getString(),
            ),
        external = { ExternalApis(dokarkiv = config.property("external.dokarkiv").getString()) },
        bucket = config.property("tsm.bucket").getString(),
    )
}

private fun ApplicationConfig.inferRuntimeEnvironment(): RuntimeEnvironments {
    return when (val configEnv = this.property("app.runtime").getString()) {
        "local" -> RuntimeEnvironments.LOCAL
        "prod-gcp" -> RuntimeEnvironments.PROD
        "dev-gcp" -> RuntimeEnvironments.DEV
        else -> {
            throw IllegalStateException(
                "Unexpected 'app.runtime' configuration: ${configEnv}. Should be one of 'local', 'dev-gcp' or 'prod-gcp'"
            )
        }
    }
}
