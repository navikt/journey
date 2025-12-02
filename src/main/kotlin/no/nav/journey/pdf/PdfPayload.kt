package no.nav.journey.pdf

import no.nav.tsm.sykmelding.input.core.model.Aktivitet
import no.nav.tsm.sykmelding.input.core.model.Aktivitetstype
import no.nav.tsm.sykmelding.input.core.model.SporsmalSvar
import no.nav.tsm.sykmelding.input.core.model.Sykmelding
import no.nav.tsm.sykmelding.input.core.model.ValidationResult
import no.nav.tsm.sykmelding.input.core.model.metadata.MessageMetadata


data class PdfPayload(
    val metadata: MessageMetadata,
    val sykmelding: Sykmelding,
    val validation: ValidationResult,
    val aktiviteter: Map<Aktivitetstype, List<Aktivitet>>,
    val utdypendeOpplysninger: Map<String, Map<String, SporsmalSvar>>?,
)
