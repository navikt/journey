#let payload_path = sys.inputs.at("data-path")
#let payload_json = read(payload_path)
#let data = json(payload_path)

#set page(margin: 1cm)
#set text(font: ("Source Sans Pro", "Noto Color Emoji", "DejaVu Sans"), lang: "nb", size: 10pt)

= Sykmelding

Flere arbeidsgivere
Gi en kort medisinsk oppsummering av tilstanden
Beskriv kort hvilke helsemessige begrensninger som gjør det vanskelig å jobbe gradert
hvilke hensyn på arbeidsplassen?
ICPC-2B

#if data.at("validation", default: (:)).at("status", default: "") == "INVALID" {
  *Avvist sykmelding*
}

#text(payload_json)
