package com.coding4world.bdi.pdf

import com.coding4world.bdi.exception.Coding4WorldBdiException
import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts

class PdfTextExtractorTest {
    @Test
    fun `extracts text from PDF bytes held in memory`() {
        val bytes = createPdf("Road Maintenance")
        val document = PdfTextExtractor().extract(bytes)

        val block = document.blocks.single { it.text == "Road Maintenance" }
        assertEquals(1, block.page)
        assertTrue(block.left < block.right)
        assertTrue(block.top < block.bottom)
    }

    @Test
    fun `rejects empty content`() {
        assertFailsWith<Coding4WorldBdiException> { PdfTextExtractor().extract(byteArrayOf()) }
    }

    private fun createPdf(text: String): ByteArray =
        PDDocument().use { document ->
            val page = PDPage()
            document.addPage(page)
            PDPageContentStream(document, page).use { content ->
                content.beginText()
                content.setFont(PDType1Font(Standard14Fonts.FontName.HELVETICA), 12f)
                content.newLineAtOffset(72f, 720f)
                content.showText(text)
                content.endText()
            }

            ByteArrayOutputStream().use { output ->
                document.save(output)
                output.toByteArray()
            }
        }
}
