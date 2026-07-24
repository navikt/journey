package no.nav.tsm.sykmelding.services

import arrow.core.right
import io.kotest.matchers.equals.shouldEqual
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import no.nav.tsm.pdf.TypstClient
import no.nav.tsm.pdl.PdlClient
import no.nav.tsm.sykmelding.dokarkiv.DokarkivClient
import no.nav.tsm.sykmelding.journalpost.JournalpostResponse
import testUtils.papir
import testUtils.utenlandsk
import testUtils.xml

class JournalpostServiceTest {

    val typstClient =
        TypstClient(
            typstBinaryPath = "typst-pdf/typst",
            templatePath = "typst-pdf/sykmelding.typ",
            fontPath = "typst-pdf/fonts",
        )

    private val bucketService: BucketService = mockk()
    private val dokarkivClient: DokarkivClient = mockk()
    private val pdlClient: PdlClient = mockk()
    private val journalpostService = JournalpostService(dokarkivClient, bucketService, typstClient, pdlClient)

    @Test
    fun `happy case - create journalpost`() = runTest {
        val sykmeldingRecord = xml.record

        coEvery { dokarkivClient.createJournalpost(any()) } returns
            JournalpostResponse(
                    dokumenter = emptyList(),
                    journalpostId = "123",
                    journalpostferdigstilt = true,
                    journalstatus = null,
                    melding = null,
                )
                .right()

        val result = journalpostService.createJournalpost(sykmeldingRecord)

        result.getOrNull() shouldEqual "123"

        coVerify(exactly = 1) { dokarkivClient.createJournalpost(any()) }
    }

    @Test
    fun `should not create journalpost for papirsykmelding`() = runTest {
        val sykmeldingRecord = papir.record

        val result = journalpostService.createJournalpost(sykmeldingRecord)

        result.getOrNull() shouldEqual "123"

        coVerify(exactly = 0) { dokarkivClient.createJournalpost(any()) }
    }

    @Test
    fun `should not create journalpost for utenlandsk sykmelding`() = runTest {
        val sykmeldingRecord = utenlandsk.record

        val result = journalpostService.createJournalpost(sykmeldingRecord)

        result.getOrNull() shouldEqual "123"

        coVerify(exactly = 0) { dokarkivClient.createJournalpost(any()) }
    }
}
