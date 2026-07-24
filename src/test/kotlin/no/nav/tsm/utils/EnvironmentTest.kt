package no.nav.tsm.utils

import com.typesafe.config.ConfigFactory
import io.kotest.matchers.equals.shouldEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import io.ktor.server.config.HoconApplicationConfig
import kotlin.test.Test

class EnvironmentTest {
    @Test
    fun `production environment should be properly configured, even lazy values`() {
        val applicationConfig =
            HoconApplicationConfig(
                ConfigFactory.parseMap(
                        mapOf(
                            // Nais injected values
                            "NAIS_POD_NAME" to "syk-inn-api-prod-123",
                            "NAIS_CLUSTER_NAME" to "prod-gcp",
                            "KAFKA_BROKERS" to "kafka-1:9092,kafka-2:9092",
                            "KAFKA_TRUSTSTORE_PATH" to "/var/run/secrets/kafka/truststore.jks",
                            "KAFKA_CREDSTORE_PASSWORD" to "credstore-password",
                            "KAFKA_KEYSTORE_PATH" to "/var/run/secrets/kafka/keystore.p12",
                            "NAIS_TOKEN_ENDPOINT" to "https://texas/token",

                            // Provided by nais-foo.yaml
                            "DOKARKIV_URL" to "https://dokarkiv.fss",
                            "TSM_SYKMELDING_BUCKET" to "bøtta",
                        )
                    )
                    .withFallback(ConfigFactory.parseResources("application.conf"))
                    .resolve()
            )

        val environment = initializeEnvironment(applicationConfig)

        // Poke lazy envs as well to ensure they are properly configured
        environment.external().shouldNotBeNull()

        environment.external().dokarkiv shouldEqual "https://dokarkiv.fss"
        environment.bucket shouldEqual "bøtta"
    }
}
