package no.nav.journey.sykmelding.api

import no.nav.journey.config.texas.TexasClient
import no.nav.journey.sykmelding.models.journalpost.JournalpostRequest
import no.nav.journey.sykmelding.models.journalpost.JournalpostResponse
import no.nav.journey.utils.DokarkivException
import no.nav.journey.utils.applog
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.RequestEntity
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder

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

        return try {
            val response = restTemplate.exchange(requestEntity, JournalpostResponse::class.java)
            response.body ?: throw DokarkivException("Tom respons fra dokarkiv")
        } catch (e: HttpClientErrorException) {
            throw DokarkivException("Feil fra dokarkiv: status=${e.statusCode}, body=${e.responseBodyAsString}", e)
        } catch (e: Exception) {
            throw DokarkivException("Oppretting av journalpost feilet for callid=${journalpostRequest.eksternReferanseId}", e)
        }
    }
}