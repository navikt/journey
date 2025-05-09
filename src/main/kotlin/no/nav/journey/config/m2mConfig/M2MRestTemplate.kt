package no.nav.journey.config.m2mConfig

import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.web.client.RestTemplate

@Configuration
class M2MRestTemplate(
    private val restTemplateBuilder: RestTemplateBuilder,
    private val m2mTokenService: M2MTokenService,
) {

    @Bean
    fun dokarkivRestTemplate(): RestTemplate {
        return restTemplateBuilder
            .additionalInterceptors(bearerTokenInterceptor("dokarkiv-m2m"))
            .build()
    }
    private fun bearerTokenInterceptor(type: String): ClientHttpRequestInterceptor {
        return ClientHttpRequestInterceptor { request: HttpRequest, body: ByteArray, execution: ClientHttpRequestExecution ->
            val token = m2mTokenService.getM2MToken(type)
            request.headers.setBearerAuth(token)
            execution.execute(request, body)
        }
    }
}
