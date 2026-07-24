package no.nav.tsm.sykmelding.utils

import com.migesok.jaxb.adapter.javatime.LocalDateTimeXmlAdapter
import com.migesok.jaxb.adapter.javatime.LocalDateXmlAdapter
import java.io.StringReader
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import javax.xml.bind.DatatypeConverter
import javax.xml.bind.JAXBContext
import javax.xml.bind.Unmarshaller
import javax.xml.parsers.SAXParserFactory
import javax.xml.transform.Source
import javax.xml.transform.sax.SAXSource
import no.nav.helse.eiFellesformat.XMLEIFellesformat
import no.nav.helse.msgHead.XMLMsgHead
import no.nav.helse.sm2013.HelseOpplysningerArbeidsuforhet
import no.nav.tsm.sykmelding.journalpost.Content
import no.nav.tsm.sykmelding.journalpost.Vedlegg
import org.w3c.dom.Element
import org.xml.sax.InputSource

inline fun <reified T> XMLEIFellesformat.get() = this.any.find { it is T } as T

object XmlHandler {
    val fellesformatJaxBContext: JAXBContext =
        JAXBContext.newInstance(
            XMLEIFellesformat::class.java,
            XMLMsgHead::class.java,
            HelseOpplysningerArbeidsuforhet::class.java,
        )
    val fellesformatUnmarshaller: Unmarshaller =
        fellesformatJaxBContext.createUnmarshaller().apply {
            setAdapter(LocalDateTimeXmlAdapter::class.java, XMLDateTimeAdapter())
            setAdapter(LocalDateXmlAdapter::class.java, XMLDateAdapter())
        }

    fun unmarshal(inputMessageText: String): XMLEIFellesformat {
        // Disable XXE
        val spf: SAXParserFactory = SAXParserFactory.newInstance()
        spf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        spf.isNamespaceAware = true

        val xmlSource: Source =
            SAXSource(
                spf.newSAXParser().xmlReader,
                InputSource(StringReader(inputMessageText)),
            )
        return fellesformatUnmarshaller.unmarshal(xmlSource) as XMLEIFellesformat
    }

    fun getVedlegg(fellesformat: XMLEIFellesformat): List<Vedlegg>? {
        val vedleggList =
            fellesformat
                .get<XMLMsgHead>()
                .document
                .filter { it.documentConnection?.v == "V" }
                .map {
                    val description = it.refDoc.description
                    val type = it.refDoc.mimeType

                    if (it.refDoc.content.any.size > 1) {
                        throw RuntimeException("Unsupported content")
                    }
                    val contentElement = it.refDoc.content.any.first() as Element
                    val contentType = contentElement.localName
                    val content = contentElement.textContent
                    Vedlegg(Content(contentType, content), type, description)
                }
        return vedleggList.ifEmpty { null }
    }

    class XMLDateTimeAdapter : LocalDateTimeXmlAdapter() {
        override fun unmarshal(stringValue: String?): LocalDateTime? =
            when (stringValue) {
                null -> null
                else ->
                    (DatatypeConverter.parseDateTime(stringValue) as GregorianCalendar)
                        .toZonedDateTime()
                        .toLocalDateTime()
            }
    }

    class XMLDateAdapter : LocalDateXmlAdapter() {
        override fun unmarshal(stringValue: String?): LocalDate? =
            when (stringValue) {
                null -> null
                else -> DatatypeConverter.parseDate(stringValue).toInstant().atZone(ZoneOffset.MAX).toLocalDate()
            }
    }
}
