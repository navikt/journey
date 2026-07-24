package no.nav.tsm.pdl

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.*
import io.ktor.serialization.jackson.jackson
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.tsm.ktor.auth.texas.TexasClient
import no.nav.tsm.ktor.otel.failSpan
import no.nav.tsm.utils.logger

/** When working within the same cluster, the URL is always the same */
private const val PDL_CACHE_URL = "http://tsm-pdl-cache"

sealed interface PdlClient {

    enum class PdlErrors {
        NotFound,
        UnknownError,
    }

    suspend fun getPerson(ident: String): Either<PdlErrors, PdlPerson>
}

class PdlCloudClient(
    httpClient: HttpClient,
    private val texasClient: TexasClient,
) : PdlClient {
    private val logger = logger()

    private val pdlHttpClient = httpClient.config {
        install(ContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())

                // tsm-pdl-cache responds with some values we don't care about
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            }
        }
        install(HttpRequestRetry) {
            retryOnServerErrors(maxRetries = 5)
            exponentialDelay()
        }
    }

    @WithSpan
    override suspend fun getPerson(ident: String): Either<PdlClient.PdlErrors, PdlPerson> {
        val (token) = getToken()

        val response =
            pdlHttpClient.get("${PDL_CACHE_URL}/api/person") {
                headers {
                    append("Nav-Consumer-Id", "syk-inn-api")
                    append("Authorization", "Bearer $token")
                    append("Ident", ident)
                }
            }

        return when {
            response.status.isSuccess() ->
                try {
                    response.body<PdlPerson>().right()
                } catch (e: Exception) {
                    failSpan(Span.current(), e)
                    logger.error("Error deserializing PDL response", e)
                    return PdlClient.PdlErrors.UnknownError.left()
                }

            response.status == HttpStatusCode.NotFound -> PdlClient.PdlErrors.NotFound.left()
            else -> {
                logger.error("Unable to get person from pdl, see team logs for ident")
                PdlClient.PdlErrors.UnknownError.left()
            }
        }
    }

    private suspend fun getToken() = texasClient.entraIdToken("tsm", "tsm-pdl-cache")
}
