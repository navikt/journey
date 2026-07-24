package no.nav.tsm.pdf

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.awt.Font
import java.io.File
import java.nio.file.Files
import no.nav.tsm.utils.logger
import no.nav.tsm.utils.teamLogger

class TypstClient(
    private val typstBinaryPath: String = "/app/typst-pdf/typst",
    private val templatePath: String = "/app/typst-pdf/sykmelding.typ",
    private val fontPath: String = "/app/typst-pdf/fonts",
) {
    private val logger = logger()
    private val teamlog = teamLogger()

    private val fonts: List<Font> by lazy {
        File(fontPath)
            .listFiles { _, name -> name.endsWith(".ttf", ignoreCase = true) }
            ?.mapNotNull { file ->
                runCatching { Font.createFont(Font.TRUETYPE_FONT, file) }
                    .onFailure { logger.warn("Could not load font ${file.name}: ${it.message}") }
                    .getOrNull()
            } ?: emptyList()
    }

    fun createPdf(payload: TypstPayload): ByteArray {
        logger.info("Generating PDF for sykmelding id ${payload.sykmeldingId} using Typst")

        val jsonData = objectMapper.writeValueAsString(payload)

        return try {
            runTypst(payload.sykmeldingId, jsonData)
        } catch (e: TypstCompilationException) {
            val dropped = mutableListOf<String>()
            val filtered = filterUndisplayable(jsonData, dropped)
            logger.warn("Error during typst, retrying by removing invalid codepoints")
            teamlog.warn(
                "Typst failed for sykmelding id ${payload.sykmeldingId}; " +
                    "retrying after dropping undisplayable chars: $dropped. " +
                    "Original error: ${e.message}"
            )
            runTypst(payload.sykmeldingId, filtered)
        }
    }

    private fun canDisplay(codePoint: Int): Boolean = fonts.any { it.canDisplay(codePoint) }

    private fun filterUndisplayable(input: String, dropped: MutableList<String>): String =
        input
            .codePoints()
            .filter { cp ->
                val ok = cp < 0x80 || canDisplay(cp)
                if (!ok) dropped.add("U+%04X".format(cp))
                ok
            }
            .collect(::StringBuilder, StringBuilder::appendCodePoint, StringBuilder::append)
            .toString()

    private fun runTypst(id: String, jsonData: String): ByteArray {
        val dataFile = Files.createTempFile(id, ".json")
        try {
            Files.writeString(dataFile, jsonData)

            val process =
                ProcessBuilder(
                        typstBinaryPath,
                        "compile",
                        "--pdf-standard=a-2a",
                        "--root=/",
                        "--font-path=$fontPath",
                        "--input=data-path=${dataFile}",
                        templatePath,
                        "-",
                    )
                    .redirectError(ProcessBuilder.Redirect.PIPE)
                    .start()

            var stderr = ""
            val stderrThread = Thread { stderr = process.errorStream.bufferedReader().readText() }
            stderrThread.start()
            val pdfBytes = process.inputStream.readBytes()
            stderrThread.join()
            val exitCode = process.waitFor()

            if (exitCode != 0) {
                logger.error("Typst compilation failed with exit code $exitCode")
                teamlog.error("Typst compilation failed with exit code $exitCode: $stderr")
                throw TypstCompilationException("Typst compilation failed: $stderr")
            }

            return pdfBytes
        } finally {
            Files.deleteIfExists(dataFile)
        }
    }

    val objectMapper =
        jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
}

class TypstCompilationException(message: String) : RuntimeException(message)
