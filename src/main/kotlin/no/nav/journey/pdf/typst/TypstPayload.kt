package no.nav.journey.pdf.typst

import no.nav.tsm.sykmelding.input.core.model.ArbeidsgiverInfo
import no.nav.tsm.sykmelding.input.core.model.MedisinskVurdering
import no.nav.tsm.sykmelding.input.core.model.Pasient
import no.nav.tsm.sykmelding.input.core.model.RuleType
import no.nav.tsm.sykmelding.input.core.model.Sykmelding
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import no.nav.tsm.sykmelding.input.core.model.ValidationResult
import no.nav.tsm.sykmelding.input.core.model.metadata.KontaktinfoType
import no.nav.tsm.sykmelding.input.core.model.metadata.MetadataType
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

data class TypstPayload(
    val sykmeldingId: String,
    val headerPasient: HeaderPasient,
    val headerStatus: HeaderStatus,
    val headerDates: HeaderDates,
    val syketilfelle: Syketilfelle,
    val pasientopplysninger: Pasientopplysninger,
    val arbeidsgiver: Arbeidsgiver,
)

/** Ferdigformatert pasientinfo til headeren. */
data class HeaderPasient(
    val navn: String,
    val fnr: String,
)

/** Banner-flags som vises øverst i sykmeldingen, utledet fra valideringsresultatet. */
data class HeaderStatus(
    val avslatt: Boolean,
    val avvist: Boolean,
)

/** Ferdigformaterte datoer til headeren. Typst-malen gjør ingen dato-håndtering. */
data class HeaderDates(
    val genereringsdato: String,
    val mottattDato: String,
    val behandletDato: String,
)

/** Seksjon 0 – syketilfelle startdato. Kun data/flagg, ingen tekst. */
data class Syketilfelle(
    val egenmeldt: Boolean,
    val startdato: String?,
)

/** Seksjon 1 – pasientopplysninger. Kun data/flagg. */
data class Pasientopplysninger(
    val etternavn: String,
    val fornavn: String,
    val fnr: String,
    val telefon: String?,
    val navnFastlege: String?,
    // 1.3/1.4 vises kun for eldre regelsett (ikke v3, ikke digital).
    val visKontaktOgFastlege: Boolean,
)

/** Seksjon 2 – arbeidsgiver. Kun data/flagg. */
data class Arbeidsgiver(
    val type: String,
    val egenmeldt: Boolean,
    val navn: String?,
    val yrkesbetegnelse: String?,
    val stillingsprosent: Int?,
)

private val DATETIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")
private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

private fun OffsetDateTime.toDateTime(): String = format(DATETIME_FORMAT)
private fun OffsetDateTime.toDate(): String = format(DATE_FORMAT)
private fun LocalDate.toDate(): String = format(DATE_FORMAT)

private const val UGYLDIG_TILBAKEDATERING = "TILBAKEDATERING_UGYLDIG_TILBAKEDATERING"

fun mapHeaderStatus(validation: ValidationResult): HeaderStatus =
    HeaderStatus(
        avslatt = validation.rules.any { it.name == UGYLDIG_TILBAKEDATERING },
        avvist = validation.status == RuleType.INVALID,
    )

fun mapHeaderPasient(pasient: Pasient): HeaderPasient =
    HeaderPasient(
        navn = pasient.navn?.let {
            listOfNotNull(it.fornavn, it.mellomnavn, it.etternavn).joinToString(" ")
        } ?: "",
        fnr = pasient.fnr,
    )

fun mapSyketilfelle(medisinskVurdering: MedisinskVurdering, metadataType: MetadataType): Syketilfelle =
    Syketilfelle(
        egenmeldt = metadataType == MetadataType.EGENMELDT,
        startdato = (medisinskVurdering as? MedisinskVurdering.Legacy)?.syketilfelletStartDato?.toDate(),
    )

