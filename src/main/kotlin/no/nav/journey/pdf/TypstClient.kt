package no.nav.journey.pdf

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.journey.utils.applog
import java.nio.file.Files

class TypstClient(
    private val typstBinaryPath: String = "/app/typst-pdf/typst",
    private val templatePath: String = "/app/typst-pdf/sm.typ",
    private val fontPath: String = "/app/fonts",
) {
    private val log = applog()
    private val objectMapper = jacksonObjectMapper()

    fun createPdf(payload: PdfPayload): ByteArray {
        val dataFile = Files.createTempFile("journey-", ".json")
        return try {
            Files.writeString(dataFile, objectMapper.writeValueAsString(payload))

            val typstBinary = when {
                Files.isExecutable(java.nio.file.Path.of(typstBinaryPath)) -> typstBinaryPath
                else -> "typst"
            }

            val process = ProcessBuilder(
                typstBinary,
                "compile",
                "--pdf-standard=a-2a",
                "--root=/",
                "--font-path=$fontPath",
                "--input=data-path=$dataFile",
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
                log.error("Typst compilation failed with exit code $exitCode: $stderr")
                throw RuntimeException("Typst compilation failed: $stderr")
            }

            pdfBytes
        } finally {
            Files.deleteIfExists(dataFile)
        }
    }
}
