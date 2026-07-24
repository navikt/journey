package no.nav.tsm.sykmelding.services

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.right
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.tsm.ktor.otel.failSpan
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Base64
import no.nav.tsm.pdf.TypstClient
import no.nav.tsm.pdf.buildTypstPayload
import no.nav.tsm.pdf.imageToPDFA
import no.nav.tsm.pdl.PdlClient
import no.nav.tsm.pdl.PdlNavn
import no.nav.tsm.sykmelding.dokarkiv.DokarkivClient
import no.nav.tsm.sykmelding.input.core.model.Aktivitet
import no.nav.tsm.sykmelding.input.core.model.Behandler
import no.nav.tsm.sykmelding.input.core.model.RuleType
import no.nav.tsm.sykmelding.input.core.model.Sykmelder
import no.nav.tsm.sykmelding.input.core.model.Sykmelding
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import no.nav.tsm.sykmelding.input.core.model.TilbakedatertMerknad
import no.nav.tsm.sykmelding.input.core.model.ValidationResult
import no.nav.tsm.sykmelding.input.core.model.metadata.MessageMetadata
import no.nav.tsm.sykmelding.input.core.model.metadata.PersonIdType
import no.nav.tsm.sykmelding.input.core.model.sykmeldingObjectMapper
import no.nav.tsm.sykmelding.journalpost.AvsenderMottaker
import no.nav.tsm.sykmelding.journalpost.Bruker
import no.nav.tsm.sykmelding.journalpost.Dokument
import no.nav.tsm.sykmelding.journalpost.Dokumentvarianter
import no.nav.tsm.sykmelding.journalpost.GosysVedlegg
import no.nav.tsm.sykmelding.journalpost.JournalpostRequest
import no.nav.tsm.sykmelding.journalpost.Sak
import no.nav.tsm.sykmelding.journalpost.Vedlegg
import no.nav.tsm.sykmelding.services.util.validatePersonAndDNumber
import no.nav.tsm.utils.logger
import no.nav.tsm.utils.teamLogger

