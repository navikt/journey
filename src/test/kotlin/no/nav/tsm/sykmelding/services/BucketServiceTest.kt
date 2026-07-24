package no.nav.tsm.sykmelding.services

import com.google.cloud.storage.Blob
import com.google.cloud.storage.Storage
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import no.nav.tsm.utils.Environment
import testUtils.loadResourceAsString

val testEnvironment =
    Environment(
        runtime = mockk(relaxed = true),
        kafka = mockk(relaxed = true),
        external = { mockk(relaxed = true) },
        bucket = "test-bucket",
    )

class BucketServiceTest {

    private val storage: Storage = mockk()
    private val bucketService =
        BucketService(
            environment = testEnvironment,
            storage = storage,
        )

    @Test
    fun `should return decompressed XML when blob exists`() {
        val sykmeldingId = "syk-123"
        val xml = loadResourceAsString("fellesformat/fellesformatMedVedlegg.xml")

        val blob = mockk<Blob>()
        every { storage.get("test-bucket", "$sykmeldingId/sykmelding.xml") } returns blob
        every { blob.exists() } returns true
        every { blob.getContent() } returns xml.toByteArray()

        val result = bucketService.downloadXml(sykmeldingId)
        result.shouldNotBeNull()
    }

    @Test
    fun `should return null when blob does not exist`() {
        val sykmeldingId = "not-found"
        every { storage.get("test-bucket", "$sykmeldingId/sykmelding.xml") } returns null

        val result = bucketService.downloadXml(sykmeldingId)
        result.shouldBeNull()
    }

    @Test
    fun `get vedlegg from bucket`() {
        val sykmeldingId = "syk-123"
        val xml = loadResourceAsString("fellesformat/fellesformatMedVedlegg.xml")

        val blob = mockk<Blob>()
        every { storage.get("test-bucket", "$sykmeldingId/sykmelding.xml") } returns blob
        every { blob.exists() } returns true
        every { blob.getContent() } returns xml.toByteArray()

        val result = bucketService.getVedleggFromBucket(sykmeldingId)
        result.shouldNotBeNull()
    }
}
