package no.nav.journey.sykmelding.services

import io.mockk.mockk
import no.nav.journey.sykmelding.api.DokarkivClient

class JournalpostServiceTest {

    val dokarkivClient = mockk<DokarkivClient>()
    val bucketService = mockk<BucketService>()
    val pdfService = mockk<PdfService>()
    val journalpostService = JournalpostService(dokarkivClient, bucketService, pdfService)

}