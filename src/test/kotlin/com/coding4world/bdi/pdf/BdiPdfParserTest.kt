package com.coding4world.bdi.pdf

import com.coding4world.bdi.exception.Coding4WorldBdiException
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BdiPdfParserTest {
    private val parser = BdiPdfParser()

    @Test
    fun `reads the value at the geometric intersection of row and column`() {
        val document = tableDocument()

        assertEquals(BigDecimal("35.08"), parser.parse(document))
    }

    @Test
    fun `recalculates the intersection when the entire table moves`() {
        val document = tableDocument(offsetX = 173f, offsetY = 241f, page = 3)

        assertEquals(BigDecimal("35.08"), parser.parse(document))
    }

    @Test
    fun `uses geometry instead of textual block order`() {
        val document = tableDocument().let { original ->
            PdfTextDocument(
                (original.blocks + block("88.88", x = 420f, y = 300f)).reversed(),
            )
        }

        assertEquals(BigDecimal("35.08"), parser.parse(document))
    }

    @Test
    fun `accepts labels split into multiple positioned blocks`() {
        val blocks = tableDocument().blocks
            .filterNot {
                it.text in setOf("Road Maintenance", "% over DC", "Total - BDI (%)")
            }
            .toMutableList()
            .apply {
                add(block("Road", x = 200f, y = 100f, width = 48f))
                add(block("Maintenance", x = 252f, y = 100f, width = 48f))
                add(block("%", x = 260f, y = 150f, width = 8f))
                add(block("over", x = 270f, y = 150f, width = 20f))
                add(block("DC", x = 292f, y = 150f, width = 8f))
                add(block("Total", x = 100f, y = 300f, width = 30f))
                add(block("- BDI", x = 132f, y = 300f, width = 32f))
                add(block("(%)", x = 166f, y = 300f, width = 24f))
            }

        assertEquals(BigDecimal("35.08"), parser.parse(PdfTextDocument(blocks)))
    }

    @Test
    fun `accepts an explicitly labeled table without tax relief`() {
        val document = tableDocument(explicitlyWithoutRelief = true, includeWithReliefMarker = false)

        assertEquals(BigDecimal("35.08"), parser.parse(document))
    }

    @Test
    fun `rejects an ambiguous cell intersection`() {
        val original = tableDocument()
        val ambiguous = PdfTextDocument(
            original.blocks + block("36.08", x = 272f, y = 300f),
        )

        val exception = assertFailsWith<Coding4WorldBdiException> { parser.parse(ambiguous) }

        assertEquals(
            "More than one BDI value intersects '% over DC' and 'Total - BDI (%)'",
            exception.message,
        )
    }

    @Test
    fun `does not fall back to another number when the intersecting cell is absent`() {
        val original = tableDocument()
        val missingCell = PdfTextDocument(
            original.blocks.filterNot { it.text == "35.08" },
        )

        val exception = assertFailsWith<Coding4WorldBdiException> { parser.parse(missingCell) }

        assertEquals(
            "Could not find a BDI value at the intersection of '% over DC' and 'Total - BDI (%)'",
            exception.message,
        )
    }

    @Test
    fun `does not read a BDI from another section`() {
        val document = PdfTextDocument(
            listOf(
                block("Total - BDI (%)", x = 100f, y = 300f),
                block("35.08", x = 270f, y = 300f),
            ),
        )

        val exception = assertFailsWith<Coding4WorldBdiException> { parser.parse(document) }

        assertEquals(
            "Could not locate the 'Road Maintenance' section in the PDF",
            exception.message,
        )
    }

    @Test
    fun `does not read the table with tax relief`() {
        val original = tableDocument(includeWithReliefMarker = false)
        val withRelief = PdfTextDocument(
            listOf(block("BDI Values - With tax relief", x = 80f, y = 70f, width = 180f)) +
                original.blocks.filterNot { it.text == "BDI Values - With tax relief" },
        )

        assertFailsWith<Coding4WorldBdiException> { parser.parse(withRelief) }
    }

    private fun tableDocument(
        offsetX: Float = 0f,
        offsetY: Float = 0f,
        page: Int = 1,
        explicitlyWithoutRelief: Boolean = false,
        includeWithReliefMarker: Boolean = true,
    ): PdfTextDocument {
        fun positioned(
            text: String,
            x: Float,
            y: Float,
            width: Float = 40f,
        ) = block(text, page, x + offsetX, y + offsetY, width)

        return PdfTextDocument(
            buildList {
                if (explicitlyWithoutRelief) {
                    add(positioned("Without tax relief", 180f, 75f, 100f))
                }
                add(positioned("Road Maintenance", 200f, 100f, 100f))
                add(positioned("% over SP", 210f, 150f, 40f))
                add(positioned("% over DC", 260f, 150f, 40f))
                add(positioned("% over DC", 360f, 150f, 40f))
                add(positioned("Total - BDI (%)", 100f, 300f, 90f))
                add(positioned("25.97", 220f, 300f, 24f))
                add(positioned("35.08", 270f, 300f, 24f))
                add(positioned("91.00", 370f, 300f, 24f))
                if (includeWithReliefMarker) {
                    add(positioned("BDI Values - With tax relief", 80f, 400f, 180f))
                }
            },
        )
    }

    private fun block(
        text: String,
        page: Int = 1,
        x: Float,
        y: Float,
        width: Float = 24f,
        height: Float = 10f,
    ) = PdfTextBlock(
        text = text,
        page = page,
        left = x,
        top = y,
        right = x + width,
        bottom = y + height,
    )
}
