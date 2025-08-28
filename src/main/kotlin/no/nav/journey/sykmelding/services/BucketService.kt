package no.nav.journey.sykmelding.services

import com.google.cloud.storage.Storage
import no.nav.helse.eiFellesformat.XMLEIFellesformat
import no.nav.journey.sykmelding.models.journalpost.Vedlegg
import no.nav.journey.utils.MetricRegister
import no.nav.journey.utils.XmlHandler
import no.nav.journey.utils.applog
import no.nav.journey.utils.ungzip
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class BucketService(
    @param:Value("\${tsm.bucket}") private val bucket: String,
    val storage: Storage,
    val metricRegister: MetricRegister,
    val xmlHandler: XmlHandler,
) {

    val log = applog()

    fun getVedleggFromBucket(sykmeldingId: String): List<Vedlegg>? {
        val fellesformat = downloadXml(sykmeldingId) ?: return null
        val vedlegg = xmlHandler.getVedlegg(fellesformat)
        log.info("Antall vedlegg ${vedlegg?.size} for sykmeldingId $sykmeldingId")
        return vedlegg
    }

    fun downloadXml(sykmeldingId: String): XMLEIFellesformat? {
        val blob = storage.get(bucket, sykmeldingId)
        return if (blob != null && blob.exists()) {
            val compressedData = blob.getContent()
            val decompressed = ungzip(compressedData)
            metricRegister.storageDownloadCounter("download").increment()
            log.info("Downloaded ${compressedData.size} vedlegg from sykmeldingId $sykmeldingId")
            return xmlHandler.unmarshal(decompressed)
        } else {
            metricRegister.storageDownloadCounter("not_found").increment()
            log.info("blob from bucket not found sykmmeldingId $sykmeldingId")
            null
        }
    }
}
