package no.nav.journey.sykmelding.services

import no.nav.journey.pdf.PdfService
import no.nav.journey.sykmelding.api.DokarkivClient
import no.nav.journey.sykmelding.models.journalpost.AvsenderMottaker
import no.nav.journey.sykmelding.models.journalpost.Bruker
import no.nav.journey.sykmelding.models.journalpost.Dokument
import no.nav.journey.sykmelding.models.journalpost.Dokumentvarianter
import no.nav.journey.sykmelding.models.journalpost.GosysVedlegg
import no.nav.journey.sykmelding.models.journalpost.JournalpostRequest
import no.nav.journey.sykmelding.models.journalpost.Sak
import no.nav.journey.sykmelding.models.journalpost.Vedlegg
import no.nav.journey.sykmelding.services.util.validatePersonAndDNumber
import no.nav.journey.utils.applog
import no.nav.journey.utils.teamLogger
import no.nav.pdfgen.core.objectMapper
import no.nav.pdfgen.core.pdf.createPDFA
import no.nav.tsm.sykmelding.input.core.model.Aktivitet
import no.nav.tsm.sykmelding.input.core.model.Behandler
import no.nav.tsm.sykmelding.input.core.model.DigitalSykmelding
import no.nav.tsm.sykmelding.input.core.model.RuleType
import no.nav.tsm.sykmelding.input.core.model.Sykmelding
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import no.nav.tsm.sykmelding.input.core.model.TilbakedatertMerknad
import no.nav.tsm.sykmelding.input.core.model.ValidationResult
import no.nav.tsm.sykmelding.input.core.model.XmlSykmelding
import no.nav.tsm.sykmelding.input.core.model.metadata.EmottakEnkel
import no.nav.tsm.sykmelding.input.core.model.metadata.Papir
import no.nav.tsm.sykmelding.input.core.model.metadata.PersonIdType
import no.nav.tsm.sykmelding.input.core.model.metadata.Utenlandsk
import no.nav.tsm.sykmelding.input.core.model.sykmeldingObjectMapper
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Base64

