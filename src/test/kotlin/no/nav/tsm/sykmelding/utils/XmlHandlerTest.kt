package no.nav.tsm.sykmelding.utils

import io.kotest.matchers.nulls.shouldNotBeNull
import kotlin.test.Test
import testUtils.loadResourceAsString

class XmlHandlerTest {

    @Test
    fun `unmarshal correctly`() {
        val xml = loadResourceAsString("fellesformat/fellesformatMedVedlegg.xml")
        val result = XmlHandler.unmarshal(xml)
        result.shouldNotBeNull()
    }
}
