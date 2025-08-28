package no.nav.journey.sykmelding.api

import no.nav.journey.config.texas.TexasClient
import no.nav.journey.sykmelding.models.journalpost.JournalpostRequest
import no.nav.journey.sykmelding.models.journalpost.JournalpostResponse
import no.nav.journey.utils.applog
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.RequestEntity
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder

@Component
class DokarkivClient(
    @param:Value("\${dokarkiv.url}") private val url: String,
    @param:Value("\${dokarkiv.scope}") private val scope: String,
    private val restTemplate: RestTemplate,
    private val texasClient: TexasClient,
) {
    val log = applog()
    fun createJournalpost(
        journalpostRequest: JournalpostRequest,
    ): JournalpostResponse? {
        val texasToken = texasClient.getTexasToken(scope)
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            accept = listOf(MediaType.APPLICATION_JSON)
            setBearerAuth(texasToken.access_token)
            set("Nav-Callid", journalpostRequest.eksternReferanseId)
        }
        val uri = UriComponentsBuilder
            .fromUriString(url)
            .queryParam("forsoekFerdigstill", true)
            .build()
            .toUri()

        val requestEntity = RequestEntity
            .post(uri)
            .headers(headers)
            .body(journalpostRequest)

        try {
            val response = restTemplate.exchange(requestEntity, JournalpostResponse::class.java)
            return response.body ?: throw RuntimeException("Tom respons fra dokarkiv")

        } catch (e: HttpClientErrorException) {
            if (e.statusCode == HttpStatus.CONFLICT) {
                try {
                    return e.getResponseBodyAs(JournalpostResponse::class.java)
                } catch (ex: Exception) {
                    log.error("Feil ved parsing av response fra dokarkiv when status = CONFLICT", ex)
                }
                log.error(
                    "Dokarkiv svarte med feil: status=${e.statusCode}, body=${e.responseBodyAsString}, Nav-Callid=${journalpostRequest.eksternReferanseId}",
                    e
                )
                return null
            }
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
