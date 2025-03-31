package no.nav.journey.sykmelding.services

import com.google.cloud.storage.Storage
import no.nav.journey.utils.MetricRegister
import no.nav.journey.utils.applog
import no.nav.journey.utils.ungzip
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class BucketService(
    @Value("\${tsm.bucket}") private val bucket: String,
    val storage: Storage,
    val metricRegister: MetricRegister
) {

    val log = applog()

    fun getVedleggFromBucket(sykmeldingId: String) {
        downloadXml(sykmeldingId)
    }

    fun downloadXml(sykmeldingId: String): String? {
        val blob = storage.get(bucket, sykmeldingId)
        log.info("blob from bucket: {}", blob)

        return if (blob != null && blob.exists()) {
            val compressedData = blob.getContent()
            val decompressed = ungzip(compressedData)
            log.info("Decompressed bucket load $decompressed")
            metricRegister.storageDownloadCounter("download").increment()
            decompressed
        } else {
            metricRegister.storageDownloadCounter("not_found").increment()
            null
        }
    }










}