package no.nav.journey.testUtils

import no.nav.journey.sykmelding.models.*
import no.nav.journey.sykmelding.models.metadata.Adresse
import no.nav.journey.sykmelding.models.metadata.AdresseType
import no.nav.journey.sykmelding.models.metadata.EmottakEnkel
import no.nav.journey.sykmelding.models.metadata.HelsepersonellKategori
import no.nav.journey.sykmelding.models.metadata.Kontaktinfo
import no.nav.journey.sykmelding.models.metadata.KontaktinfoType
import no.nav.journey.sykmelding.models.metadata.Meldingstype
import no.nav.journey.sykmelding.models.metadata.MessageInfo
import no.nav.journey.sykmelding.models.metadata.MessageMetadata
import no.nav.journey.sykmelding.models.metadata.Navn
import no.nav.journey.sykmelding.models.metadata.Organisasjon
import no.nav.journey.sykmelding.models.metadata.OrganisasjonsType
import no.nav.journey.sykmelding.models.metadata.PersonId
import no.nav.journey.sykmelding.models.metadata.PersonIdType
import no.nav.journey.sykmelding.models.validation.InvalidRule
import no.nav.journey.sykmelding.models.validation.PendingRule
import no.nav.journey.sykmelding.models.validation.Reason
import no.nav.journey.sykmelding.models.validation.RuleType
import no.nav.journey.sykmelding.models.validation.TilbakedatertMerknad
import no.nav.journey.sykmelding.models.validation.ValidationResult
import no.nav.journey.sykmelding.models.validation.ValidationType
import no.nav.journey.testUtils.TestUtils.Companion.januar
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*

class SykmeldingRecordBuilder {
    var arbeidsgiver: ArbeidsgiverInfo = FlereArbeidsgivere(
        "Standard AS",
        "Utvikler",
        100,
        "melding til arbeidsgiver",
        "word word word word word word word word word word word word word word word word word word\n" +
                "word word word word word word word word word word word word word word word word word word\n" +
                "word word word word word word word word word word word word word word word word word word\n" +
                "word word word word word word word word word word word word word word word word word word\n" +
                "word word word word word word word word word word word word word word word"
    )
    var fnr: String = "12345678910"
    var diagnose: String = "A01"
    var diagnoseSystem: DiagnoseSystem = DiagnoseSystem.ICPC2
    var hovedDiagnose = DiagnoseInfo(diagnoseSystem, diagnose, "Smerte generell/flere steder")
    var sykmeldtFom: LocalDate = 1.januar(2023)
    var sykmeldtTom: LocalDate = 31.januar(2023)
    var aktivitet: List<Aktivitet> = listOf(
        Avventende("Trenger tilrettelegging", sykmeldtFom, sykmeldtTom),
        Gradert(60, sykmeldtTom, sykmeldtTom, true),
        Gradert(60, sykmeldtTom, sykmeldtTom, true),
        Behandlingsdager(5, sykmeldtFom, sykmeldtTom),
        Reisetilskudd(sykmeldtFom, sykmeldtTom),
        AktivitetIkkeMulig(
            MedisinskArsak(
                "Grunnet brukket bein kan ikke pasienten gå opp trapper",
                listOf(MedisinskArsakType.AKTIVITET_FORHINDRER_BEDRING)
            ),
            ArbeidsrelatertArsak(
                "Grunnet brukket bein kan ikke pasienten gå opp trapper",
                listOf(ArbeidsrelatertArsakType.MANGLENDE_TILRETTELEGGING)
            ), sykmeldtFom, sykmeldtTom
        ),
        Behandlingsdager(5, sykmeldtFom, sykmeldtTom),
        Gradert(60, sykmeldtTom, sykmeldtTom, false)
    )
    var tiltak: Tiltak? = Tiltak(
        "word word word word word word word word word word word word word word word word word word\n" +
                "word word word word word word word word word word word word word word",
        "word word word word word word word word word word word word word word word word word word\n" +
                "word word word word word word word word word word word word word word"
    )
    var prognose: Prognose? = Prognose(
        true,
        "word word word word word word word word word word word word word word word word word word\n" +
                "word word word word word word word word word word word",
        ErIkkeIArbeid(true, 1.januar(2023), sykmeldtFom)
    )
    var type: Meldingstype = Meldingstype.SYKMELDING
    var metadata: MessageMetadata = EmottakEnkel(
        MessageInfo(
            type,
            genDate = OffsetDateTime.now(ZoneOffset.UTC),
            msgId = UUID.randomUUID().toString(),
            migVersjon = "v1"
        ),
        sender = dummyOrganisasjon(),
        receiver = dummyOrganisasjon(),
        emptyList()
    )

