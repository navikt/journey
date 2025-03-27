package no.nav.journey.sykmelding.services

import com.google.cloud.storage.Blob
import com.google.cloud.storage.Storage
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import no.nav.journey.testUtils.gzip
import no.nav.journey.utils.MetricRegister
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test


class BucketServiceTest {

    private val storage: Storage = mockk()
    private val metrics: MetricRegister = mockk()

    private val bucketService = BucketService("test-bucket", storage, metrics)

    @Test
    fun `should return decompressed XML when blob exists`() {
        val sykmeldingId = "syk-123"
        val expectedXml = "<xml>hello</xml>"
        val compressed = gzip(expectedXml)

        val blob = mockk<Blob>()
        every { storage.get("test-bucket", sykmeldingId) } returns blob
        every { blob.exists() } returns true
        every { blob.getContent() } returns compressed

        val counter = mockk<io.micrometer.core.instrument.Counter>()
        every { counter.increment() } just runs
        every { metrics.storageDownloadCounter("download") } returns counter

        val result = bucketService.downloadXml(sykmeldingId)
        assertEquals(expectedXml, result)
    }

    @Test
    fun `should return null when blob does not exist`() {
        val sykmeldingId = "not-found"
        every { storage.get("test-bucket", sykmeldingId) } returns null

        val counter = mockk<io.micrometer.core.instrument.Counter>()
        every { metrics.storageDownloadCounter("not_found") } returns counter
        every { counter.increment() } just runs

        val result = bucketService.downloadXml(sykmeldingId)
        assertNull(result)
    }
}