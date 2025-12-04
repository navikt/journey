package no.nav.journey.sykmelding.services

import com.google.cloud.storage.Blob
import com.google.cloud.storage.Storage
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import no.nav.journey.testUtils.gzip
import no.nav.journey.testUtils.loadResourceAsString
import no.nav.journey.utils.MetricRegister
import no.nav.journey.utils.XmlHandler
import no.nav.journey.utils.ungzip
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test


class BucketServiceTest {

    private val storage: Storage = mockk()
    private val metrics: MetricRegister = mockk()
    private val xmlHandler = XmlHandler()

    private val bucketService = BucketService("test-bucket", storage, metrics, xmlHandler)

    @Test
    fun `should return decompressed XML when blob exists`() {
        val sykmeldingId = "syk-123"
        val xml = loadResourceAsString("fellesformat/fellesformatMedVedlegg.xml")
        val compressed = gzip(xml)

        val blob = mockk<Blob>()
        every { storage.get("test-bucket", "$sykmeldingId/sykmelding.xml") } returns blob
        every { blob.exists() } returns true
        every { blob.getContent() }  returns ungzip(compressed)

        val counter = mockk<io.micrometer.core.instrument.Counter>()
        every { counter.increment() } just runs
        every { metrics.storageDownloadCounter("download") } returns counter

        val result = bucketService.downloadXml(sykmeldingId)
        assertNotNull(result)
    }

    @Test
    fun `should return null when blob does not exist`() {
        val sykmeldingId = "not-found"
        every { storage.get("test-bucket", "$sykmeldingId/sykmelding.xml") } returns null

        val counter = mockk<io.micrometer.core.instrument.Counter>()
        every { metrics.storageDownloadCounter("not_found") } returns counter
        every { counter.increment() } just runs

        val result = bucketService.downloadXml(sykmeldingId)
        assertNull(result)
    }

    @Test
    fun `get vedlegg from bucket`() {
        val sykmeldingId = "syk-123"
        val xml = loadResourceAsString("fellesformat/fellesformatMedVedlegg.xml")
        val compressed = gzip(xml)

        val blob = mockk<Blob>()
        every { storage.get("test-bucket", "$sykmeldingId/sykmelding.xml") } returns blob
        every { blob.exists() } returns true
        every { blob.getContent() } returns ungzip(compressed)

        val counter = mockk<io.micrometer.core.instrument.Counter>()
        every { counter.increment() } just runs
        every { metrics.storageDownloadCounter("download") } returns counter

        val result = bucketService.getVedleggFromBucket(sykmeldingId)
        assertNotNull(result)
    }
}
