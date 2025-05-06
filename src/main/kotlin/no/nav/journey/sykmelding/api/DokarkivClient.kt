package no.nav.journey.sykmelding.api

import no.nav.journey.utils.applog
import no.nav.journey.utils.securelog
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class DokarkivClient(
    @Value("\${dokarkiv.url}") private val url: String,
    private val dokarkivRestTemplate: RestTemplate,
) {
    val log = applog()
    val securelog = securelog()

    fun opprettJournalpost(

    ){

    }








}