class JournalpostService(
    val dokarkivClient: DokarkivClient,
    val bucketService: BucketService,
    val typstClient: TypstClient,
    val pdlClient: PdlClient,
) {
    private val logger = logger()
    private val teamlog = teamLogger()

    @WithSpan
    suspend fun createJournalpost(
        sykmelding: SykmeldingRecord
    ): Either<DokarkivClient.JournalpostError, String> = either {
        val span = Span.current()

        if (!skalOpprettePdf(sykmelding)) {
            span.setAttribute("journalpost.skipped", true)
            span.setAttribute("journalpost.type", sykmelding.javaClass.simpleName)

            val metadata = sykmelding.metadata
            val journalpostId =
                when (metadata) {
                    is MessageMetadata.Papir -> metadata.journalPostId
                    is MessageMetadata.Utenlandsk -> metadata.journalPostId
                    else ->
                        throw IllegalArgumentException(
                            "Could not find journalpostId in metadata, sykmeldingId: ${sykmelding.sykmelding.id} "
                        ).failSpan()
                }

            return journalpostId.right()
        }

        span.setAttribute("journalpost.skipped", false)
        val vedlegg = getVedlegg(sykmelding)
        vedlegg?.forEach {
            teamlog.info(
                "vedlegg for sykmeldingId ${sykmelding.sykmelding.id}: type: ${it.type}, content-type: ${it.content.contentType}, description: ${it.description}"
            )
        }

        val pdf = typstClient.createPdf(buildTypstPayload(sykmelding))
        val journalpostPayload =
            createJournalPostRequest(sykmelding, vedlegg, pdf, sykmelding.validation)
        val response = dokarkivClient.createJournalpost(journalpostPayload).bind()

        logger.info(
            "Created journalpost for sykmelding ${sykmelding.sykmelding.id}, journalpost: ${response.journalpostId}"
        )
        return response.journalpostId.right()
    }

    private fun skalOpprettePdf(sykmeldingRecord: SykmeldingRecord): Boolean {
        return when (sykmeldingRecord.sykmelding) {
            is Sykmelding.Xml -> true
            is Sykmelding.Digital -> true
            else -> false
        }
    }

    private fun getVedlegg(sykmelding: SykmeldingRecord): List<Vedlegg>? {
        val metadata = sykmelding.metadata
        val vedlegg: List<String>? =
            when (metadata) {
                is MessageMetadata.Xml.Emottak -> metadata.vedlegg
                else -> null
            }
        if (!vedlegg.isNullOrEmpty()) {
            logger.info("skal hente vedlegg for sykmelding ${sykmelding.sykmelding.id}")
            return bucketService.getVedleggFromBucket(sykmelding.sykmelding.id)
        }
        return null
    }

    private suspend fun createJournalPostRequest(
        sykmelding: SykmeldingRecord,
        vedlegg: List<Vedlegg>?,
        pdf: ByteArray,
        validationResult: ValidationResult,
    ): JournalpostRequest {
        return JournalpostRequest(
            avsenderMottaker = sykmelding.sykmelding.createAvsenderMottakerDelegert(),
            bruker = Bruker(sykmelding.sykmelding.pasient.fnr, "FNR"),
            dokumenter = sykmelding.sykmelding.leggTilDokumenter(vedlegg, pdf, validationResult),
            eksternReferanseId = sykmelding.sykmelding.id,
            journalfoerendeEnhet = "9999",
            journalpostType = "INNGAAENDE",
            kanal = "HELSENETTET",
            sak = Sak(sakstype = "GENERELL_SAK"),
            tema = "SYM",
            tittel = sykmelding.sykmelding.createTittleJournalpost(validationResult),
        )
    }

    private fun Sykmelding.leggTilDokumenter(
        vedlegg: List<Vedlegg>?,
        pdf: ByteArray,
        validationResult: ValidationResult,
    ): List<Dokument> {
        val dokumenter = mutableListOf<Dokument>()

        dokumenter.add(
            Dokument(
                dokumentvarianter =
                    listOf(
                        Dokumentvarianter(
                            filnavn = "Sykmelding",
                            filtype = "PDFA",
                            variantformat = "ARKIV",
                            fysiskDokument = pdf,
                        ),
                        Dokumentvarianter(
                            filnavn = "Sykmelding json",
                            filtype = "JSON",
                            variantformat = "ORIGINAL",
                            fysiskDokument = sykmeldingObjectMapper.writeValueAsBytes(this),
                        ),
                    ),
                tittel = this.createTittleJournalpost(validationResult),
                brevkode = "NAV 08-07.04 A",
            )
        )

        vedlegg
            ?.filter { it.content.content.isNotEmpty() }
            ?.map { vedleggToPDF(toGosysVedlegg(it)) }
            ?.forEachIndexed { index, vedlegg ->
                dokumenter.add(
                    Dokument(
                        dokumentvarianter =
                            listOf(
                                Dokumentvarianter(
                                    filtype = findFiltype(vedlegg),
                                    filnavn = "Vedlegg_nr_${index}_Sykmelding_${this.id}",
                                    variantformat = "ARKIV",
                                    fysiskDokument = vedlegg.content,
                                )
                            ),
                        tittel = "Vedlegg til sykmelding ${getFomTomTekst(this.aktivitet)}",
                        brevkode = "NAV 08-07.04 A",
                    )
                )
            }
        return dokumenter
    }

    private fun toGosysVedlegg(vedlegg: Vedlegg): GosysVedlegg {
        return GosysVedlegg(
            contentType = vedlegg.type,
            content = Base64.getMimeDecoder().decode(vedlegg.content.content),
            description = vedlegg.description,
        )
    }

    private fun vedleggToPDF(vedlegg: GosysVedlegg): GosysVedlegg {
        if (findFiltype(vedlegg) == "PDFA") return vedlegg
        logger.info("Converting vedlegg of type ${vedlegg.contentType} to PDFA")

        val image =
            ByteArrayOutputStream().use { outputStream ->
                imageToPDFA(vedlegg.content.inputStream(), outputStream)
                outputStream.toByteArray()
            }

        return GosysVedlegg(
            content = image,
            contentType = "application/pdf",
            description = vedlegg.description,
        )
    }

    private fun findFiltype(vedlegg: GosysVedlegg): String =
        when (vedlegg.contentType) {
            "application/pdf" -> "PDFA"
            "image/tiff" -> "TIFF"
            "image/png" -> "PNG"
            "image/jpeg" -> "JPEG"
            else ->
                throw RuntimeException("Vedlegget er av av ukjent mimeType ${vedlegg.contentType}")
        }

    private fun Sykmelding.createTittleJournalpost(validationResult: ValidationResult): String {
        return if (validationResult.ugyldigTilbakedatering()) {
            "Avslått sykmelding ${getFomTomTekst(this.aktivitet)}"
        } else if (validationResult.status == RuleType.INVALID) {
            "Avvist sykmelding ${getFomTomTekst(this.aktivitet)}"
        } else if (validationResult.delvisGodkjent()) {
            "Delvis godkjent sykmelding ${getFomTomTekst(this.aktivitet)}"
        } else {
            "Sykmelding ${getFomTomTekst(this.aktivitet)}"
        }
    }

    private fun ValidationResult.ugyldigTilbakedatering(): Boolean {
        return rules.any {
            it.name == TilbakedatertMerknad.TILBAKEDATERING_UGYLDIG_TILBAKEDATERING.name
        }
    }

    private fun ValidationResult.delvisGodkjent(): Boolean {
        return rules.any { it.name == TilbakedatertMerknad.TILBAKEDATERING_DELVIS_GODKJENT.name }
    }

    private fun getFomTomTekst(aktiviteter: List<Aktivitet>): String =
        "${
            formaterDato(
                aktiviteter.sortedSykmeldingPeriodeFOMDate().first().fom
            )
        } -" +
                " ${
                    formaterDato(
                        aktiviteter.sortedSykmeldingPeriodeTOMDate().last().tom
                    )
                }"

    private fun List<Aktivitet>.sortedSykmeldingPeriodeFOMDate(): List<Aktivitet> = sortedBy {
        it.fom
    }

    private fun List<Aktivitet>.sortedSykmeldingPeriodeTOMDate(): List<Aktivitet> = sortedBy {
        it.tom
    }

    private fun formaterDato(dato: LocalDate): String {
        val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        return dato.format(formatter)
    }

    private suspend fun Sykmelding.createAvsenderMottakerDelegert(): AvsenderMottaker =
        when (this) {
            is Sykmelding.Xml -> createAvsenderMottaker(sykmelder, behandler, id)
            is Sykmelding.Digital -> createAvsenderMottaker(sykmelder, behandler, id)
            else ->
                throw IllegalArgumentException(
                    "Skal ikke opprette journalpost for sykmeldingtype ${this::class.simpleName}"
                )
        }

    private suspend fun createAvsenderMottaker(
        sykmelder: Sykmelder,
        behandler: Behandler,
        sykmeldingId: String,
    ): AvsenderMottaker {
        try {
            val sykmelderName =
                pdlClient
                    .getPerson(
                        sykmelder.ids
                            .first { it.type == PersonIdType.FNR || it.type == PersonIdType.DNR }
                            .id
                    )
                    .fold(
                        {
                            throw IllegalStateException(
                                "Could not find person in pdl, cause: ${it.name} (${sykmeldingId})"
                            )
                        },
                        {
                            it.navn
                                ?: throw IllegalStateException(
                                    "Person has no name in PDL (${sykmeldingId})"
                                )
                        },
                    )

            return AvsenderMottaker(
                id =
                    hprnummerMedRiktigLengdeOgFormat(
                        sykmelder.ids.first { it.type == PersonIdType.HPR }.id
                    ),
                idType = "HPRNR",
                navn = sykmelderName.formatName(),
            )
        } catch (e: Exception) {
            logger.warn(
                "Could not find person in pdl for sykmelder, trying with behandler. SykmeldingID $sykmeldingId",
                e,
            )
        }

        val hpr = behandler.ids.find { it.type == PersonIdType.HPR }?.id

        if (hpr != null && hpr.length >= 7 && hpr.length <= 9) {
            return AvsenderMottaker(
                id = hprnummerMedRiktigLengdeOgFormat(hpr),
                idType = "HPRNR",
                navn = behandler.formatName(),
            )
        }
        logger.warn("Could not find HPR for behandler, using fnr")
        val fnr =
            behandler.ids.find { it.type == PersonIdType.FNR && validatePersonAndDNumber(it.id) }
        if (fnr != null) {
            return AvsenderMottaker(
                id = fnr.id,
                idType = fnr.type.name,
                navn = behandler.formatName(),
            )
        }

        logger.warn("invalid behandler ids, using land = NORGE")
        return AvsenderMottaker(
            land = "Norge",
            navn = behandler.formatName(),
        )
    }

    private fun hprnummerMedRiktigLengdeOgFormat(hprnummer: String): String {
        val hprnummerKunTall = hprnummer.filter { it.isDigit() }
        if (hprnummerKunTall.length < 9) {
            return hprnummerKunTall.padStart(9, '0')
        }
        return hprnummerKunTall
    }

    private fun Behandler.formatName(): String =
        if (navn.mellomnavn == null) {
            "${navn.etternavn} ${navn.fornavn}"
        } else {
            "${navn.etternavn} ${navn.fornavn} ${navn.mellomnavn}"
        }

    private fun PdlNavn.formatName(): String =
        if (mellomnavn == null) {
            "$etternavn $fornavn"
        } else {
            "$etternavn $fornavn $mellomnavn"
        }
}
