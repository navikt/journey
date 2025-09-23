package no.nav.journey.sykmelding.services

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import no.nav.journey.pdf.PdfService
import no.nav.journey.pdl.PdlClient
import no.nav.journey.sykmelding.api.DokarkivClient
import no.nav.journey.sykmelding.models.journalpost.JournalpostResponse
import no.nav.journey.testUtils.dummyOrganisasjon
import no.nav.journey.testUtils.sykmeldingRecord
import no.nav.tsm.sykmelding.input.core.model.Papirsykmelding
import no.nav.tsm.sykmelding.input.core.model.UtenlandskInfo
import no.nav.tsm.sykmelding.input.core.model.UtenlandskSykmelding
import no.nav.tsm.sykmelding.input.core.model.metadata.MessageInfo
import no.nav.tsm.sykmelding.input.core.model.metadata.Papir
import no.nav.tsm.sykmelding.input.core.model.metadata.Utenlandsk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class JournalpostServiceTest {

    private lateinit var pdfService: PdfService
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
        val sykmeldingRecord = sykmeldingRecord { sykmeldingId = "123" }
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
        val sykmeldingRecord = sykmeldingRecord {
            sykmeldingId = "123"
            metadata = Papir(
                msgInfo = MessageInfo(
                    type,
                    genDate = OffsetDateTime.now(ZoneOffset.UTC),
                    msgId = UUID.randomUUID().toString(),
                    migVersjon = "v1"
                ),
                sender = dummyOrganisasjon(),
                receiver = dummyOrganisasjon(),
                journalPostId = "123"
            )
            sykmelding = Papirsykmelding(
                id = sykmeldingId,
                pasient = pasient,
                medisinskVurdering = medisinskVurdering,
                aktivitet = aktivitet,
                metadata = createSykmeldingMetadata(),
                arbeidsgiver = arbeidsgiver,
                tiltak = tiltak,
                behandler = behandler,
                sykmelder = sykmelder,
                prognose = null,
                bistandNav = null,
                tilbakedatering = null,
                utdypendeOpplysninger = null
            )
        }

        val result = journalpostService.createJournalpost(sykmeldingRecord)

        assertEquals("123", result)

        coVerify(exactly = 0) { pdfService.createPdf(any()) }
        coVerify(exactly = 0) { dokarkivClient.createJournalpost(any()) }
    }

    @Test
    fun `create journalpost utenlandsk`() {
        val sykmeldingRecord = sykmeldingRecord {
            sykmeldingId = "123"
            metadata = Utenlandsk(
                land = "UTLAND",
                journalPostId = "123"
            )
            sykmelding = UtenlandskSykmelding(
                id = sykmeldingId,
                pasient =pasient,
                medisinskVurdering = medisinskVurdering,
                aktivitet = aktivitet,
                metadata = createSykmeldingMetadata(),
                utenlandskInfo = UtenlandskInfo(
                    land = "UTLAND",
                    folkeRegistertAdresseErBrakkeEllerTilsvarende = false,
                    erAdresseUtland = false
                )
            )
        }

        val result = journalpostService.createJournalpost(sykmeldingRecord)

        assertEquals("123", result)

        coVerify(exactly = 0) { pdfService.createPdf(any()) }
        coVerify(exactly = 0) { dokarkivClient.createJournalpost(any()) }
    }

}
