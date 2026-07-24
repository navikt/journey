package no.nav.tsm.sykmelding.dokarkiv

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.fasterxml.jackson.databind.DeserializationFeature
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.jackson.jackson
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.tsm.ktor.auth.texas.TexasClient
import no.nav.tsm.ktor.auth.texas.TexasTarget
import no.nav.tsm.ktor.auth.texas.TexasToken
import no.nav.tsm.ktor.otel.failSpan
import no.nav.tsm.sykmelding.journalpost.JournalpostRequest
import no.nav.tsm.sykmelding.journalpost.JournalpostResponse
import no.nav.tsm.utils.Environment
import no.nav.tsm.utils.RuntimeEnvironments
import no.nav.tsm.utils.logger

class DokarkivCloudClient(
    httpClient: HttpClient,
    private val environment: Environment,
    private val texasClient: TexasClient,
) : DokarkivClient {
    private val logger = logger()

    private val httpClient: HttpClient = httpClient.config {
        install(ContentNegotiation) {
            jackson { configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false) }
        }
        install(HttpRequestRetry) {
            retryOnServerErrors(maxRetries = 3)
            exponentialDelay()
        }
    }

    @WithSpan
    override suspend fun createJournalpost(
        journalpostRequest: JournalpostRequest
    ): Either<DokarkivClient.JournalpostError, JournalpostResponse> {
        val span = Span.current()
        val (accessToken) = this.getToken()

        val response =
            httpClient.post("${environment.external().dokarkiv}?forsoekFerdigstill=true") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                bearerAuth(accessToken)
                headers { append("Nav-Callid", journalpostRequest.eksternReferanseId) }
                setBody(journalpostRequest)
            }

        return when {
            response.status.isSuccess() -> {
                span.setAttribute("dokrakiv.status", "ok")
                response.body<JournalpostResponse>().right()
            }

            response.status == HttpStatusCode.Conflict -> {
                span.setAttribute("dokrakiv.status", "conflict")
                try {
                    val conflictResponse = response.body<JournalpostResponse>()
                    conflictResponse.right()
                } catch (ex: Exception) {
                    logger.error(
                        "Feil ved parsing av response fra dokarkiv when status = CONFLICT",
                        ex.failSpan(),
                    )
                    DokarkivClient.JournalpostError.MALFORMED_CONFLICT.left()
                }
            }

            response.status == HttpStatusCode.NotFound -> {
                span.setAttribute("dokrakiv.status", "not_found")
                logger.error(
                    "Person not found in Dokarkiv for callid=${journalpostRequest.eksternReferanseId}"
                        .failSpan()
                )
                DokarkivClient.JournalpostError.PERSON_NOT_FOUND.left()
            }

            else -> {
                span.setAttribute("dokrakiv.status", "error")
                logger.error(
                    "Oppretting av journalpost feilet for callid=${journalpostRequest.eksternReferanseId}, status=${response.status}}"
                        .failSpan()
                )
                // See if there is any response body to log, but don't fail if it can't be read
                try {
                    response.body<String>().failSpan()
                } catch (_: Exception) {}

                DokarkivClient.JournalpostError.UNKNOWN_ERROR.left()
            }
        }
    }

    suspend fun getToken(): TexasToken =
        texasClient.entraIdToken(
            namespace = "teamdokumenthandtering",
            app = "dokarkiv",
            cluster = environment.runtime.env.toCorrespondingFSS(),
        )
}

private fun RuntimeEnvironments.toCorrespondingFSS(): TexasTarget {
    return when (this) {
        RuntimeEnvironments.DEV -> TexasTarget.DEV_FSS
        RuntimeEnvironments.PROD -> TexasTarget.PROD_FSS
        else ->
            throw IllegalStateException(
                "Unexpected 'app.runtime' configuration: ${this}. Should be one of 'dev-gcp' or 'prod-gcp'"
            )
    }
}