    fun build(): SykmeldingRecord {
        return SykmeldingRecord(
            metadata = metadata,
            sykmelding = XmlSykmelding(
                id = UUID.randomUUID().toString(),
                metadata = SykmeldingMetadata(
                    mottattDato = OffsetDateTime.now(),
                    genDate = OffsetDateTime.now(),
                    behandletTidspunkt = OffsetDateTime.now(),
                    regelsettVersjon = "6",
                    avsenderSystem = AvsenderSystem("NAV", "1.0"),
                    strekkode = null
                ),
                pasient = Pasient(
                    navn = Navn("Tester", null, "Testesen"),
                    navKontor = null,
                    navnFastlege = "Dr. Test",
                    fnr = fnr,
                    kontaktinfo = listOf(Kontaktinfo(KontaktinfoType.TLF, "12345678"))
                ),
                medisinskVurdering = MedisinskVurdering(
                    hovedDiagnose = hovedDiagnose,
                    biDiagnoser = listOf(hovedDiagnose, DiagnoseInfo(DiagnoseSystem.ICD10, "A500", "Tidlig medfødt syfilis,\n" +
                            "symptomgivende")),
                    svangerskap = true,
                    yrkesskade = Yrkesskade(1.januar(2023)),
                    skjermetForPasient = false,
                    syketilfelletStartDato = sykmeldtFom,
                    annenFraversArsak = AnnenFraverArsak(
                        "Stor smittefare", listOf(
                            AnnenFravarArsakType.NODVENDIG_KONTROLLUNDENRSOKELSE,
                            AnnenFravarArsakType.MOTTAR_TILSKUDD_GRUNNET_HELSETILSTAND
                        )
                    )
                ),
                aktivitet = aktivitet,
                behandler = Behandler(
                    navn = Navn("Beate", "B.", "Behandler"),
                    adresse = Adresse(
                        AdresseType.ARBEIDSADRESSE,
                        "Skoleveien 4",
                        "5401",
                        "STORD",
                        "STORD",
                        "STORD",
                        "NORGE"
                    ),
                    ids = listOf(PersonId("123456", PersonIdType.HPR)),
                    kontaktinfo = listOf(Kontaktinfo(KontaktinfoType.TLF, "1881"))
                ),
                arbeidsgiver = arbeidsgiver,
                sykmelder = Sykmelder(
                    ids = listOf(PersonId("9999", PersonIdType.HPR)),
                    helsepersonellKategori = HelsepersonellKategori.LEGE
                ),
                prognose = prognose,
                tiltak = tiltak,
                bistandNav = BistandNav(true, "word"),
                tilbakedatering = Tilbakedatering(
                    1.januar(2023),
                    begrunnelse = "word word word word word word word word word word word word word word word word word word\n" +
                            "word word word word word word word word word word word word word word word word word word\n" +
                            "word word word word word word word word word word word word word word word word word word\n" +
                            "word word word word word word word word word word"
                ),
                utdypendeOpplysninger = mapOf(
                    "6.3" to mapOf(
                        "6.3.1" to SporsmalSvar(
                            "Er pasienten veldig syk?",
                            "word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word",
                            listOf(
                                SvarRestriksjon.SKJERMET_FOR_PASIENT
                            )
                        )
                    ),
                    "6.4" to mapOf(
                        "6.4.1" to SporsmalSvar(
                            "Helseopplysninger?",
                            "word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word word",
                            listOf(
                                SvarRestriksjon.SKJERMET_FOR_PASIENT
                            )
                        )
                    )

                )
            ),
            validation = ValidationResult(
                status = RuleType.INVALID,
                timestamp = OffsetDateTime.now(),
                rules = listOf(
                    PendingRule(
                        TilbakedatertMerknad.TILBAKEDATERING_UGYLDIG_TILBAKEDATERING.name,
                        OffsetDateTime.now(),
                        ValidationType.MANUAL,
                        Reason(
                            "Den som\n" +
                                    "skrev\n" +
                                    "sykmeldingen\n" +
                                    "mangler\n" +
                                    "autorisasjon", "Behandler er manuellterapeut/kiropraktor eller\n" +
                                    "fysioterapeut med autorisasjon har angitt annen\n" +
                                    "diagnose enn kapitel L (muskel og\n" +
                                    "skjelettsykdommer)"
                        )
                    ), InvalidRule(
                        "INVALID",
                        ValidationType.MANUAL, OffsetDateTime.now(), Reason(
                            "Den som\n" +
                                    "skrev\n" +
                                    "sykmeldingen\n" +
                                    "mangler\n" +
                                    "autorisasjon", "Behandler er manuellterapeut/kiropraktor eller\n" +
                                    "fysioterapeut med autorisasjon har angitt annen\n" +
                                    "diagnose enn kapitel L (muskel og\n" +
                                    "skjelettsykdommer)"
                        )
                    )
                )
            )
        )
    }

}
fun sykmeldingRecord(init: SykmeldingRecordBuilder.() -> Unit): SykmeldingRecord {
    return SykmeldingRecordBuilder().apply(init).build()
}


fun dummyOrganisasjon() = Organisasjon(
    navn = "Legekontor",
    type = OrganisasjonsType.UGYLDIG,
    ids = emptyList(),
    adresse = null,
    kontaktinfo = null,
    underOrganisasjon = null,
    helsepersonell = null
)