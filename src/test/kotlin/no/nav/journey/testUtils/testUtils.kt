package no.nav.journey.testUtils

import java.io.InputStreamReader

fun loadResourceAsString(resourceName: String): String {
    val inputStream = ClassLoader.getSystemResourceAsStream(resourceName)
        ?: throw IllegalArgumentException("Resource not found: $resourceName")
    return InputStreamReader(inputStream, Charsets.UTF_8).readText()
}