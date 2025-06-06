package no.nav.journey.pdf

import no.nav.journey.sykmelding.models.Aktivitet
import no.nav.journey.sykmelding.models.Aktivitetstype
import no.nav.journey.sykmelding.models.Sykmelding
import no.nav.journey.sykmelding.models.metadata.MessageMetadata
import no.nav.journey.sykmelding.models.validation.ValidationResult

data class PdfPayload(
    val metadata: MessageMetadata,
    val sykmelding: Sykmelding,
    val validation: ValidationResult,
    val aktiviteter: Map<Aktivitetstype, List<Aktivitet>>,
)
