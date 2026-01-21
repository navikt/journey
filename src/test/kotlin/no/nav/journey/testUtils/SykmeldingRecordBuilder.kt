package no.nav.journey.testUtils

import no.nav.journey.testUtils.TestUtils.Companion.januar
import no.nav.tsm.sykmelding.input.core.model.*
import no.nav.tsm.sykmelding.input.core.model.Pasient
import no.nav.tsm.sykmelding.input.core.model.metadata.*
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
    var bistandNav = BistandNav(true, "Bistand nav")
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
    var sykmeldingId = UUID.randomUUID().toString()
    var regelsettVersjon = "6"

    var sykmeldingMetadata: SykmeldingMetadata? = null

    var behandler: Behandler = Behandler(
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
        ids = listOf(PersonId("12345678", PersonIdType.HPR)),
        kontaktinfo = listOf(Kontaktinfo(KontaktinfoType.TLF, "1881"))
    )
    var sykmelder = Sykmelder(
        ids = listOf(PersonId("9999", PersonIdType.HPR)),
        helsepersonellKategori = HelsepersonellKategori.LEGE
    )
    var pasient = Pasient(
        navn = Navn("Tester", null, "Testesen"),
        navKontor = null,
        navnFastlege = "Dr. Test",
        fnr = fnr,
        kontaktinfo = listOf(Kontaktinfo(KontaktinfoType.TLF, "12345678"))
    )
    var medisinskVurdering = LegacyMedisinskVurdering(
        hovedDiagnose = hovedDiagnose,
        biDiagnoser = listOf(
            hovedDiagnose, DiagnoseInfo(
                DiagnoseSystem.ICD10, "A500", "Tidlig medfødt syfilis,\n" +
                        "symptomgivende"
            )
        ),
        svangerskap = true,
        yrkesskade = Yrkesskade(1.januar(2023)),
        skjermetForPasient = true,
        syketilfelletStartDato = sykmeldtFom,
        annenFraversArsak = AnnenFraverArsak(
            "Stor smittefare", listOf(
                AnnenFravarArsakType.NODVENDIG_KONTROLLUNDENRSOKELSE,
                AnnenFravarArsakType.MOTTAR_TILSKUDD_GRUNNET_HELSETILSTAND
            )
        )

    )
    val digitalMedisinskVurdering = DigitalMedisinskVurdering(
        hovedDiagnose = hovedDiagnose,
        biDiagnoser = listOf(
            hovedDiagnose, DiagnoseInfo(
                DiagnoseSystem.ICD10, "A500", "Tidlig medfødt syfilis,\n" +
                        "symptomgivende"
            )
        ),
        svangerskap = true,
        yrkesskade = Yrkesskade(1.januar(2023)),
        skjermetForPasient = true,
        annenFravarsgrunn = AnnenFravarsgrunn.MOTTAR_TILSKUDD_GRUNNET_HELSETILSTAND
    )
    var sykmelding: Sykmelding? = null

    fun createSykmeldingMetadata(): SykmeldingMetadata = SykmeldingMetadata(
        mottattDato = OffsetDateTime.now(),
        genDate = OffsetDateTime.now(),
        behandletTidspunkt = OffsetDateTime.now(),
        regelsettVersjon = regelsettVersjon,
        avsenderSystem = AvsenderSystem("NAV", "1.0"),
        strekkode = null
    )

    fun createDigitalSykmeldingMetadata(): DigitalSykmeldingMetadata {
        return DigitalSykmeldingMetadata(
            mottattDato = OffsetDateTime.now(),
            genDate = OffsetDateTime.now(),
            avsenderSystem = AvsenderSystem("syk-inn (FHIR)", "1.0"),
        )
    }

    fun createDigitalSykmelding(): DigitalSykmelding {
        return DigitalSykmelding(
            id = sykmeldingId,
            metadata = createDigitalSykmeldingMetadata(),
            pasient = pasient,
            medisinskVurdering = digitalMedisinskVurdering,
            aktivitet = aktivitet,
            behandler = behandler,
            arbeidsgiver = arbeidsgiver,
            sykmelder = sykmelder,
            bistandNav = bistandNav,
            tilbakedatering = Tilbakedatering(
                1.januar(2023),
                begrunnelse = "word word word word word word word word word word word word word word word word word word\n" +
                        "word word word word word word word word word word word word word word word word word word\n" +
                        "word word word word word word word word word word word word word word word word word word\n" +
                        "word word word word word word word word word word"
            ),
            utdypendeSporsmal = listOf(
                UtdypendeSporsmal(
                    type = Sporsmalstype.MEDISINSK_OPPSUMMERING,
                    svar = "svar 6.3.1",
                    sporsmal = null
                ),
                UtdypendeSporsmal(
                    type = Sporsmalstype.UTFORDRINGER_MED_GRADERT_ARBEID,
                    svar = "svar 6.3.2",
                    sporsmal = null
                ),
                UtdypendeSporsmal(
                    type = Sporsmalstype.HENSYN_PA_ARBEIDSPLASSEN,
                    svar = "svar 6.3.3",
                    sporsmal = null
                ),
            )
        )
    }

    private fun createSykmelding(): XmlSykmelding {

        return XmlSykmelding(
            id = sykmeldingId,
            metadata = sykmeldingMetadata ?: createSykmeldingMetadata(),
            pasient = pasient,
            medisinskVurdering = medisinskVurdering,
            aktivitet = aktivitet,
            behandler = behandler,
            arbeidsgiver = arbeidsgiver,
            sykmelder = sykmelder,
            prognose = prognose,
            tiltak = tiltak,
            bistandNav = bistandNav,
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
        )
    }

    fun build(): SykmeldingRecord {
        return SykmeldingRecord(
            metadata = metadata,
            sykmelding = sykmelding ?: createSykmelding(),
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
