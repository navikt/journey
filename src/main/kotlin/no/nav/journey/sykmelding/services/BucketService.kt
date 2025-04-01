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
    @Value("\${tsm.bucket}") private val bucket: String,
    val storage: Storage,
    val metricRegister: MetricRegister,
    val xmlHandler: XmlHandler,
) {

    val log = applog()

    fun getVedleggFromBucket(sykmeldingId: String): List<Vedlegg>? {
        val fellesformat = downloadXml(sykmeldingId) ?: return null // TODO: kan det være null????
        return xmlHandler.getVedlegg(fellesformat)
    }

    fun downloadXml(sykmeldingId: String): XMLEIFellesformat? {
        val blob = storage.get(bucket, sykmeldingId)
        log.info("blob from bucket: {}", blob)

        return if (blob != null && blob.exists()) {
            val compressedData = blob.getContent()
            val decompressed = ungzip(compressedData)
            metricRegister.storageDownloadCounter("download").increment()
            return xmlHandler.unmarshal(decompressed)
        } else {
            metricRegister.storageDownloadCounter("not_found").increment()
            null
        }
    }










}