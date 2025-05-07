package no.nav.journey.sykmelding.services

import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.journey.sykmelding.api.DokarkivClient
import no.nav.journey.sykmelding.models.Aktivitet
import no.nav.journey.sykmelding.models.Behandler
import no.nav.journey.sykmelding.models.SykmeldingRecord
import no.nav.journey.sykmelding.models.XmlSykmelding
import no.nav.journey.sykmelding.models.journalpost.AvsenderMottaker
import no.nav.journey.sykmelding.models.journalpost.Bruker
import no.nav.journey.sykmelding.models.journalpost.Dokument
import no.nav.journey.sykmelding.models.journalpost.Dokumentvarianter
import no.nav.journey.sykmelding.models.journalpost.GosysVedlegg
import no.nav.journey.sykmelding.models.journalpost.JournalpostRequest
import no.nav.journey.sykmelding.models.journalpost.Sak
import no.nav.journey.sykmelding.models.journalpost.Vedlegg
import no.nav.journey.sykmelding.models.metadata.EmottakEnkel
import no.nav.journey.sykmelding.models.metadata.MetadataType
import no.nav.journey.sykmelding.models.metadata.PersonIdType
import no.nav.journey.sykmelding.models.validation.RuleType
import no.nav.journey.sykmelding.models.validation.TilbakedatertMerknad
import no.nav.journey.sykmelding.models.validation.ValidationResult
import no.nav.journey.sykmelding.services.util.objectMapper
import no.nav.journey.sykmelding.services.util.validatePersonAndDNumber
import no.nav.journey.utils.applog
import no.nav.journey.utils.securelog
import no.nav.pdfgen.core.pdf.createPDFA
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
    val securelog = securelog()

    fun createJournalpost(
        sykmelding: SykmeldingRecord,
    ) {
        val metadataType = sykmelding.metadata.type
        if (metadataType != MetadataType.EMOTTAK){
            log.info("Oppretter ikke ny pdf for papirsykmelding ${sykmelding.sykmelding.id} fordi metadataType er: $metadataType")
            return
        }
        val vedlegg = getVedlegg(sykmelding)
        val pdf = pdfService.createPdf(sykmelding) ?: throw Exception("sykmeldingid=${sykmelding.sykmelding.id} pdf er null")
        val journalpostPayload = createJournalPostRequest(sykmelding, vedlegg, pdf, sykmelding.validation)
        securelog.info(
            "Journalpost avsender: " + journalpostPayload.avsenderMottaker.toString() + "{} {}",
            kv("SykmeldingId", sykmelding.sykmelding.id)
        )
        dokarkivClient.createJournalpost(journalpostPayload)
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
        val xmlSykmelding = (sykmelding.sykmelding as? XmlSykmelding) ?: throw IllegalArgumentException("The provided sykmelding is not of type XmlSykmelding id ${sykmelding.sykmelding.id}")

        return JournalpostRequest(
            avsenderMottaker = createAvsenderMottaker(xmlSykmelding),
            bruker = Bruker(xmlSykmelding.pasient.fnr, "FNR"),
            dokumenter = leggTilDokumenter(vedlegg, xmlSykmelding, pdf, validationResult),
            eksternReferanseId = xmlSykmelding.id,
            journalfoerendeEnhet = "9999",
            journalpostType = "INNGAAENDE",
            kanal = "HELSENETTET",
            sak = Sak(
                sakstype = "GENERELL_SAK",
            ),
            tema = "SYM",
            tittel = createTittleJournalpost(xmlSykmelding, validationResult)
        )
    }

    private fun leggTilDokumenter(
        vedlegg: List<Vedlegg>?,
        xmlSykmelding: XmlSykmelding,
        pdf: ByteArray,
        validationResult: ValidationResult
    ): List<Dokument>? {
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
                        fysiskDokument = objectMapper.writeValueAsBytes(xmlSykmelding),
                    )
                ),
                tittel = createTittleJournalpost(xmlSykmelding, validationResult),
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
                                filnavn = "Vedlegg_nr_${index}_Sykmelding_${xmlSykmelding.id}",
                                variantformat = "ARKIV",
                                fysiskDokument = vedlegg.content,
                            )
                        ),
                        tittel = "Vedlegg til sykmelding ${getFomTomTekst(xmlSykmelding.aktivitet)}",
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


    private fun createTittleJournalpost(
        xmlSykmelding: XmlSykmelding,
        validationResult: ValidationResult
    ): String {
        return if (validationResult.status == RuleType.INVALID) {
            "Avvist sykmelding ${getFomTomTekst(xmlSykmelding.aktivitet)}"
        } else if (validationResult.ugyldigTilbakedatering()) {
            "Avslått sykmelding ${getFomTomTekst(xmlSykmelding.aktivitet)}"
        } else if (validationResult.delvisGodkjent()) {
            "Delvis godkjent sykmelding ${getFomTomTekst(xmlSykmelding.aktivitet)}"
        } else {
            "Sykmelding ${getFomTomTekst(xmlSykmelding.aktivitet)}"
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

    private fun createAvsenderMottaker(xmlSykmelding: XmlSykmelding): AvsenderMottaker {
        val hpr = xmlSykmelding.behandler.ids.find { it.type == PersonIdType.HPR }?.id
        if (hpr != null) {
            return AvsenderMottaker(
                id = hprnummerMedRiktigLengdeOgFormat(hpr),
                idType = "HPRNR",
                navn = xmlSykmelding.behandler.formatName()
            )
        }

        val fnr = xmlSykmelding.behandler.ids.find { it.type == PersonIdType.FNR && validatePersonAndDNumber(it.id) }
        return fnr?.let {
            AvsenderMottaker(
                id = it.id,
                idType = it.type.name,
                navn = xmlSykmelding.behandler.formatName()
            )
        } ?: throw IllegalArgumentException("Neither HPR nor valid FNR found for the given XmlSykmelding")
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