@Service
class JournalpostService(
    val dokarkivClient: DokarkivClient,
    val bucketService: BucketService,
    val pdfService: PdfService,
) {
    val log = applog()
    val teamlog = teamLogger()

    fun createJournalpost(
        sykmelding: SykmeldingRecord,
    ): String? {
        if (!skalOpprettePdf(sykmelding)) {
            val metadata = sykmelding.metadata
            val journalpostId = when (metadata) {
                is Papir -> metadata.journalPostId
                is Utenlandsk -> metadata.journalPostId
                else -> throw IllegalArgumentException("Could not find journalpostId in metadata, sykmeldingId: ${sykmelding.sykmelding.id} ")
            }
            return journalpostId
        }
        val vedlegg = getVedlegg(sykmelding)
        if(!vedlegg.isNullOrEmpty()) {
            teamlog.info("vedlegg for sykmeldingId ${sykmelding.sykmelding.id}: ${objectMapper.writeValueAsString(vedlegg)}")
        }
        val pdf = pdfService.createPdf(sykmelding) ?: throw Exception("sykmeldingid=${sykmelding.sykmelding.id} pdf er null")
        val journalpostPayload = createJournalPostRequest(sykmelding, vedlegg, pdf, sykmelding.validation)
        log.info("Creating journalpost for sykmelding ${sykmelding.sykmelding.id}")
        val response = dokarkivClient.createJournalpost(journalpostPayload)
        log.info("Created journalpost for sykmelding ${sykmelding.sykmelding.id}, journalpost: ${response?.journalpostId}")
        return response?.journalpostId
    }

    private fun skalOpprettePdf(sykmeldingRecord: SykmeldingRecord): Boolean {
        return when (sykmeldingRecord.sykmelding) {
            is XmlSykmelding -> true
            is DigitalSykmelding -> true
            else -> false
        }
    }

    fun getVedlegg(sykmelding: SykmeldingRecord): List<Vedlegg>? {
        val metadata = sykmelding.metadata
        if (metadata is EmottakEnkel) {
            if (!metadata.vedlegg.isNullOrEmpty()){
                log.info("skal hente vedlegg for sykmelding ${sykmelding.sykmelding.id}")
                val vedlegg = bucketService.getVedleggFromBucket(sykmelding.sykmelding.id)
                return vedlegg
            }
        }
        return null
    }

    private fun createJournalPostRequest(
        sykmelding: SykmeldingRecord,
        vedlegg: List<Vedlegg>?,
        pdf: ByteArray,
        validationResult: ValidationResult
    ): JournalpostRequest {
        return JournalpostRequest(
            avsenderMottaker = sykmelding.sykmelding.createAvsenderMottakerDelegert(),
            bruker = Bruker(sykmelding.sykmelding.pasient.fnr, "FNR"),
            dokumenter = sykmelding.sykmelding.leggTilDokumenter(vedlegg, pdf, validationResult),
            eksternReferanseId = sykmelding.sykmelding.id,
            journalfoerendeEnhet = "9999",
            journalpostType = "INNGAAENDE",
            kanal = "HELSENETTET",
            sak = Sak(
                sakstype = "GENERELL_SAK",
            ),
            tema = "SYM",
            tittel = sykmelding.sykmelding.createTittleJournalpost(validationResult).also {
                log.info("Creating tittel: $it for sykmeldingId ${sykmelding.sykmelding.id}")
            }
        )
    }

    fun Sykmelding.leggTilDokumenter(
        vedlegg: List<Vedlegg>?,
        pdf: ByteArray,
        validationResult: ValidationResult
    ): List<Dokument> {
        val dokumenter = mutableListOf<Dokument>()

        dokumenter.add(
            Dokument(
                dokumentvarianter = listOf(
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
                    )
                ),
                tittel = this.createTittleJournalpost(validationResult),
                brevkode = "NAV 08-07.04 A",
            )
        )

        vedlegg?.filter { it.content.content.isNotEmpty() }
            ?.map { vedleggToPDF(toGosysVedlegg(it)) }
            ?.forEachIndexed { index, vedlegg ->
                dokumenter.add(
                    Dokument(
                        dokumentvarianter = listOf(
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

    fun toGosysVedlegg(vedlegg: Vedlegg): GosysVedlegg {
        return GosysVedlegg(
            contentType = vedlegg.type,
            content = Base64.getMimeDecoder().decode(vedlegg.content.content),
            description = vedlegg.description,
        )
    }
    fun vedleggToPDF(vedlegg: GosysVedlegg): GosysVedlegg {
        if (findFiltype(vedlegg) == "PDFA") return vedlegg
        log.info("Converting vedlegg of type ${vedlegg.contentType} to PDFA")

        val image =
            ByteArrayOutputStream().use { outputStream ->
                createPDFA(vedlegg.content.inputStream(), outputStream)
                outputStream.toByteArray()
            }

        return GosysVedlegg(
            content = image,
            contentType = "application/pdf",
            description = vedlegg.description,
        )
    }


    fun findFiltype(vedlegg: GosysVedlegg): String =
        when (vedlegg.contentType) {
            "application/pdf" -> "PDFA"
            "image/tiff" -> "TIFF"
            "image/png" -> "PNG"
            "image/jpeg" -> "JPEG"
            else -> throw RuntimeException("Vedlegget er av av ukjent mimeType ${vedlegg.contentType}")
        }

    private fun Sykmelding.createTittleJournalpost(
        validationResult: ValidationResult
    ): String {
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

    fun ValidationResult.ugyldigTilbakedatering(): Boolean {
        return rules.any { it.name == TilbakedatertMerknad.TILBAKEDATERING_UGYLDIG_TILBAKEDATERING.name }
    }
    fun ValidationResult.delvisGodkjent(): Boolean {
        return rules.any { it.name == TilbakedatertMerknad.TILBAKEDATERING_DELVIS_GODKJENT.name }
    }


    private fun getFomTomTekst(aktiviteter: List<Aktivitet>): String =
        "${
            formaterDato(
                aktiviteter.sortedSykmeldingPeriodeFOMDate().first().fom,
            )
        } -" +
                " ${
                    formaterDato(
                        aktiviteter.sortedSykmeldingPeriodeTOMDate().last().tom,
                    )
                }"


    fun List<Aktivitet>.sortedSykmeldingPeriodeFOMDate(): List<Aktivitet> = sortedBy { it.fom }

    fun List<Aktivitet>.sortedSykmeldingPeriodeTOMDate(): List<Aktivitet> = sortedBy { it.tom }
    fun formaterDato(dato: LocalDate): String {
        val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        return dato.format(formatter)
    }

    fun Sykmelding.createAvsenderMottakerDelegert(): AvsenderMottaker = when (this) {
        is XmlSykmelding -> behandler.createAvsenderMottaker()
        is DigitalSykmelding -> behandler.createAvsenderMottaker()
        else -> throw IllegalArgumentException("Skal ikke opprette journalpost for sykmeldingtype ${this::class.simpleName}")
    }

    fun Behandler.createAvsenderMottaker(): AvsenderMottaker {
        val hpr = ids.find { it.type == PersonIdType.HPR }?.id

        if (hpr != null && hpr.length >= 7 && hpr.length <= 9 ) {
            return AvsenderMottaker(
                id = hprnummerMedRiktigLengdeOgFormat(hpr),
                idType = "HPRNR",
                navn = formatName()
            )
        }
        log.warn("HPR is null or invalid size, using fnr instead")
        val fnr = ids.find { it.type == PersonIdType.FNR && validatePersonAndDNumber(it.id) }
        if(fnr != null) {
            return AvsenderMottaker(
                    id = fnr.id,
                    idType = fnr.type.name,
                    navn = formatName()
                )
        }

        log.warn("invalid behandler ids, using land = NORGE")
        return AvsenderMottaker(
            land = "Norge",
            navn = formatName(),
        )
    }

    private fun hprnummerMedRiktigLengdeOgFormat(hprnummer: String): String {
        val hprnummerKunTall = hprnummer.filter { it.isDigit() }
        if (hprnummerKunTall.length < 9) {
            return hprnummerKunTall.padStart(9, '0')
        }
        return hprnummerKunTall
    }

    fun Behandler.formatName(): String =
        if (navn.mellomnavn == null) {
            "${navn.etternavn} ${navn.fornavn}"
        } else {
            "${navn.etternavn} ${navn.fornavn} ${navn.mellomnavn}"
        }


}

