package no.nav.journey.pdf

import no.nav.tsm.sykmelding.input.core.model.Aktivitet
import no.nav.tsm.sykmelding.input.core.model.ArbeidsgiverInfo
import no.nav.tsm.sykmelding.input.core.model.ArbeidsrelatertArsakType
import no.nav.tsm.sykmelding.input.core.model.AnnenFravarArsakType
import no.nav.tsm.sykmelding.input.core.model.AvsenderSystem
import no.nav.tsm.sykmelding.input.core.model.Behandler
import no.nav.tsm.sykmelding.input.core.model.BistandNav
import no.nav.tsm.sykmelding.input.core.model.DiagnoseInfo
import no.nav.tsm.sykmelding.input.core.model.IArbeid
import no.nav.tsm.sykmelding.input.core.model.MedisinskArsakType
import no.nav.tsm.sykmelding.input.core.model.MedisinskVurdering
import no.nav.tsm.sykmelding.input.core.model.Pasient
import no.nav.tsm.sykmelding.input.core.model.Rule
import no.nav.tsm.sykmelding.input.core.model.RuleType
import no.nav.tsm.sykmelding.input.core.model.SporsmalSvar
import no.nav.tsm.sykmelding.input.core.model.Sporsmalstype
import no.nav.tsm.sykmelding.input.core.model.Sykmelder
import no.nav.tsm.sykmelding.input.core.model.Sykmelding
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import no.nav.tsm.sykmelding.input.core.model.Tiltak
import no.nav.tsm.sykmelding.input.core.model.UtdypendeSporsmal
import no.nav.tsm.sykmelding.input.core.model.ValidationResult
import no.nav.tsm.sykmelding.input.core.model.metadata.KontaktinfoType
import no.nav.tsm.sykmelding.input.core.model.metadata.MessageMetadata
import no.nav.tsm.sykmelding.input.core.model.metadata.MetadataType
import no.nav.tsm.sykmelding.input.core.model.metadata.PersonId
import no.nav.tsm.sykmelding.input.core.model.metadata.PersonIdType
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
    val aktivitet: List<AktivitetGruppe>,
    val arbeidsevne: Arbeidsevne,
    val meldingTilNav: MeldingTilNav?,
    val meldingTilArbeidsgiver: MeldingTilArbeidsgiver?,
    val tilbakedatering: Tilbakedatering?,
    val prognose: Prognose?,
    val utdypende: List<UtdypendeGruppe>,
    val bekreftelse: Bekreftelse,
    val avvisning: List<AvvisningRad>,
    val avslatt: Boolean,
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

/** Seksjon 4 – mulighet for arbeid. Aktiviteter gruppert per type (4.1–4.5). */
data class AktivitetGruppe(
    val type: String,
    val rader: List<AktivitetRad>,
)

/** Én periode. Kun feltene som er relevante for typen er utfylt. */
data class AktivitetRad(
    val fom: String,
    val tom: String,
    // GRADERT
    val grad: Int? = null,
    val reisetilskudd: Boolean? = null,
    // AVVENTENDE
    val innspillTilArbeidsgiver: String? = null,
    // BEHANDLINGSDAGER
    val antallBehandlingsdager: Int? = null,
    // AKTIVITET_IKKE_MULIG
    val medisinskArsak: AktivitetArsak? = null,
    val arbeidsrelatertArsak: AktivitetArsak? = null,
)

data class AktivitetArsak(
    val arsaker: List<String>,
    val beskrivelse: String?,
)

/** Seksjon 7 – hva skal til for å bedre arbeidsevnen. */
data class Arbeidsevne(
    val tiltakArbeidsplassen: String?, // 7.1
    val tiltakNav: String?,            // 7.2
    val andreTiltak: String?,          // 7.3
) {
    val harInnhold: Boolean
        get() = tiltakArbeidsplassen != null || tiltakNav != null || andreTiltak != null
}

