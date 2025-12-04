package no.nav.journey.utils

import java.io.ByteArrayInputStream
import java.util.zip.GZIPInputStream

fun ungzip(input: ByteArray): ByteArray {
    ByteArrayInputStream(input).use { byteStream ->
        GZIPInputStream(byteStream).use { gzipStream ->
            return gzipStream.readBytes()
        }
    }
}
