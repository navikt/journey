#let data = json(sys.inputs.at("data-path"))
#let status = data.headerStatus
#let pasient = data.headerPasient
#let dates = data.headerDates

#set text(font: ("Source Sans 3", "Noto Color Emoji", "DejaVu Sans"), lang: "nb", size: 10pt)
#set table(stroke: 1pt + black, inset: 4pt)

#let dokumentHeader = context [
    #let page-num = counter(page).get().first()
    #let total = counter(page).final().first()

    #grid(
        columns: (auto, 1fr, auto),
        align: (left + horizon, left + horizon, right + horizon),
        column-gutter: 1em,
        pdf.artifact(image("resources/logo-red.png", height: 1.5cm)),
        [
            #text(size: 15pt, weight: "bold")[Sykmelding for #pasient.navn] \
            Fødselsnummer: #pasient.fnr \
            #if status.avslatt [
                *AVSLÅTT* på grunn av ugyldig tilbakedatering
            ] else if status.avvist [
                *AVVIST* av Nav
            ] else [ OK ]
        ],
        text(size: 9pt, fill: luma(90))[
            Genereringsdato: #dates.genereringsdato \
            Mottatt dato NAV: #dates.mottattDato \
            Behandlet dato: #dates.behandletDato \
            Side #page-num av #total
        ],
    )
    #v(0.3em)
    #line(length: 100%, stroke: 0.5pt + luma(90))
]

#let documentFooter = context [
    #let page-num = counter(page).get().first()
    #let total = counter(page).final().first()
    #grid(
        columns: (1fr, auto),
        text(fill: gray, size: 8pt)[Teknisk sporingsinformasjon: #data.sykmeldingId],
        text(size: 8pt)[Side #page-num av #total],
    )
]

#set page(
    margin: (top: 4cm, rest: 1cm),
    header: dokumentHeader,
    footer: documentFooter,
)

// Seksjonstabell: svart ytre ramme, hvit indre, blå header, full bredde.
#let seksjonstabell(columns, ..cells) = block(width: 100%, stroke: black, inset: 1pt, table(
    columns: columns,
    stroke: white,
    fill: (_, y) => if y == 0 { blue.lighten(60%) },
    ..cells,
))

// Seksjon 0 – Syketilfelle startdato
#seksjonstabell(
    (10%, 65%, 1fr),
    table.header(
        [*0*], [*Syketilfelle startdato*], [],
    ),
    [*0*],
    if data.syketilfelle.egenmeldt [ Startdato for egenmeldingen ] else [ Når startet det legemeldte fraværet (angitt av behandler) ],
    [#{ data.syketilfelle.startdato }],
)

// Seksjon 1 – Pasientopplysninger
#{
    let p = data.pasientopplysninger
    seksjonstabell(
        (10%, 65%, 1fr),
        table.header(
            [*1*], [*Pasientopplysninger*], [],
        ),
        [*1.1.1*], [Etternavn], [#p.etternavn],
        [*1.1.2*], [Fornavn], [#p.fornavn],
        [*1.2*], [Fødselsnummer], [#p.fnr],
        ..if p.visKontaktOgFastlege {
            (
                [*1.3*], [Telefon], [#{ p.telefon }],
                [*1.4*], [Navn på pasientens fastlege], [#{ p.navnFastlege }],
            )
        } else { () },
    )
}

// Seksjon 2 – Arbeidsgiver
#{
    let a = data.arbeidsgiver
    seksjonstabell(
        (10%, 65%, 1fr),
        table.header(
            [*2*], [*Arbeidsgiver*], [],
        ),
        [*2.1*], [Pasienten har:],
        if a.type == "EN_ARBEIDSGIVER" [ Én arbeidsgiver ]
        else if a.type == "FLERE_ARBEIDSGIVERE" [ Flere arbeidsgivere ]
        else [ Ingen arbeidsgiver ],
        ..if a.type != "INGEN_ARBEIDSGIVER" {
            (
                [*2.2*], [Arbeidsgiver for denne perioden], [#{ a.navn }],
                [*2.3*],
                if a.egenmeldt [ Yrke/stilling for denne egenmeldingen ] else [ Yrke/stilling for denne sykmeldingen ],
                [#{ a.yrkesbetegnelse }],
                [*2.4*], [Stillingsprosent], [#{ a.stillingsprosent }],
            )
        } else { () },
    )
}

// Seksjon 3 – Diagnose
#{
    let d = data.diagnose
    seksjonstabell(
        (10%, 20%, 30%, 15%, 1fr),
        table.header(
            [*3*], [*Diagnose*], [], [], [],
        ),
        ..if d.hovedDiagnose != none {
            (
                [*3.1*], [*Hoveddiagnose*],
                [*3.1.1 Kodesystem* \ #d.hovedDiagnose.system],
                [*3.1.2 Kode* \ #d.hovedDiagnose.kode],
                [*3.1.3 Diagnose* \ #{ d.hovedDiagnose.tekst }],
            )
        } else { () },
        ..if d.biDiagnoser.len() > 0 {
            (
                [*3.2*], [*Bidiagnoser*],
                [*3.2.1 Kodesystem*], [*3.2.2 Kode*], [*3.2.3 Diagnose*],
            ) + d.biDiagnoser.map(b => (
                [], [],
                [#b.system],
                [#b.kode],
                [#{ b.tekst }],
            )).flatten()
        } else { () },
        // 3.3 Annen fraværsgrunn
        ..if d.annenFravarsgrunn != none {
            (
                [*3.3*], [*Annen fraværsgrunn*],
                [*3.3.1 Lovfestet fraværsgrunn* #list(..d.annenFravarsgrunn.arsaker.map(a => [#a]))],
                table.cell(colspan: 2)[*3.3.2 Beskriv fravær* \ #{ d.annenFravarsgrunn.beskrivelse }],
            )
        } else { () },
        // 3.4 Svangerskap (ja-utsagn)
        ..if d.svangerskap {
            ([*3.4*], table.cell(colspan: 4)[#sym.ballot.check *Sykdommen er svangerskapsrelatert*])
        } else { () },
        // 3.5 Yrkesskade (ja-utsagn) + 3.6 skadedato
        ..if d.yrkesskade {
            (
                [*3.5*], table.cell(colspan: 4)[#sym.ballot.check *Sykmeldingen kan skyldes yrkesskade/yrkessykdom*],
                [*3.6*], [*Skadedato*], table.cell(colspan: 3)[#{ d.yrkesskadeDato }],
            )
        } else { () },
        // 3.7 Skjermet for pasient (ja-utsagn)
        ..if d.skjermetForPasient {
            ([*3.7*], table.cell(colspan: 4)[#sym.ballot.check *Pasienten må skjermes for medisinske opplysninger*])
        } else { () },
    )
}
