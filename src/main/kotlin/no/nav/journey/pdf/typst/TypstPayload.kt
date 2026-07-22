package no.nav.journey.pdf.typst

import no.nav.tsm.sykmelding.input.core.model.ArbeidsgiverInfo
import no.nav.tsm.sykmelding.input.core.model.AnnenFravarArsakType
import no.nav.tsm.sykmelding.input.core.model.DiagnoseInfo
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
    val diagnose: Diagnose,
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

/** Seksjon 3 – diagnose. */
data class Diagnose(
    val hovedDiagnose: DiagnoseRad?,
    val biDiagnoser: List<DiagnoseRad>,
    // 3.3 – annen fraværsgrunn (normalisert på tvers av Digital/Legacy)
    val annenFravarsgrunn: AnnenFravarsgrunn?,
    // 3.4 / 3.5 / 3.7 – rene ja-utsagn
    val svangerskap: Boolean,
    val yrkesskade: Boolean,
    val skjermetForPasient: Boolean,
    // 3.6 – skadedato (kun når yrkesskade)
    val yrkesskadeDato: String?,
)

data class AnnenFravarsgrunn(
    val arsaker: List<String>,
    val beskrivelse: String?,
)

data class DiagnoseRad(
    val system: String,
    val kode: String,
    val tekst: String?,
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

private fun DiagnoseInfo.toRad(): DiagnoseRad =
    DiagnoseRad(system = system.name, kode = kode, tekst = tekst)

private fun AnnenFravarArsakType.label(): String = when (this) {
    AnnenFravarArsakType.GODKJENT_HELSEINSTITUSJON -> "Godkjent helseinstitusjon"
    AnnenFravarArsakType.BEHANDLING_FORHINDRER_ARBEID -> "Behandling forhindrer arbeid"
    AnnenFravarArsakType.ARBEIDSRETTET_TILTAK -> "Arbeidsrettet tiltak"
    AnnenFravarArsakType.MOTTAR_TILSKUDD_GRUNNET_HELSETILSTAND -> "Mottar tilskudd grunnet helsetilstand"
    AnnenFravarArsakType.NODVENDIG_KONTROLLUNDENRSOKELSE -> "Nødvendig kontrollundersøkelse"
    AnnenFravarArsakType.SMITTEFARE -> "Smittefare"
    AnnenFravarArsakType.ABORT -> "Abort"
    AnnenFravarArsakType.UFOR_GRUNNET_BARNLOSHET -> "Ufør grunnet barnløshet"
    AnnenFravarArsakType.DONOR -> "Donor"
    AnnenFravarArsakType.BEHANDLING_STERILISERING -> "Behandling/sterilisering"
}

fun mapDiagnose(medisinskVurdering: MedisinskVurdering): Diagnose =
    Diagnose(
        hovedDiagnose = medisinskVurdering.hovedDiagnose?.toRad(),
        biDiagnoser = medisinskVurdering.biDiagnoser?.map { it.toRad() } ?: emptyList(),
        annenFravarsgrunn = mapAnnenFravarsgrunn(medisinskVurdering),
        svangerskap = medisinskVurdering.svangerskap,
        yrkesskade = medisinskVurdering.yrkesskade != null,
        skjermetForPasient = medisinskVurdering.skjermetForPasient,
        yrkesskadeDato = medisinskVurdering.yrkesskade?.yrkesskadeDato?.toDate(),
    )

private fun mapAnnenFravarsgrunn(medisinskVurdering: MedisinskVurdering): AnnenFravarsgrunn? =
    when (medisinskVurdering) {
        is MedisinskVurdering.Digital ->
            medisinskVurdering.annenFravarsgrunn?.let {
                AnnenFravarsgrunn(arsaker = listOf(it.label()), beskrivelse = null)
            }

        is MedisinskVurdering.Legacy ->
            medisinskVurdering.annenFraversArsak?.let { arsak ->
                AnnenFravarsgrunn(
                    arsaker = arsak.arsak?.map { it.label() } ?: emptyList(),
                    beskrivelse = arsak.beskrivelse,
                )
            }
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
                diagnose = mapDiagnose(sykmelding.medisinskVurdering),
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
                diagnose = mapDiagnose(sykmelding.medisinskVurdering),
            )
        }

        else -> throw IllegalArgumentException("Kan ikke bygge pdf payload for type ${sykmelding::class.simpleName}")
    }
}