fun mapPasientopplysninger(pasient: Pasient, regelsettVersjon: String?): Pasientopplysninger =
    Pasientopplysninger(
        etternavn = pasient.navn?.etternavn ?: "",
        fornavn = pasient.navn?.fornavn ?: "",
        fnr = pasient.fnr,
        telefon = pasient.kontaktinfo.firstOrNull { it.type == KontaktinfoType.TLF }?.value,
        navnFastlege = pasient.navnFastlege,
        // Digital sender ikke regelsettVersjon (null) → skjules. Legacy viser når ikke v3.
        visKontaktOgFastlege = regelsettVersjon != null && regelsettVersjon != "3",
    )

fun mapArbeidsgiver(arbeidsgiver: ArbeidsgiverInfo, metadataType: MetadataType): Arbeidsgiver {
    val navn = (arbeidsgiver as? ArbeidsgiverInfo.En)?.navn
        ?: (arbeidsgiver as? ArbeidsgiverInfo.Flere)?.navn
    val yrkesbetegnelse = (arbeidsgiver as? ArbeidsgiverInfo.En)?.yrkesbetegnelse
        ?: (arbeidsgiver as? ArbeidsgiverInfo.Flere)?.yrkesbetegnelse
    val stillingsprosent = (arbeidsgiver as? ArbeidsgiverInfo.En)?.stillingsprosent
        ?: (arbeidsgiver as? ArbeidsgiverInfo.Flere)?.stillingsprosent
    return Arbeidsgiver(
        type = arbeidsgiver.type.name,
        egenmeldt = metadataType == MetadataType.EGENMELDT,
        navn = navn,
        yrkesbetegnelse = yrkesbetegnelse,
        stillingsprosent = stillingsprosent,
    )
}


fun buildTypstPayload(sykmeldingRecord: SykmeldingRecord): TypstPayload {
    return when (val sykmelding = sykmeldingRecord.sykmelding) {
        is Sykmelding.Xml -> {
            TypstPayload(
                sykmeldingId = sykmelding.id,
                headerPasient = mapHeaderPasient(sykmelding.pasient),
                headerStatus = mapHeaderStatus(sykmeldingRecord.validation),
                headerDates = HeaderDates(
                    genereringsdato = sykmelding.metadata.genDate.toDateTime(),
                    mottattDato = sykmelding.metadata.mottattDato.toDateTime(),
                    behandletDato = sykmelding.metadata.behandletTidspunkt.toDate(),
                ),
                syketilfelle = mapSyketilfelle(sykmelding.medisinskVurdering, sykmeldingRecord.metadata.type),
                pasientopplysninger = mapPasientopplysninger(sykmelding.pasient, sykmelding.metadata.regelsettVersjon),
                arbeidsgiver = mapArbeidsgiver(sykmelding.arbeidsgiver, sykmeldingRecord.metadata.type),
            )
        }

        is Sykmelding.Digital -> {
            TypstPayload(
                sykmeldingId = sykmelding.id,
                headerPasient = mapHeaderPasient(sykmelding.pasient),
                headerStatus = mapHeaderStatus(sykmeldingRecord.validation),
                headerDates = HeaderDates(
                    genereringsdato = sykmelding.metadata.genDate.toDateTime(),
                    mottattDato = sykmelding.metadata.mottattDato.toDateTime(),
                    // Digital har ingen behandletTidspunkt – faller tilbake til mottattDato.
                    behandletDato = sykmelding.metadata.mottattDato.toDate(),
                ),
                syketilfelle = mapSyketilfelle(sykmelding.medisinskVurdering, sykmeldingRecord.metadata.type),
                pasientopplysninger = mapPasientopplysninger(sykmelding.pasient, regelsettVersjon = null),
                arbeidsgiver = mapArbeidsgiver(sykmelding.arbeidsgiver, sykmeldingRecord.metadata.type),
            )
        }

        else -> throw IllegalArgumentException("Kan ikke bygge pdf payload for type ${sykmelding::class.simpleName}")
    }
}
