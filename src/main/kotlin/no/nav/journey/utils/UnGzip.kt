package no.nav.journey.utils

import java.io.ByteArrayInputStream
import java.util.zip.GZIPInputStream

fun ungzip(input: ByteArray): String {
    ByteArrayInputStream(input).use { byteStream ->
        GZIPInputStream(byteStream).use { gzipStream ->
            return gzipStream.bufferedReader(Charsets.UTF_8).readText()
        }
    }
}