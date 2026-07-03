package com.coding4world.bdi.pdf

import com.coding4world.bdi.exception.Coding4WorldBdiException
import java.math.BigDecimal
import kotlin.math.abs
import kotlin.math.max

internal class BdiPdfParser {
    fun parse(document: PdfTextDocument): BigDecimal {
        val sections = findPhrase(document, CONSERVATION_SECTION)
        if (sections.isEmpty()) {
            throw Coding4WorldBdiException("Could not locate the 'Road Maintenance' section in the PDF")
        }

        val tables = sections.mapNotNull { locateTableWithoutTaxRelief(document, it) }
        val table = requireSingle(
            tables,
            missing = "Could not identify the 'Without tax relief' table in the PDF",
            ambiguous = "Found more than one 'Without tax relief' table for 'Road Maintenance'",
        )
        val directCostHeader = locateDirectCostHeader(document, table)
        val totalRow = locateTotalRow(document, table, directCostHeader)
        val valueBlock = locateValueAtIntersection(document, table, directCostHeader, totalRow)
        val value = parsePercentage(valueBlock.text)

        if (value <= BigDecimal.ZERO || value >= MAX_PERCENTAGE) {
            throw Coding4WorldBdiException("BDI percentage outside the expected range: $value")
        }
        return value
    }

    private fun locateTableWithoutTaxRelief(
        document: PdfTextDocument,
        section: PdfTextBlock,
    ): TableRegion? {
        val withoutReliefMarkers = findPhrase(document, WITHOUT_TAX_RELIEF, allowContainingBlock = true)
            .filter { it.page == section.page }
        val withReliefMarkers = findPhrase(document, WITH_TAX_RELIEF, allowContainingBlock = true)
            .filter { it.page == section.page }
        val markers = (withoutReliefMarkers.map { Marker(it, TaxRelief.WITHOUT) } +
            withReliefMarkers.map { Marker(it, TaxRelief.WITH) })
            .sortedBy { it.block.centerY }
        val previousMarker = markers.lastOrNull { it.block.centerY < section.centerY }

        if (previousMarker?.type == TaxRelief.WITH) return null

        val nextWithoutReliefMarker = withoutReliefMarkers
            .filter { it.centerY > section.centerY }
            .minByOrNull(PdfTextBlock::centerY)
        val nextWithReliefMarker = withReliefMarkers
            .filter { it.centerY > section.centerY }
            .minByOrNull(PdfTextBlock::centerY)
        val explicitlyWithoutRelief = previousMarker?.type == TaxRelief.WITHOUT ||
            (nextWithoutReliefMarker != null &&
                (nextWithReliefMarker == null || nextWithoutReliefMarker.centerY < nextWithReliefMarker.centerY))
        val implicitlyWithoutRelief = previousMarker == null && nextWithReliefMarker != null

        if (!explicitlyWithoutRelief && !implicitlyWithoutRelief) return null

        val top = nextWithoutReliefMarker
            ?.takeIf { it.centerY < (nextWithReliefMarker?.centerY ?: Float.MAX_VALUE) }
            ?.bottom
            ?: section.bottom
        val bottom = nextWithReliefMarker?.top ?: Float.MAX_VALUE
        return TableRegion(section, top, bottom)
    }

    private fun locateDirectCostHeader(
        document: PdfTextDocument,
        table: TableRegion,
    ): PdfTextBlock {
        val candidates = findPhrase(document, PERCENTAGE_OVER_DIRECT_COST)
            .filter { it.isInside(table) }
            .filter { it.overlapsHorizontally(table.section) }
            .sortedBy { it.centerY }
        if (candidates.isEmpty()) {
            throw Coding4WorldBdiException(
                "Could not locate the '% over DC' column under 'Road Maintenance'",
            )
        }

        val firstRowY = candidates.first().centerY
        val firstRow = candidates.filter {
            abs(it.centerY - firstRowY) <= max(it.height, candidates.first().height)
        }
        return requireSingle(
            firstRow,
            missing = "Could not locate the '% over DC' column under 'Road Maintenance'",
            ambiguous = "The '% over DC' column under 'Road Maintenance' is ambiguous",
        )
    }

    private fun locateTotalRow(
        document: PdfTextDocument,
        table: TableRegion,
        header: PdfTextBlock,
    ): PdfTextBlock {
        val candidates = findPhrase(document, TOTAL_BDI_ROW)
            .filter { it.isInside(table) && it.centerY > header.centerY }
        return requireSingle(
            candidates,
            missing = "Could not locate the 'Total - BDI (%)' row in the 'Without tax relief' table",
            ambiguous = "The 'Total - BDI (%)' row in the 'Without tax relief' table is ambiguous",
        )
    }

