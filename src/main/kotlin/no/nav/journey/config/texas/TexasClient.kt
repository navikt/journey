package no.nav.journey.config.texas

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.journey.utils.applog
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.RequestEntity
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import java.net.URI

data class TexasResponse(
    val access_token: String,
    val expires_in: Int,
    val token_type: String,
)

data class TexasRequest(
    val identity_provider: String,
    val target: String,
)

@Component
class TexasClient(private val restTemplate: RestTemplate,
                  @Value("\${nais.cluster}") private val cluster: String,
                  @Value("\${nais.texas.endpoint}") private val endpoint: String) {
    val log = applog()
    fun getTexasToken(service: String): TexasResponse {
        val texasRequest = TexasRequest(
            identity_provider = "azuread",
            target = "api://$cluster.$service/.default"
        )
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
        }

        val requestEntity = RequestEntity
            .post(URI(endpoint))
            .headers(headers)
            .body(texasRequest)
        try {
            val response = restTemplate.exchange(requestEntity, String::class.java)
            val raw = response.body ?: throw RuntimeException("Tom respons fra Texas")
            log.info("Texas rårespons (status ${response.statusCode}): $raw")

            return jacksonObjectMapper().readValue(raw, TexasResponse::class.java)
        } catch (e: HttpClientErrorException) {
            val rawError = e.responseBodyAsString
            log.error("Texas svarte med HTTP-feil: ${e.statusCode}, body: $rawError", e)
            throw RuntimeException("Texas svarte med feil ved token-kall", e)
        } catch (e: Exception) {
            log.error("Feil ved parsing av Texas-tokenrespons: ${e.message}", e)
            throw RuntimeException("Klarte ikke parse Texas-response", e)
        }
    }
}