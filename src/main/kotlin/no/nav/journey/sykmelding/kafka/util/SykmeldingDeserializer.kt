package no.nav.journey.sykmelding.kafka.util

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.journey.sykmelding.models.SykmeldingModule
import no.nav.journey.utils.applog
import org.apache.kafka.common.errors.SerializationException
import org.apache.kafka.common.serialization.Deserializer
import kotlin.reflect.KClass

class SykmeldingDeserializer<T : Any>(private val type: KClass<T>) : Deserializer<T> {

    val log = applog()

    private val objectMapper: ObjectMapper = jacksonObjectMapper().apply {
            registerKotlinModule()
            registerModule(SykmeldingModule())
            registerModule(JavaTimeModule())
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        }

    override fun deserialize(topic: String, data: ByteArray): T {
        return try {
            objectMapper.readValue(data, type.java)
        } catch (ex: Exception) {
            val rawPayload = runCatching { String(data) }.getOrElse { "[kunne ikke leses som string]" }
            log.error("Klarte ikke å deserialisere melding på topic '$topic' payload $rawPayload, ${ex.message} ${ex.stackTrace}", ex)
            log.error("Payload: $rawPayload")
            throw SerializationException("Feil ved deserialisering av melding på topic '$topic'", ex)
        }
    }
}

