package com.coding4world.bdi.pdf

import com.coding4world.bdi.exception.Coding4WorldBdiException
import java.io.IOException
import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import org.apache.pdfbox.text.TextPosition

internal class PdfTextExtractor {
    fun extract(pdfBytes: ByteArray): PdfTextDocument {
        if (pdfBytes.isEmpty()) {
            throw Coding4WorldBdiException("The downloaded PDF is empty")
        }

        return try {
            Loader.loadPDF(pdfBytes).use { document ->
                val stripper = PositionedTextStripper()
                stripper.getText(document)
                PdfTextDocument(stripper.blocks.toList())
            }
        } catch (exception: IOException) {
            throw Coding4WorldBdiException("Could not extract text from the downloaded PDF", exception)
        }
    }

    private class PositionedTextStripper : PDFTextStripper() {
        val blocks = mutableListOf<PdfTextBlock>()

        init {
            sortByPosition = true
        }

        override fun writeString(text: String, textPositions: List<TextPosition>) {
            val content = text.trim()
            if (content.isNotEmpty() && textPositions.isNotEmpty()) {
                blocks += PdfTextBlock(
                    text = content,
                    page = currentPageNo,
                    left = textPositions.minOf(TextPosition::getXDirAdj),
                    top = textPositions.minOf(TextPosition::getYDirAdj),
                    right = textPositions.maxOf { it.xDirAdj + it.widthDirAdj },
                    bottom = textPositions.maxOf { it.yDirAdj + it.heightDir },
                )
            }
            super.writeString(text, textPositions)
        }
    }
}
