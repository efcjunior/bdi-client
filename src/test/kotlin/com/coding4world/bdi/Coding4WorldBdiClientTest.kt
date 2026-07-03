package com.coding4world.bdi

import com.coding4world.bdi.html.Coding4WorldBdiPageParser
import com.coding4world.bdi.http.ContentFetcher
import com.coding4world.bdi.pdf.BdiPdfParser
import com.coding4world.bdi.pdf.PdfTextExtractor
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.net.URI
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts

class Coding4WorldBdiClientTest {
    @Test
    fun `retrieves and interprets the current publication`() {
        val pdfUri = URI("https://www.coding4world.com/bdi/files/current-bdi.pdf")
        val pdfBytes = createBdiPdf()
        val fetcher = StubContentFetcher(PAGE_HTML, pdfUri, pdfBytes)
        val fetchedAt = Instant.parse("2026-06-28T15:30:00Z")
        val client = Coding4WorldBdiClient(
            fetcher = fetcher,
            pageParser = Coding4WorldBdiPageParser(),
            pdfTextExtractor = PdfTextExtractor(),
            pdfParser = BdiPdfParser(),
            clock = Clock.fixed(fetchedAt, ZoneOffset.UTC),
            pageUri = BDI_PAGE_URI,
        )

        val result = client.current()

        assertEquals(BigDecimal("35.08"), result.value)
        assertEquals(LocalDate.of(2026, 6, 28), result.validFrom)
        assertEquals(pdfUri, result.sourcePdf)
        assertEquals(fetchedAt, result.fetchedAt)
        assertEquals(
            listOf(
                BDI_PAGE_URI,
                pdfUri,
            ),
            fetcher.requestedUris,
        )
    }

    @Test
    fun `offers the public no-argument API`() {
        Coding4WorldBdiClient()
    }

    private fun createBdiPdf(): ByteArray =
        PDDocument().use { document ->
            val page = PDPage()
            document.addPage(page)
            PDPageContentStream(document, page).use { content ->
                val font = PDType1Font(Standard14Fonts.FontName.HELVETICA)
                content.drawText("Road Maintenance", 200f, 700f, font)
                content.drawText("% over SP", 220f, 660f, font)
                content.drawText("% over DC", 300f, 660f, font)
                content.drawText("% over DC", 430f, 660f, font)
                content.drawText("Total - BDI (%)", 100f, 580f, font)
                content.drawText("25.97", 230f, 580f, font)
                content.drawText("35.08", 310f, 580f, font)
                content.drawText("91.00", 440f, 580f, font)
                content.drawText("BDI Values - With tax relief", 80f, 500f, font)
            }

            ByteArrayOutputStream().use { output ->
                document.save(output)
                output.toByteArray()
            }
        }

    private fun PDPageContentStream.drawText(
        text: String,
        x: Float,
        y: Float,
        font: PDType1Font,
    ) {
        beginText()
        setFont(font, 12f)
        newLineAtOffset(x, y)
        showText(text)
        endText()
    }

    private class StubContentFetcher(
        private val html: String,
        private val pdfUri: URI,
        private val pdfBytes: ByteArray,
    ) : ContentFetcher {
        val requestedUris = mutableListOf<URI>()

        override fun fetchText(uri: URI): String {
            requestedUris += uri
            return html
        }

        override fun fetchBytes(uri: URI): ByteArray {
            requestedUris += uri
            assertEquals(pdfUri, uri)
            return pdfBytes
        }
    }

    private companion object {
        val BDI_PAGE_URI = URI(
            "https://www.gov.br/dnit/pt-br/assuntos/planejamento-e-pesquisa/" +
                "custos-referenciais/sistemas-de-custos/bdi/bdi-2",
        )
        val PAGE_HTML = """
            <html>
              <body>
                <table class="plain">
                  <tr>
                    <th>Effective Period</th>
                    <th>Reason for Change</th>
                    <th>BDI</th>
                  </tr>
                  <tr>
                    <td>Since June 28, 2026</td>
                    <td>279th Committee Meeting</td>
                    <td><a href="https://www.coding4world.com/bdi/files/current-bdi.pdf">Circular Letter</a></td>
                  </tr>
                </table>
              </body>
            </html>
        """.trimIndent()
    }
}
