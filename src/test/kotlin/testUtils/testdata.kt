package testUtils

import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import no.nav.tsm.sykmelding.input.core.model.Aktivitet
import no.nav.tsm.sykmelding.input.core.model.AnnenFravarArsakType
import no.nav.tsm.sykmelding.input.core.model.AnnenFravarsgrunn
import no.nav.tsm.sykmelding.input.core.model.AnnenFraverArsak
import no.nav.tsm.sykmelding.input.core.model.ArbeidsgiverInfo
import no.nav.tsm.sykmelding.input.core.model.ArbeidsrelatertArsak
import no.nav.tsm.sykmelding.input.core.model.ArbeidsrelatertArsakType
import no.nav.tsm.sykmelding.input.core.model.AvsenderSystem
import no.nav.tsm.sykmelding.input.core.model.Behandler
import no.nav.tsm.sykmelding.input.core.model.BistandNav
import no.nav.tsm.sykmelding.input.core.model.DiagnoseInfo
import no.nav.tsm.sykmelding.input.core.model.DiagnoseSystem
import no.nav.tsm.sykmelding.input.core.model.MedisinskArsak
import no.nav.tsm.sykmelding.input.core.model.MedisinskArsakType
import no.nav.tsm.sykmelding.input.core.model.MedisinskVurdering
import no.nav.tsm.sykmelding.input.core.model.Pasient
import no.nav.tsm.sykmelding.input.core.model.Reason
import no.nav.tsm.sykmelding.input.core.model.Rule
import no.nav.tsm.sykmelding.input.core.model.RuleType
import no.nav.tsm.sykmelding.input.core.model.Sporsmalstype
import no.nav.tsm.sykmelding.input.core.model.Sykmelder
import no.nav.tsm.sykmelding.input.core.model.Sykmelding
import no.nav.tsm.sykmelding.input.core.model.SykmeldingMeta
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import no.nav.tsm.sykmelding.input.core.model.Tilbakedatering
import no.nav.tsm.sykmelding.input.core.model.TilbakedatertMerknad
import no.nav.tsm.sykmelding.input.core.model.Tiltak
import no.nav.tsm.sykmelding.input.core.model.UtdypendeSporsmal
import no.nav.tsm.sykmelding.input.core.model.UtenlandskInfo
import no.nav.tsm.sykmelding.input.core.model.ValidationResult
import no.nav.tsm.sykmelding.input.core.model.ValidationType
import no.nav.tsm.sykmelding.input.core.model.Yrkesskade
import no.nav.tsm.sykmelding.input.core.model.metadata.Adresse
import no.nav.tsm.sykmelding.input.core.model.metadata.AdresseType
import no.nav.tsm.sykmelding.input.core.model.metadata.HelsepersonellKategori
import no.nav.tsm.sykmelding.input.core.model.metadata.Kontaktinfo
import no.nav.tsm.sykmelding.input.core.model.metadata.KontaktinfoType
import no.nav.tsm.sykmelding.input.core.model.metadata.Meldingstype
import no.nav.tsm.sykmelding.input.core.model.metadata.MessageInfo
import no.nav.tsm.sykmelding.input.core.model.metadata.MessageMetadata
import no.nav.tsm.sykmelding.input.core.model.metadata.Navn
import no.nav.tsm.sykmelding.input.core.model.metadata.Organisasjon
import no.nav.tsm.sykmelding.input.core.model.metadata.OrganisasjonsType
import no.nav.tsm.sykmelding.input.core.model.metadata.PersonId
import no.nav.tsm.sykmelding.input.core.model.metadata.PersonIdType
import testUtils.TestUtils.Companion.januar

private object shared {
    val sykmeldingId = UUID.randomUUID().toString()

    val fnr: String = "12345678910"

    val pasient =
        Pasient(
            navn = Navn("Tester", null, "Testesen"),
            navKontor = null,
            navnFastlege = "Dr. Test",
            fnr = fnr,
            kontaktinfo = listOf(Kontaktinfo(KontaktinfoType.TLF, "12345678")),
        )

