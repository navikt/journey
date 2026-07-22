package no.nav.journey.sykmelding.services

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import no.nav.journey.pdf.TypstClient
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

    private lateinit var bucketService: BucketService
    private lateinit var dokarkivClient: DokarkivClient
    private lateinit var journalpostService: JournalpostService
    private lateinit var pdlClient: PdlClient

    @BeforeEach
    fun setup() {
        val typstClient = TypstClient(
            typstBinaryPath = "typst-pdf/typst",
            templatePath = "typst-pdf/sykmelding.typ",
            fontPath = "typst-pdf/fonts",
        )
        dokarkivClient = mockk()
        bucketService = mockk()
        pdlClient = mockk()
        journalpostService = JournalpostService(dokarkivClient, bucketService, typstClient, pdlClient)
    }


    @Test
    fun `happy case - create journalpost`() {
        val sykmeldingRecord = xml.record

        coEvery { dokarkivClient.createJournalpost(any()) } returns JournalpostResponse(
            emptyList(),
            "123",
            true,
            null,
            null
        )

        val result = journalpostService.createJournalpost(sykmeldingRecord)

        assertEquals("123", result)

        coVerify { dokarkivClient.createJournalpost(any()) }
    }

    @Test
    fun `create journalpost papirsykmelding`() {
        val sykmeldingRecord = papir.record

        val result = journalpostService.createJournalpost(sykmeldingRecord)

        assertEquals("123", result)

        coVerify(exactly = 0) { dokarkivClient.createJournalpost(any()) }
    }

    @Test
    fun `create journalpost utenlandsk`() {
        val sykmeldingRecord = utenlandsk.record

        val result = journalpostService.createJournalpost(sykmeldingRecord)

        assertEquals("123", result)

        coVerify(exactly = 0) { dokarkivClient.createJournalpost(any()) }
    }
}
