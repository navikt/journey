package no.nav.journey.sykmelding.services

import no.nav.journey.sykmelding.api.DokarkivClient
import no.nav.journey.sykmelding.models.SykmeldingRecord
import no.nav.journey.sykmelding.models.metadata.EmottakEnkel
import no.nav.journey.sykmelding.models.metadata.MetadataType
import no.nav.journey.utils.applog
import org.springframework.stereotype.Service

@Service
class JournalpostService(
    val dokarkivClient: DokarkivClient,
    val bucketService: BucketService,
    val pdfService: PdfService,
) {
    val log = applog()

    fun createJournalpost(
        sykmelding: SykmeldingRecord,
    ) {
        val metadataType = sykmelding.metadata.type
        if (metadataType != MetadataType.EMOTTAK){
            log.info("Oppretter ikke ny pdf for papirsykmelding ${sykmelding.sykmelding.id} fordi metadataType er: $metadataType")
            return
        }
       /* val metadata = sykmelding.metadata
        if (metadata is EmottakEnkel) {
            if (!metadata.vedlegg.isNullOrEmpty()){
                log.info("skal hente vedlegg for sykmelding ${sykmelding.sykmelding.id}")
                metadata.vedlegg.apply { bucketService.getVedleggFromBucket(sykmelding.sykmelding.id) }
            }
        }*/
        // TODO: create pdf
    }
}