    val sykmeldtFom: LocalDate = 1.januar(2023)
    val sykmeldtTom: LocalDate = 31.januar(2023)
    val hovedDiagnose = DiagnoseInfo(DiagnoseSystem.ICPC2, "A01", "Smerte generell/flere steder")

    var aktivitet: List<Aktivitet> =
        listOf(
            Aktivitet.Avventende("Trenger tilrettelegging", sykmeldtFom, sykmeldtTom),
            Aktivitet.Gradert(60, sykmeldtTom, sykmeldtTom, true),
            Aktivitet.Gradert(60, sykmeldtTom, sykmeldtTom, true),
            Aktivitet.Behandlingsdager(5, sykmeldtFom, sykmeldtTom),
            Aktivitet.Reisetilskudd(sykmeldtFom, sykmeldtTom),
            Aktivitet.IkkeMulig(
                MedisinskArsak(
                    "Grunnet brukket bein kan ikke pasienten gå opp trapper",
                    listOf(MedisinskArsakType.AKTIVITET_FORHINDRER_BEDRING),
                ),
                ArbeidsrelatertArsak(
                    "Grunnet brukket bein kan ikke pasienten gå opp trapper",
                    listOf(ArbeidsrelatertArsakType.MANGLENDE_TILRETTELEGGING),
                ),
                sykmeldtFom,
                sykmeldtTom,
            ),
            Aktivitet.Behandlingsdager(5, sykmeldtFom, sykmeldtTom),
            Aktivitet.Gradert(60, sykmeldtTom, sykmeldtTom, false),
        )

    val arbeidsgiver: ArbeidsgiverInfo =
        ArbeidsgiverInfo.Flere(
            "Standard AS",
            "Utvikler",
            100,
            "melding til arbeidsgiver",
            "word word word word word word word word word word word word word word word word word word\n" +
                "word word word word word word word word word word word word word word word word word word\n" +
                "word word word word word word word word word word word word word word word word word word\n" +
                "word word word word word word word word word word word word word word word word word word\n" +
                "word word word word word word word word word word word word word word word",
        )

    val tiltak: Tiltak =
        Tiltak(
            "word word word word word word word word word word word word word word word word word word\n" +
                "word word word word word word word word word word word word word word",
            "word word word word word word word word word word word word word word word word word word\n" +
                "word word word word word word word word word word word word word word",
        )

    val behandler: Behandler =
        Behandler(
            navn = Navn("Beate", "B.", "Behandler"),
            adresse =
                Adresse(
                    AdresseType.ARBEIDSADRESSE,
                    "Skoleveien 4",
                    "5401",
                    "STORD",
                    "STORD",
                    "STORD",
                    "NORGE",
                ),
            ids = listOf(PersonId("12345678", PersonIdType.HPR)),
            kontaktinfo = listOf(Kontaktinfo(KontaktinfoType.TLF, "1881")),
        )

    val sykmelder =
        Sykmelder(
            ids = listOf(PersonId("9999", PersonIdType.HPR)),
            helsepersonellKategori = HelsepersonellKategori.LEGE,
        )

    val validationResult =
        ValidationResult(
            status = RuleType.INVALID,
            timestamp = OffsetDateTime.now(),
            rules =
                listOf(
                    Rule.Pending(
                        TilbakedatertMerknad.TILBAKEDATERING_UGYLDIG_TILBAKEDATERING.name,
                        OffsetDateTime.now(),
                        ValidationType.MANUAL,
                        Reason(
                            "Den som\n" + "skrev\n" + "sykmeldingen\n" + "mangler\n" + "autorisasjon",
                            "Behandler er manuellterapeut/kiropraktor eller\n" +
                                "fysioterapeut med autorisasjon har angitt annen\n" +
                                "diagnose enn kapitel L (muskel og\n" +
                                "skjelettsykdommer)",
                        ),
                    ),
                    Rule.Invalid(
                        "INVALID",
                        ValidationType.MANUAL,
                        OffsetDateTime.now(),
                        Reason(
                            "Den som\n" + "skrev\n" + "sykmeldingen\n" + "mangler\n" + "autorisasjon",
                            "Behandler er manuellterapeut/kiropraktor eller\n" +
                                "fysioterapeut med autorisasjon har angitt annen\n" +
                                "diagnose enn kapitel L (muskel og\n" +
                                "skjelettsykdommer)",
                        ),
                    ),
                ),
        )

