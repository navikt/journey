package no.nav.journey.sykmelding.api

import net.logstash.logback.argument.StructuredArguments.fields
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.journey.sykmelding.models.journalpost.JournalpostRequest
import no.nav.journey.sykmelding.models.journalpost.JournalpostResponse
import no.nav.journey.utils.applog
import no.nav.journey.utils.securelog
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class DokarkivClient(
    @Value("\${dokarkiv.url}") private val url: String,
    private val dokarkivRestTemplate: RestTemplate,
) {
    val log = applog()

    fun createJournalpost(
        journalpostRequest: JournalpostRequest,
    ): JournalpostResponse {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.accept = listOf(MediaType.APPLICATION_JSON)
        headers["Nav-Callid"] = journalpostRequest.eksternReferanseId
        log.info("Kall til dokarkiv Nav-Callid {}", journalpostRequest.eksternReferanseId,)

        try {
            val response = dokarkivRestTemplate.exchange(
                url,
                HttpMethod.POST,
                HttpEntity(journalpostRequest, headers),
                JournalpostResponse::class.java
            )
            if (response.statusCode == HttpStatus.OK) {
                return response.body!!
            } else {
                log.error(
                    "Mottok uventet statuskode fra dokarkiv: {}, Nav-Callid {},",
                    response.statusCode,
                    journalpostRequest.eksternReferanseId,
                )
                throw RuntimeException(
                    "Mottok uventet statuskode fra dokarkiv: ${response.statusCode}",
                )
            }

        } catch (e: Exception) {
            log.error("Oppretting av journalpost feilet for callid: ${journalpostRequest.eksternReferanseId} ", e)
            throw e
        }
    }
}