/** Seksjon 8 – melding til NAV. */
data class MeldingTilNav(
    val bistandUmiddelbart: Boolean,
    val beskrivBistand: String?,
    val regelsettV3: Boolean,
)

/** Seksjon 9 – melding til arbeidsgiver. */
data class MeldingTilArbeidsgiver(
    val tekst: String,
    val regelsettV3: Boolean,
)

/** Seksjon 11 – tilbakedatering. */
data class Tilbakedatering(
    val kontaktDato: String?,
    val begrunnelse: String?,
)

/** Seksjon 12 – bekreftelse. */
data class Bekreftelse(
    val egenmeldt: Boolean,        // styrer 12.1-etikett
    val bekreftelsesdato: String,  // 12.1
    val sykmeldersNavn: String,    // 12.2
    val hprNummer: String?,        // 12.4
    val telefon: String?,          // 12.5
    val adresse: String?,          // 12.6
    val organisasjonsnavn: String?,
    val avsenderSystemNavn: String,
    val avsenderSystemVersjon: String,
    val signerendeHprNummer: String?,
)

/** Seksjon 13 – begrunnelse for avvisning (én rad per INVALID-regel). */
data class AvvisningRad(
    val sykmeldt: String,
    val sykmelder: String,
)

/** Seksjon 5 – friskmelding/prognose. */
data class Prognose(
    val arbeidsforEtterPeriode: Boolean, // 5.1
    val hensynArbeidsplassen: String?,   // 5.1.1
    val arbeid: PrognoseArbeid?,         // 5.2/5.3 (kun ikke-v3)
)

data class PrognoseArbeid(
    val type: String, // ER_I_ARBEID / ER_IKKE_I_ARBEID
    // ER_I_ARBEID
    val egetArbeidPaSikt: Boolean? = null,
    val annetArbeidPaSikt: Boolean? = null,
    val arbeidFOM: String? = null,
    // ER_IKKE_I_ARBEID
    val arbeidsforPaSikt: Boolean? = null,
    val arbeidsforFOM: String? = null,
    // felles
    val vurderingsdato: String? = null,
)

/** Seksjon 6 – utdypende opplysninger, normalisert på tvers av Legacy/Digital. */
data class UtdypendeGruppe(
    val tittel: String,
    val sporsmal: List<UtdypendeSporsmalRad>,
)