    val organisasjon =
        Organisasjon(
            navn = "Legekontor",
            type = OrganisasjonsType.UGYLDIG,
            ids = emptyList(),
            adresse = null,
            kontaktinfo = null,
            underOrganisasjon = null,
            helsepersonell = null,
        )
}

object legacy {
    val metadata =
        SykmeldingMeta.Legacy(
            mottattDato = OffsetDateTime.now(),
            genDate = OffsetDateTime.now(),
            behandletTidspunkt = OffsetDateTime.now(),
            regelsettVersjon = "3",
            avsenderSystem = AvsenderSystem("NAV", "1.0"),
            strekkode = null,
        )

    val medisinskVUrdering =
        MedisinskVurdering.Legacy(
            hovedDiagnose = shared.hovedDiagnose,
            biDiagnoser =
                listOf(
                    shared.hovedDiagnose,
                    DiagnoseInfo(
                        DiagnoseSystem.ICD10,
                        "A500",
                        "Tidlig medfødt syfilis,\nsymptomgivende",
                    ),
                ),
            svangerskap = true,
            yrkesskade = Yrkesskade(1.januar(2023)),
            skjermetForPasient = true,
            syketilfelletStartDato = shared.sykmeldtFom,
            annenFraversArsak =
                AnnenFraverArsak(
                    "Stor smittefare",
                    listOf(
                        AnnenFravarArsakType.NODVENDIG_KONTROLLUNDENRSOKELSE,
                        AnnenFravarArsakType.MOTTAR_TILSKUDD_GRUNNET_HELSETILSTAND,
                    ),
                ),
        )
}

object utenlandsk {
    val sykmelding =
        Sykmelding.Utenlandsk(
            id = shared.sykmeldingId,
            pasient = shared.pasient,
            medisinskVurdering = legacy.medisinskVUrdering,
            aktivitet = shared.aktivitet,
            metadata = legacy.metadata,
            utenlandskInfo =
                UtenlandskInfo(
                    land = "UTLAND",
                    folkeRegistertAdresseErBrakkeEllerTilsvarende = false,
                    erAdresseUtland = false,
                ),
        )

    val record =
        SykmeldingRecord.Utenlandsk(
            metadata =
                MessageMetadata.Utenlandsk(
                    land = "UTLAND",
                    journalPostId = "123",
                ),
            sykmelding = sykmelding,
            validation = shared.validationResult,
        )
}

object papir {
    val sykmelding =
        Sykmelding.Papir(
            id = shared.sykmeldingId,
            pasient = shared.pasient,
            medisinskVurdering = legacy.medisinskVUrdering,
            aktivitet = shared.aktivitet,
            metadata = legacy.metadata,
            arbeidsgiver = shared.arbeidsgiver,
            tiltak = shared.tiltak,
            behandler = shared.behandler,
            sykmelder = shared.sykmelder,
            prognose = null,
            bistandNav = null,
            tilbakedatering = null,
            utdypendeOpplysninger = null,
        )

    val record =
        SykmeldingRecord.Papir(
            MessageMetadata.Papir(
                msgInfo =
                    MessageInfo(
                        Meldingstype.SYKMELDING,
                        genDate = OffsetDateTime.now(ZoneOffset.UTC),
                        msgId = UUID.randomUUID().toString(),
                        migVersjon = "v1",
                    ),
                sender = shared.organisasjon,
                receiver = shared.organisasjon,
                journalPostId = "123",
            ),
            sykmelding = sykmelding,
            validation = shared.validationResult,
        )
}

