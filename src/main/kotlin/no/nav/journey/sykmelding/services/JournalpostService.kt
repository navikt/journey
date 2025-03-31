package no.nav.journey.sykmelding.services

import no.nav.journey.sykmelding.models.AvsenderSystem
import no.nav.journey.sykmelding.models.SykmeldingRecord
import no.nav.journey.utils.applog
import org.springframework.stereotype.Service

@Service
class JournalpostService(
    val bucketService: BucketService
) {

    val log = applog()

    fun createJournalpost(
        sykmelding: SykmeldingRecord,
    ) {
        val avsenderSystem = sykmelding.sykmelding.metadata.avsenderSystem
        if (!skalOpprettePdf(avsenderSystem)){
            log.info("Oppretter ikke ny pdf for papirsykmelding ${sykmelding.sykmelding.id} fordi avsenderSystem er: $avsenderSystem")
            return
        }
        val vedlegg = sykmelding.metadata.vedlegg
        log.info("vedlegg: $vedlegg")
        vedlegg.apply { bucketService.getVedleggFromBucket(sykmelding.sykmelding.id) }
        if (!vedlegg.isNullOrEmpty()) {
            vedlegg.apply { bucketService.getVedleggFromBucket(sykmelding.sykmelding.id) }
        }
    }


    private fun skalOpprettePdf(avsenderSystem: AvsenderSystem): Boolean {
        return !((avsenderSystem.navn == "Papirsykmelding" || avsenderSystem.navn == "syk-dig") &&
                avsenderSystem.versjon != "1")
    }
}