data class UtdypendeSporsmalRad(
    val sporsmal: String?,
    val svar: String,
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

private fun MedisinskArsakType.label(): String = when (this) {
    MedisinskArsakType.TILSTAND_HINDRER_AKTIVITET -> "Tilstanden hindrer aktivitet"
    MedisinskArsakType.AKTIVITET_FORVERRER_TILSTAND -> "Aktivitet forverrer tilstanden"
    MedisinskArsakType.AKTIVITET_FORHINDRER_BEDRING -> "Aktivitet forhindrer bedring"
    MedisinskArsakType.ANNET -> "Annet"
}

private fun ArbeidsrelatertArsakType.label(): String = when (this) {
    ArbeidsrelatertArsakType.MANGLENDE_TILRETTELEGGING -> "Tilrettelegging ikke mulig"
    ArbeidsrelatertArsakType.ANNET -> "Annet"
}

private fun Aktivitet.toRad(): AktivitetRad = when (this) {
    is Aktivitet.Avventende -> AktivitetRad(
        fom = fom.toDate(),
        tom = tom.toDate(),
        innspillTilArbeidsgiver = innspillTilArbeidsgiver,
    )

    is Aktivitet.Gradert -> AktivitetRad(
        fom = fom.toDate(),
        tom = tom.toDate(),
        grad = grad,
        reisetilskudd = reisetilskudd,
    )

    is Aktivitet.IkkeMulig -> AktivitetRad(
        fom = fom.toDate(),
        tom = tom.toDate(),
        medisinskArsak = medisinskArsak?.let {
            AktivitetArsak(arsaker = it.arsak.map(MedisinskArsakType::label), beskrivelse = it.beskrivelse)
        },
        arbeidsrelatertArsak = arbeidsrelatertArsak?.let {
            AktivitetArsak(arsaker = it.arsak.map(ArbeidsrelatertArsakType::label), beskrivelse = it.beskrivelse)
        },
    )

    is Aktivitet.Behandlingsdager -> AktivitetRad(
        fom = fom.toDate(),
        tom = tom.toDate(),
        antallBehandlingsdager = antallBehandlingsdager,
    )

    is Aktivitet.Reisetilskudd -> AktivitetRad(
        fom = fom.toDate(),
        tom = tom.toDate(),
    )
}

// Fast rekkefølge 4.1–4.5 i utskriften.
private val AKTIVITET_REKKEFOLGE = listOf(
    "AVVENTENDE", "GRADERT", "AKTIVITET_IKKE_MULIG", "BEHANDLINGSDAGER", "REISETILSKUDD",
)

fun mapAktivitet(aktiviteter: List<Aktivitet>): List<AktivitetGruppe> =
    aktiviteter
        .groupBy { it.type.name }
        .map { (type, rader) -> AktivitetGruppe(type = type, rader = rader.map { it.toRad() }) }
        .sortedBy { AKTIVITET_REKKEFOLGE.indexOf(it.type) }

private val ArbeidsgiverInfo.tiltakArbeidsplassen: String?
    get() = (this as? ArbeidsgiverInfo.En)?.tiltakArbeidsplassen
        ?: (this as? ArbeidsgiverInfo.Flere)?.tiltakArbeidsplassen

private val ArbeidsgiverInfo.meldingTilArbeidsgiver: String?
    get() = (this as? ArbeidsgiverInfo.En)?.meldingTilArbeidsgiver
        ?: (this as? ArbeidsgiverInfo.Flere)?.meldingTilArbeidsgiver

fun mapArbeidsevne(arbeidsgiver: ArbeidsgiverInfo, tiltak: Tiltak?): Arbeidsevne =
    Arbeidsevne(
        tiltakArbeidsplassen = arbeidsgiver.tiltakArbeidsplassen,
        tiltakNav = tiltak?.tiltakNav,
        andreTiltak = tiltak?.andreTiltak,
    )

fun mapMeldingTilNav(bistandNav: BistandNav?, regelsettVersjon: String?): MeldingTilNav? =
    bistandNav?.takeIf { it.beskrivBistand != null }?.let {
        MeldingTilNav(
            bistandUmiddelbart = it.bistandUmiddelbart,
            beskrivBistand = it.beskrivBistand,
            regelsettV3 = regelsettVersjon == "3",
        )
    }

fun mapMeldingTilArbeidsgiver(arbeidsgiver: ArbeidsgiverInfo, regelsettVersjon: String?): MeldingTilArbeidsgiver? =
    arbeidsgiver.meldingTilArbeidsgiver?.let {
        MeldingTilArbeidsgiver(tekst = it, regelsettV3 = regelsettVersjon == "3")
    }

fun mapTilbakedatering(
    tilbakedatering: no.nav.tsm.sykmelding.input.core.model.Tilbakedatering?,
    metadataType: MetadataType,
): Tilbakedatering? {
    if (metadataType == MetadataType.EGENMELDT) return null
    val kontaktDato = tilbakedatering?.kontaktDato
    val begrunnelse = tilbakedatering?.begrunnelse
    if (kontaktDato == null && begrunnelse == null) return null
    return Tilbakedatering(
        kontaktDato = kontaktDato?.toDate(),
        begrunnelse = begrunnelse,
    )
}

private fun List<PersonId>.hpr(): String? =
    firstOrNull { it.type == PersonIdType.HPR }?.id

private fun fulltNavn(fornavn: String, mellomnavn: String?, etternavn: String): String =
    listOfNotNull(fornavn, mellomnavn, etternavn).joinToString(" ")

private fun MessageMetadata.senderNavn(): String? = when (this) {
    is MessageMetadata.Papir -> sender.navn
    is MessageMetadata.Xml.Emottak -> sender.navn
    else -> null
}

fun mapBekreftelse(
    behandler: Behandler,
    sykmelder: Sykmelder,
    avsenderSystem: AvsenderSystem,
    bekreftelsesdato: String,
    organisasjonsnavn: String?,
    metadataType: MetadataType,
): Bekreftelse =
    Bekreftelse(
        egenmeldt = metadataType == MetadataType.EGENMELDT,
        bekreftelsesdato = bekreftelsesdato,
        sykmeldersNavn = fulltNavn(behandler.navn.fornavn, behandler.navn.mellomnavn, behandler.navn.etternavn),
        hprNummer = behandler.ids.hpr(),
        telefon = behandler.kontaktinfo.firstOrNull { it.type == KontaktinfoType.TLF }?.value,
        adresse = behandler.adresse?.let {
            val gate = it.gateadresse?.let { g -> "$g," }
            listOfNotNull(gate, it.postnummer, it.kommune).joinToString(" ").ifBlank { null }
        },
        organisasjonsnavn = organisasjonsnavn,
        avsenderSystemNavn = avsenderSystem.navn,
        avsenderSystemVersjon = avsenderSystem.versjon,
        signerendeHprNummer = sykmelder.ids.hpr(),
    )

fun mapAvvisning(validation: ValidationResult): List<AvvisningRad> {
    if (validation.status != RuleType.INVALID) return emptyList()
    return validation.rules
        .filterIsInstance<Rule.Invalid>()
        .map { AvvisningRad(sykmeldt = it.reason.sykmeldt, sykmelder = it.reason.sykmelder) }
}

fun mapPrognose(prognose: no.nav.tsm.sykmelding.input.core.model.Prognose?, regelsettVersjon: String?): Prognose? {
    if (prognose == null) return null
    // Arbeid-blokken (5.2/5.3) vises ikke for regelsett v3.
    val arbeid = prognose.arbeid?.takeIf { regelsettVersjon != "3" }?.let { a ->
        when (a) {
            is IArbeid.ErIArbeid -> PrognoseArbeid(
                type = a.type.name,
                egetArbeidPaSikt = a.egetArbeidPaSikt,
                annetArbeidPaSikt = a.annetArbeidPaSikt,
                arbeidFOM = a.arbeidFOM?.toDate(),
                vurderingsdato = a.vurderingsdato?.toDate(),
            )

            is IArbeid.ErIkkeIArbeid -> PrognoseArbeid(
                type = a.type.name,
                arbeidsforPaSikt = a.arbeidsforPaSikt,
                arbeidsforFOM = a.arbeidsforFOM?.toDate(),
                vurderingsdato = a.vurderingsdato?.toDate(),
            )
        }
    }
    return Prognose(
        arbeidsforEtterPeriode = prognose.arbeidsforEtterPeriode,
        hensynArbeidsplassen = prognose.hensynArbeidsplassen,
        arbeid = arbeid,
    )
}

private fun utdypendeTittel(nokkel: String): String = when (nokkel) {
    "6.1" -> "Utdypende opplysninger ved 4, 12 og 28 uker ved visse diagnoser"
    "6.2" -> "Utdypende opplysninger ved 8, 17 og 39 uker"
    "6.3" -> "Opplysninger ved vurdering av aktivitetskravet"
    "6.4" -> "Helseopplysninger ved 17 uker"
    "6.5" -> "Utdypende opplysninger ved 39 uker"
    "6.6" -> "Helseopplysninger dersom pasienten søker om AAP"
    else -> nokkel
}

/** Legacy: nested map keyed by "6.x". */
fun mapUtdypendeLegacy(utdypende: Map<String, Map<String, SporsmalSvar>>?): List<UtdypendeGruppe> =
    utdypende.orEmpty().map { (nokkel, sporsmal) ->
        UtdypendeGruppe(
            tittel = utdypendeTittel(nokkel),
            sporsmal = sporsmal.values.map { UtdypendeSporsmalRad(sporsmal = it.sporsmal, svar = it.svar) },
        )
    }

// Seksjonsprefikser for digitale utdypende spørsmål.
private const val UKE7_PREFIX = "6.3"
private const val UKE17_PREFIX = "6.4"
private const val UKE39_PREFIX = "6.5"

// Fast spørsmålstekst + delnøkkel per spørsmålstype (brukes når spm.sporsmal mangler).
private fun spmMapping(prefix: String): Map<Sporsmalstype, Pair<String, String>> = mapOf(
    Sporsmalstype.MEDISINSK_OPPSUMMERING to
        ("$prefix.1" to "Gi en kort medisinsk oppsummering av tilstanden (sykehistorie, hovedsymptomer, behandling)"),
    Sporsmalstype.UTFORDRINGER_MED_ARBEID to
        ("$prefix.2" to "Beskriv kort hvilke utfordringer helsetilstanden gir i arbeidssituasjonen nå. Oppgi også kort hva pasienten likevel kan mestre"),
    Sporsmalstype.UTFORDRINGER_MED_GRADERT_ARBEID to
        ("$UKE7_PREFIX.2" to "Beskriv kort hvilke helsemessige begrensninger som gjør det vanskelig å jobbe gradert"),
    Sporsmalstype.HENSYN_PA_ARBEIDSPLASSEN to
        ("$UKE7_PREFIX.3" to "Beskriv eventuelle medisinske forhold som bør ivaretas ved eventuell tilbakeføring til nåværende arbeid (ikke obligatorisk)"),
    Sporsmalstype.BEHANDLING_OG_FREMTIDIG_ARBEID to
        ("$UKE17_PREFIX.3" to "Beskriv pågående og planlagt utredning/behandling, og om dette forventes å påvirke muligheten for økt arbeidsdeltakelse fremover"),
    Sporsmalstype.UAVKLARTE_FORHOLD to
        ("$UKE17_PREFIX.4" to "Er det forhold som fortsatt er uavklarte eller hindrer videre arbeidsdeltakelse, som Nav bør være kjent med i sin oppfølging?"),
    Sporsmalstype.FORVENTET_HELSETILSTAND_UTVIKLING to
        ("$UKE39_PREFIX.3" to "Hvordan forventes helsetilstanden å utvikle seg de neste 3-6 månedene med tanke på mulighet for økt arbeidsdeltakelse?"),
    Sporsmalstype.MEDISINSKE_HENSYN to
        ("$UKE39_PREFIX.4" to "Er det medisinske hensyn eller avklaringsbehov Nav bør kjenne til i videre oppfølging?"),
)

/**
 * Digital: flat liste normaliseres til samme grupperte struktur som Legacy.
 * Delnøkkel og standard spørsmålstekst utledes per spørsmålstype, deretter
 * grupperes det på seksjon (6.3/6.4/6.5).
 */
fun mapUtdypendeDigital(sporsmal: List<UtdypendeSporsmal>?): List<UtdypendeGruppe> {
    if (sporsmal.isNullOrEmpty()) return emptyList()

    val prefix = when {
        sporsmal.any { it.type == Sporsmalstype.MEDISINSKE_HENSYN } -> UKE39_PREFIX
        sporsmal.any { it.type == Sporsmalstype.BEHANDLING_OG_FREMTIDIG_ARBEID } -> UKE17_PREFIX
        sporsmal.any { it.type == Sporsmalstype.UTFORDRINGER_MED_GRADERT_ARBEID } -> UKE7_PREFIX
        else -> throw IllegalArgumentException("Utdypende sporsmal mangler gyldig prefiks: ${sporsmal.first().type}")
    }

    val mappings = spmMapping(prefix)
    // key -> (spørsmålstekst, svar)
    val rader = sporsmal.mapNotNull { spm ->
        mappings[spm.type]?.let { (key, standardSporsmal) ->
            key to UtdypendeSporsmalRad(sporsmal = spm.sporsmal ?: standardSporsmal, svar = spm.svar)
        }
    }

    return rader
        .groupBy { (key, _) ->
            when {
                key.startsWith(UKE39_PREFIX) -> UKE39_PREFIX
                key.startsWith(UKE17_PREFIX) -> UKE17_PREFIX
                key.startsWith(UKE7_PREFIX) -> UKE7_PREFIX
                else -> throw IllegalArgumentException("Sporsmal mangler gyldig prefiks: $key")
            }
        }
        .map { (seksjon, par) ->
            UtdypendeGruppe(tittel = utdypendeTittel(seksjon), sporsmal = par.map { it.second })
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
                aktivitet = mapAktivitet(sykmelding.aktivitet),
                arbeidsevne = mapArbeidsevne(sykmelding.arbeidsgiver, sykmelding.tiltak),
                meldingTilNav = mapMeldingTilNav(sykmelding.bistandNav, sykmelding.metadata.regelsettVersjon),
                meldingTilArbeidsgiver = mapMeldingTilArbeidsgiver(
                    sykmelding.arbeidsgiver,
                    sykmelding.metadata.regelsettVersjon
                ),
                tilbakedatering = mapTilbakedatering(sykmelding.tilbakedatering, sykmeldingRecord.metadata.type),
                prognose = mapPrognose(sykmelding.prognose, sykmelding.metadata.regelsettVersjon),
                utdypende = if (sykmeldingRecord.metadata.type == MetadataType.EGENMELDT) emptyList()
                else mapUtdypendeLegacy(sykmelding.utdypendeOpplysninger),
                bekreftelse = mapBekreftelse(
                    behandler = sykmelding.behandler,
                    sykmelder = sykmelding.sykmelder,
                    avsenderSystem = sykmelding.metadata.avsenderSystem,
                    bekreftelsesdato = sykmelding.metadata.behandletTidspunkt.toDate(),
                    organisasjonsnavn = sykmeldingRecord.metadata.senderNavn(),
                    metadataType = sykmeldingRecord.metadata.type,
                ),
                avvisning = mapAvvisning(sykmeldingRecord.validation),
                avslatt = mapHeaderStatus(sykmeldingRecord.validation).avslatt,
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
                aktivitet = mapAktivitet(sykmelding.aktivitet),
                arbeidsevne = mapArbeidsevne(sykmelding.arbeidsgiver, tiltak = null),
                meldingTilNav = mapMeldingTilNav(sykmelding.bistandNav, regelsettVersjon = null),
                meldingTilArbeidsgiver = mapMeldingTilArbeidsgiver(sykmelding.arbeidsgiver, regelsettVersjon = null),
                tilbakedatering = mapTilbakedatering(sykmelding.tilbakedatering, sykmeldingRecord.metadata.type),
                prognose = null, // Digital har ingen prognose
                utdypende = if (sykmeldingRecord.metadata.type == MetadataType.EGENMELDT) emptyList()
                else mapUtdypendeDigital(sykmelding.utdypendeSporsmal),
                bekreftelse = mapBekreftelse(
                    behandler = sykmelding.behandler,
                    sykmelder = sykmelding.sykmelder,
                    avsenderSystem = sykmelding.metadata.avsenderSystem,
                    // Digital har ingen behandletTidspunkt – faller tilbake til mottattDato.
                    bekreftelsesdato = sykmelding.metadata.mottattDato.toDate(),
                    organisasjonsnavn = sykmeldingRecord.metadata.senderNavn(),
                    metadataType = sykmeldingRecord.metadata.type,
                ),
                avvisning = mapAvvisning(sykmeldingRecord.validation),
                avslatt = mapHeaderStatus(sykmeldingRecord.validation).avslatt,
            )
        }

        else -> throw IllegalArgumentException("Kan ikke bygge pdf payload for type ${sykmelding::class.simpleName}")
    }
}
