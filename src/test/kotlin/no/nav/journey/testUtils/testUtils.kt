package no.nav.journey.testUtils

import org.apache.tika.Tika
import java.io.File
import java.io.InputStreamReader
import java.time.LocalDate

fun loadResourceAsString(resourceName: String): String {
    val inputStream = ClassLoader.getSystemResourceAsStream(resourceName)
        ?: throw IllegalArgumentException("Resource not found: $resourceName")
    return InputStreamReader(inputStream, Charsets.UTF_8).readText()
}

fun extractTextFromPdf(file: File): String {
    val tika = Tika()
    return tika.parseToString(file)
}


class TestUtils {
    companion object {
        internal fun Int.januar(year: Int) = LocalDate.of(year, 1, this)

        internal fun Int.februar(year: Int) = LocalDate.of(year, 2, this)

        internal fun Int.mars(year: Int) = LocalDate.of(year, 3, this)

        internal fun Int.juni(year: Int) = LocalDate.of(year, 6, this)
    }
}