package no.nav.journey.sykmelding.services

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import no.nav.journey.pdf.PdfServiceOld
import no.nav.journey.pdl.PdlClient
import no.nav.journey.sykmelding.api.DokarkivClient
import no.nav.journey.sykmelding.models.journalpost.JournalpostResponse
import no.nav.journey.testUtils.papir
import no.nav.journey.testUtils.utenlandsk
import no.nav.journey.testUtils.xml
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class JournalpostServiceTest {

    private lateinit var pdfService: PdfServiceOld
    private lateinit var bucketService: BucketService
    private lateinit var dokarkivClient: DokarkivClient
    private lateinit var journalpostService: JournalpostService
    private lateinit var pdlClient: PdlClient

    @BeforeEach
    fun setup() {
        pdfService = mockk()
        dokarkivClient = mockk()
        bucketService = mockk()
        pdlClient = mockk()
        journalpostService = JournalpostService(dokarkivClient, bucketService, pdfService, pdlClient)
    }


    @Test
    fun `happy case - create journalpost`() {
        val sykmeldingRecord = xml.record
        val pdfBytes = "pdf-content".toByteArray()

        coEvery { pdfService.createPdf(sykmeldingRecord) } returns pdfBytes
        coEvery { dokarkivClient.createJournalpost(any()) } returns JournalpostResponse(
            emptyList(),
            "123",
            true,
            null,
            null
        )

        val result = journalpostService.createJournalpost(sykmeldingRecord)

        assertEquals("123", result)

        coVerify { pdfService.createPdf(sykmeldingRecord) }
        coVerify { dokarkivClient.createJournalpost(any()) }
    }

    @Test
    fun `create journalpost papirsykmelding`() {
        val sykmeldingRecord = papir.record

        val result = journalpostService.createJournalpost(sykmeldingRecord)

        assertEquals("123", result)

        coVerify(exactly = 0) { pdfService.createPdf(any()) }
        coVerify(exactly = 0) { dokarkivClient.createJournalpost(any()) }
    }

    @Test
    fun `create journalpost utenlandsk`() {
        val sykmeldingRecord = utenlandsk.record

        val result = journalpostService.createJournalpost(sykmeldingRecord)

        assertEquals("123", result)

        coVerify(exactly = 0) { pdfService.createPdf(any()) }
        coVerify(exactly = 0) { dokarkivClient.createJournalpost(any()) }
    }

}