object xml {
    val sykmelding =
        Sykmelding.Xml(
            id = shared.sykmeldingId,
            pasient = shared.pasient,
            medisinskVurdering = legacy.medisinskVUrdering,
            aktivitet = shared.aktivitet,
            metadata = legacy.metadata,
            arbeidsgiver = shared.arbeidsgiver,
            tiltak = shared.tiltak,
            behandler = shared.behandler,
            sykmelder = shared.sykmelder,
            prognose = null,
            bistandNav = null,
            tilbakedatering = null,
            utdypendeOpplysninger = null,
        )

    val record =
        SykmeldingRecord.Xml(
            metadata =
                MessageMetadata.Xml.Emottak.Legacy(
                    MessageInfo(
                        Meldingstype.SYKMELDING,
                        genDate = OffsetDateTime.now(ZoneOffset.UTC),
                        msgId = UUID.randomUUID().toString(),
                        migVersjon = "v1",
                    ),
                    sender = shared.organisasjon,
                    receiver = shared.organisasjon,
                    emptyList(),
                ),
            sykmelding = sykmelding,
            validation = shared.validationResult,
        )
}

object digital {
    val sykmelding =
        Sykmelding.Digital(
            id = shared.sykmeldingId,
            metadata =
                SykmeldingMeta.Digital(
                    mottattDato = OffsetDateTime.now(),
                    genDate = OffsetDateTime.now(),
                    avsenderSystem = AvsenderSystem("syk-inn (FHIR)", "1.0"),
                ),
            pasient = shared.pasient,
            medisinskVurdering =
                MedisinskVurdering.Digital(
                    hovedDiagnose = shared.hovedDiagnose,
                    biDiagnoser =
                        listOf(
                            shared.hovedDiagnose,
                            DiagnoseInfo(
                                DiagnoseSystem.ICD10,
                                "A500",
                                "Tidlig medfødt syfilis,\n" + "symptomgivende",
                            ),
                        ),
                    svangerskap = true,
                    yrkesskade = Yrkesskade(1.januar(2023)),
                    skjermetForPasient = true,
                    annenFravarsgrunn = AnnenFravarsgrunn.MOTTAR_TILSKUDD_GRUNNET_HELSETILSTAND,
                ),
            aktivitet = shared.aktivitet,
            behandler = shared.behandler,
            arbeidsgiver = shared.arbeidsgiver,
            sykmelder = shared.sykmelder,
            bistandNav = BistandNav(true, "Bistand nav"),
            tilbakedatering =
                Tilbakedatering(
                    1.januar(2023),
                    begrunnelse =
                        "word word word word word word word word word word word word word word word word word word\n" +
                            "word word word word word word word word word word word word word word word word word word\n" +
                            "word word word word word word word word word word word word word word word word word word\n" +
                            "word word word word word word word word word word",
                ),
            utdypendeSporsmal =
                listOf(
                    UtdypendeSporsmal(
                        type = Sporsmalstype.MEDISINSK_OPPSUMMERING,
                        svar = "svar 6.3.1",
                        sporsmal = null,
                    ),
                    UtdypendeSporsmal(
                        type = Sporsmalstype.UTFORDRINGER_MED_GRADERT_ARBEID,
                        svar = "svar 6.3.2",
                        sporsmal = null,
                    ),
                    UtdypendeSporsmal(
                        type = Sporsmalstype.HENSYN_PA_ARBEIDSPLASSEN,
                        svar = "svar 6.3.3",
                        sporsmal = "hensyn på arbeidsplassen?",
                    ),
                ),
        )

    val record =
        SykmeldingRecord.Digital(
            metadata = MessageMetadata.Digital("123456789"),
            sykmelding = sykmelding,
            validation = shared.validationResult,
        )
}
