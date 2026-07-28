package no.nav.tsm.pdf

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import java.awt.Font
import java.io.File
import java.nio.file.Files
import no.nav.tsm.ktor.logger
import no.nav.tsm.ktor.teamLogger

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

    @WithSpan
    fun createPdf(payload: TypstPayload): ByteArray {
        val current = Span.current()
        logger.info("Generating PDF for sykmelding id ${payload.sykmeldingId} using Typst")

        val jsonData = objectMapper.writeValueAsString(payload)
        current.setAttribute("typst.sykmeldingId", payload.sykmeldingId)
        current.setAttribute("typst.payloadSizeKb", (jsonData.length / 1024).toString())

        return try {
            runTypst(payload.sykmeldingId, jsonData, firstAttempt = true)
        } catch (e: TypstCompilationException) {
            current.setAttribute("typst.retry", true)

            val dropped = mutableListOf<String>()
            val filtered = filterUndisplayable(jsonData, dropped)
            logger.warn("Error during typst, retrying by removing invalid codepoints")
            teamlog.warn(
                "Typst failed for sykmelding id ${payload.sykmeldingId}; " +
                    "retrying after dropping undisplayable chars: $dropped. " +
                    "Original error: ${e.message}"
            )

            current.setAttribute("typst.retry.dropped", dropped.joinToString(","))
            runTypst(payload.sykmeldingId, filtered, firstAttempt = false).also {
                current.setAttribute("typst.retry.success", true)
                logger.info(
                    "Had to retry typst, but succeeded on second attempt for sykmelding id ${payload.sykmeldingId}"
                )
            }
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

    @WithSpan
    private fun runTypst(id: String, jsonData: String, firstAttempt: Boolean): ByteArray {
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
                if (!firstAttempt) {
                    logger.error("Typst compilation failed with exit code $exitCode")
                }
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
