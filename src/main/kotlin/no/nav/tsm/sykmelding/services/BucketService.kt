package no.nav.tsm.sykmelding.services

import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import no.nav.helse.eiFellesformat.XMLEIFellesformat
import no.nav.tsm.sykmelding.journalpost.Vedlegg
import no.nav.tsm.sykmelding.utils.XmlHandler
import no.nav.tsm.utils.Environment
import no.nav.tsm.utils.Metrics
import no.nav.tsm.utils.logger

class BucketService(
    val environment: Environment,
    val storage: Storage = StorageOptions.getDefaultInstance().service,
) {
    private val logger = logger()

    fun getVedleggFromBucket(sykmeldingId: String): List<Vedlegg>? {
        val fellesformat = downloadXml(sykmeldingId) ?: return null
        val vedlegg = XmlHandler.getVedlegg(fellesformat)
        logger.info("Antall vedlegg ${vedlegg?.size} for sykmeldingId $sykmeldingId")
        return vedlegg
    }

    fun downloadXml(sykmeldingId: String): XMLEIFellesformat? {
        val blob = storage.get(environment.bucket, "$sykmeldingId/sykmelding.xml")
        return if (blob != null && blob.exists()) {
            val content = blob.getContent()
            Metrics.storageDownloadCounter.increment()
            logger.info("Downloaded ${content.size / 1024} KB vedlegg from sykmeldingId $sykmeldingId")
            XmlHandler.unmarshal(content.toString(Charsets.UTF_8))
        } else {
            Metrics.storageNotFoundCounter.increment()
            logger.info("blob from bucket not found sykmmeldingId $sykmeldingId")
            null
        }
    }
}