    private fun locateValueAtIntersection(
        document: PdfTextDocument,
        table: TableRegion,
        header: PdfTextBlock,
        row: PdfTextBlock,
    ): PdfTextBlock {
        val candidates = document.blocks
            .filter { it.isInside(table) }
            .filter { DECIMAL.matches(it.text.trim()) }
            .filter { it.isOnSameRowAs(row) }
            .filter { it.isInColumn(header) }

        return requireSingle(
            candidates,
            missing = "Could not find a BDI value at the intersection of '% over DC' and 'Total - BDI (%)'",
            ambiguous = "More than one BDI value intersects '% over DC' and 'Total - BDI (%)'",
        )
    }

    private fun findPhrase(
        document: PdfTextDocument,
        phrase: String,
        allowContainingBlock: Boolean = false,
    ): List<PdfTextBlock> {
        val target = normalize(phrase)
        val direct = document.blocks.filter {
            val text = normalize(it.text)
            text == target || (allowContainingBlock && text.contains(target))
        }
        val composite = groupIntoLines(document.blocks).flatMap { line ->
            buildCompositeMatches(line, target)
        }

        return (direct + composite)
            .distinctBy { listOf(it.page, it.left, it.top, it.right, it.bottom) }
    }

    private fun buildCompositeMatches(
        line: List<PdfTextBlock>,
        target: String,
    ): List<PdfTextBlock> = buildList {
        for (start in line.indices) {
            for (end in start..minOf(line.lastIndex, start + MAX_PHRASE_BLOCKS - 1)) {
                if (end == start) continue
                val blocks = line.subList(start, end + 1)
                val text = normalize(blocks.joinToString(" ") { it.text })
                if (text == target) {
                    add(blocks.merge(text = blocks.joinToString(" ") { it.text }))
                    break
                }
                if (text.length > target.length + PHRASE_LENGTH_TOLERANCE) break
            }
        }
    }

    private fun groupIntoLines(blocks: List<PdfTextBlock>): List<List<PdfTextBlock>> {
        val lines = mutableListOf<MutableList<PdfTextBlock>>()
        blocks.sortedWith(compareBy(PdfTextBlock::page, PdfTextBlock::centerY, PdfTextBlock::left))
            .forEach { block ->
                val line = lines.lastOrNull()
                val sameLine = line != null && line.first().page == block.page &&
                    abs(line.map(PdfTextBlock::centerY).average().toFloat() - block.centerY) <=
                    line.maxOf { max(it.height, block.height) }
                if (sameLine) {
                    line.add(block)
                    line.sortBy(PdfTextBlock::left)
                } else {
                    lines += mutableListOf(block)
                }
            }
        return lines
    }

    private fun PdfTextBlock.isInside(table: TableRegion): Boolean =
        page == table.section.page && centerY > table.top && centerY < table.bottom

    private fun PdfTextBlock.overlapsHorizontally(other: PdfTextBlock): Boolean =
        right >= other.left && left <= other.right

    private fun PdfTextBlock.isOnSameRowAs(row: PdfTextBlock): Boolean {
        return bottom >= row.top && top <= row.bottom
    }

    private fun PdfTextBlock.isInColumn(header: PdfTextBlock): Boolean {
        return right >= header.left && left <= header.right
    }

    private fun List<PdfTextBlock>.merge(text: String): PdfTextBlock = PdfTextBlock(
        text = text,
        page = first().page,
        left = minOf(PdfTextBlock::left),
        top = minOf(PdfTextBlock::top),
        right = maxOf(PdfTextBlock::right),
        bottom = maxOf(PdfTextBlock::bottom),
    )

    private fun parsePercentage(value: String): BigDecimal =
        try {
            BigDecimal(value.trim().replace(',', '.'))
        } catch (exception: NumberFormatException) {
            throw Coding4WorldBdiException("Invalid BDI percentage in the PDF: $value", exception)
        }

    private fun normalize(value: String): String = value
        .lowercase()
        .replace('\u00A0', ' ')
        .replace('–', '-')
        .replace('—', '-')
        .replace(WHITESPACE, " ")
        .trim()

    private fun <T> requireSingle(
        values: List<T>,
        missing: String,
        ambiguous: String,
    ): T = when (values.size) {
        0 -> throw Coding4WorldBdiException(missing)
        1 -> values.single()
        else -> throw Coding4WorldBdiException(ambiguous)
    }

    private data class TableRegion(
        val section: PdfTextBlock,
        val top: Float,
        val bottom: Float,
    )

    private data class Marker(
        val block: PdfTextBlock,
        val type: TaxRelief,
    )

    private enum class TaxRelief {
        WITH,
        WITHOUT,
    }

    private companion object {
        const val CONSERVATION_SECTION = "road maintenance"
        const val WITHOUT_TAX_RELIEF = "without tax relief"
        const val WITH_TAX_RELIEF = "with tax relief"
        const val PERCENTAGE_OVER_DIRECT_COST = "% over dc"
        const val TOTAL_BDI_ROW = "total - bdi (%)"
        const val MAX_PHRASE_BLOCKS = 5
        const val PHRASE_LENGTH_TOLERANCE = 12

        val DECIMAL = Regex("\\d{1,3}[.,]\\d{1,2}")
        val WHITESPACE = Regex("\\s+")
        val MAX_PERCENTAGE = BigDecimal("100")
    }
}
