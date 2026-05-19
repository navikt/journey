#let payload_path = sys.inputs.at("data-path")
#let data = json(payload_path)
#let sykmelding = data.at("sykmelding", default: (:))
#let arbeidsgiver = sykmelding.at("arbeidsgiver", default: (:))
#let medisinsk = sykmelding.at("medisinskVurdering", default: (:))
#let hoveddiagnose = medisinsk.at("hovedDiagnose", default: none)
#let bidiagnoser = medisinsk.at("biDiagnoser", default: ())
#let utdypende = data.at("utdypendeOpplysninger", default: (:))
#let workplace_considerations_suffix = ".3"

#set page(margin: 1cm)
#set text(font: ("Source Sans Pro", "Noto Color Emoji", "DejaVu Sans"), lang: "nb", size: 10pt)

= Sykmelding

== Arbeidsgiver
#let arbeidsgiver_type = str(arbeidsgiver.at("type", default: ""))
#let is_flere_arbeidsgivere = arbeidsgiver_type == "FLERE_ARBEIDSGIVERE" or arbeidsgiver_type == "FlereArbeidsgivere"
#if is_flere_arbeidsgivere [
  Flere arbeidsgivere
]
#arbeidsgiver.at("navn", default: "")
#linebreak()
#arbeidsgiver.at("stillingstittel", default: "")
#linebreak()
#arbeidsgiver.at("stillingsprosent", default: "")

== Utdypende opplysninger
#for (_, gruppe) in utdypende.pairs() [
  #for (key, svar) in gruppe.pairs() [
    #linebreak()
    #if str(key).ends-with(workplace_considerations_suffix) [hvilke hensyn på arbeidsplassen?]
    #linebreak()
    #svar.at("sporsmal", default: "")
    #linebreak()
    #svar.at("svar", default: "")
  ]
]

== Diagnoser
#if hoveddiagnose != none {
  #let system = str(hoveddiagnose.at("system", default: ""))
  #if system == "ICPC2B" [ICPC-2B] else [#system]
  #linebreak()
  #hoveddiagnose.at("kode", default: "")
}

#for diagnose in bidiagnoser [
  #linebreak()
  #diagnose.at("kode", default: "")
]

#if data.at("validation", default: (:)).at("status", default: "") == "INVALID" {
  *Avvist sykmelding*
}
