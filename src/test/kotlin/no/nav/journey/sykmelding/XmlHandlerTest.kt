package no.nav.journey.sykmelding

import no.nav.journey.testUtils.loadResourceAsString
import no.nav.journey.utils.XmlHandler
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class XmlHandlerTest {
    private val xmlHandler = XmlHandler()

    @Test
    fun `unmarshal correctly`(){
        val xml = loadResourceAsString("fellesformat/fellesformatMedVedlegg.xml")
        val result = xmlHandler.unmarshal(xml)
        assertNotNull(result)
    }
}