package no.nav.journey.sykmelding.api

import no.nav.journey.config.texas.TexasClient
import no.nav.journey.sykmelding.models.journalpost.JournalpostRequest
import no.nav.journey.sykmelding.models.journalpost.JournalpostResponse
import no.nav.journey.utils.applog
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.RequestEntity
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import java.net.URI

@Component
class DokarkivClient(
    @Value("\${dokarkiv.url}") private val url: String,
    @Value("\${dokarkiv.scope}") private val scope: String,
    private val restTemplate: RestTemplate,
    private val texasClient: TexasClient,
) {
    val log = applog()

    fun createJournalpost(
        journalpostRequest: JournalpostRequest,
    ): JournalpostResponse {

        val texasToken = texasClient.getTexasToken(scope)
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            accept = listOf(MediaType.APPLICATION_JSON)
            setBearerAuth(texasToken.access_token)
            set("Nav-Callid", journalpostRequest.eksternReferanseId)
        }
        val requestEntity = RequestEntity
            .post(URI(url))
            .headers(headers)
            .body(journalpostRequest)

        try {
            val response = restTemplate.exchange(requestEntity, JournalpostResponse::class.java)
            return response.body ?: throw RuntimeException("Tom respons fra dokarkiv")

        } catch (e: HttpClientErrorException) {
            log.error(
                "Dokarkiv svarte med feil: status=${e.statusCode}, body=${e.responseBodyAsString}, Nav-Callid=${journalpostRequest.eksternReferanseId}",
                e
            )
            throw RuntimeException("Feil ved kall til dokarkiv", e)
        } catch (e: Exception) {
            log.error("Oppretting av journalpost feilet for callid=${journalpostRequest.eksternReferanseId}", e)
            throw RuntimeException("Ukjent feil ved kall til dokarkiv", e)
        }
    }
}