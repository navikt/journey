package no.nav.journey.pdf

import java.awt.geom.AffineTransform
import java.awt.image.AffineTransformOp
import java.awt.image.BufferedImage
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDMetadata
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDMarkInfo
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureTreeRoot
import org.apache.pdfbox.pdmodel.graphics.color.PDOutputIntent
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.apache.pdfbox.pdmodel.interactive.viewerpreferences.PDViewerPreferences
import org.apache.pdfbox.util.Matrix
import org.apache.xmpbox.XMPMetadata
import org.apache.xmpbox.type.BadFieldValueException
import org.apache.xmpbox.xml.XmpSerializer
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.Calendar
import javax.imageio.ImageIO

fun imageToPDFA(imageStream: InputStream, outputStream: OutputStream) {
    PDDocument().use { document ->
        val page = PDPage(PDRectangle.A4)
        document.addPage(page)
        val image = toPortait(ImageIO.read(imageStream))

        val quality = 1.0f

        val pdImage =
            try {
                JPEGFactory.createFromImage(document, image, quality)
            } catch (e: javax.imageio.IIOException) {
                // To avoid "javax.imageio.IIOException: Illegal band size: should be 0 < size <= 8"
                // for certain black/white pictures
                LosslessFactory.createFromImage(document, image)
            }

        val imageSize = scale(pdImage, page)
        val xOffset = (page.cropBox.width - imageSize.width) / 2f
        val yOffset = (page.cropBox.height - imageSize.height) / 2f

        PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, false).use {
            it.drawImage(pdImage, Matrix(imageSize.width, 0f, 0f, imageSize.height, xOffset, yOffset))
        }

        val xmp = XMPMetadata.createXMPMetadata()
        val catalog = document.documentCatalog
        val cal = Calendar.getInstance()

        try {
            val dc = xmp.createAndAddDublinCoreSchema()
            dc.addCreator("pdfgen-coree")
            dc.addDate(cal)

            val id = xmp.createAndAddPDFAIdentificationSchema()
            id.part = 2
            id.conformance = "U"

            val serializer = XmpSerializer()
            val baos = ByteArrayOutputStream()
            serializer.serialize(xmp, baos, true)

            val metadata = PDMetadata(document)
            metadata.importXMPMetadata(baos.toByteArray())
            catalog.metadata = metadata
        } catch (e: BadFieldValueException) {
            throw IllegalArgumentException(e)
        }

        val colorProfileStream =
            object {}.javaClass.getResourceAsStream("/pdf/sRGB2014.icc")
                ?: throw IllegalStateException("Fant ikke fargeprofil /pdf/sRGB2014.icc på classpath")

        val intent =
            colorProfileStream.use { PDOutputIntent(document, it) }
        intent.info = "sRGB IEC61966-2.1"
        intent.outputCondition = "sRGB IEC61966-2.1"
        intent.outputConditionIdentifier = "sRGB IEC61966-2.1"
        intent.registryName = "http://www.color.org"
        catalog.addOutputIntent(intent)

        catalog.language = "nb-NO"

        val pdViewer = PDViewerPreferences(page.cosObject)
        pdViewer.setDisplayDocTitle(true)
        catalog.viewerPreferences = pdViewer

        catalog.markInfo = PDMarkInfo(page.cosObject)
        catalog.structureTreeRoot = PDStructureTreeRoot()
        catalog.markInfo.isMarked = true

        document.save(outputStream)
        document.close()
    }
}

private data class ImageSize(val width: Float, val height: Float)

private fun toPortait(image: BufferedImage): BufferedImage {
    // TYPE_CUSTOM (0) er ikke støttet av AffineTransformOp — normaliser til ARGB
    val normalized =
        if (image.type == BufferedImage.TYPE_CUSTOM) {
            BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_ARGB).also {
                val g = it.createGraphics()
                g.drawImage(image, 0, 0, null)
                g.dispose()
            }
        } else {
            image
        }

    if (normalized.height >= normalized.width) {
        return normalized
    }

    val rotateTransform =
        AffineTransform.getRotateInstance(
            Math.toRadians(90.0),
            (normalized.height / 2f).toDouble(),
            (normalized.height / 2f).toDouble(),
        )

    return AffineTransformOp(rotateTransform, AffineTransformOp.TYPE_BILINEAR)
        .filter(normalized, BufferedImage(normalized.height, normalized.width, normalized.type))
}

private fun scale(image: PDImageXObject, page: PDPage): ImageSize {
    var width = image.width.toFloat()
    var height = image.height.toFloat()

    if (width > page.cropBox.width) {
        width = page.cropBox.width
        height = width * image.height.toFloat() / image.width.toFloat()
    }

    if (height > page.cropBox.height) {
        height = page.cropBox.height
        width = height * image.width.toFloat() / image.height.toFloat()
    }

    return ImageSize(width, height)
}