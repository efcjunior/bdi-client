package com.coding4world.bdi.html

import com.coding4world.bdi.exception.Coding4WorldBdiException
import java.net.URI
import java.time.DateTimeException
import java.time.LocalDate
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

internal class Coding4WorldBdiPageParser {
    fun parse(html: String, pageUri: URI): BdiPublication {
        val document = Jsoup.parse(html, pageUri.toString())
        val table = document.select("table").firstOrNull(::isBdiTable)
            ?: throw Coding4WorldBdiException("Could not locate the BDI table on $pageUri")

        val firstRow = table.select("tr").asSequence()
            .drop(1)
            .firstOrNull { directCells(it).size >= EXPECTED_COLUMN_COUNT }
            ?: throw Coding4WorldBdiException("The BDI table on $pageUri has no publication rows")
        val cells = directCells(firstRow)

        return BdiPublication(
            validFrom = parseEffectiveDate(cells[DATE_COLUMN].text()),
            sourcePdf = parsePdfUri(cells[PDF_COLUMN], pageUri),
        )
    }

    private fun isBdiTable(table: Element): Boolean {
        val headings = table.selectFirst("tr")?.let(::directCells) ?: return false
        if (headings.size < EXPECTED_COLUMN_COUNT) return false

        return normalize(headings[DATE_COLUMN].text()).contains("effective period") &&
            normalize(headings[PDF_COLUMN].text()) == "bdi"
    }

    private fun parseEffectiveDate(text: String): LocalDate {
        val normalized = text.replace('\u00A0', ' ').trim()

        TEXT_DATE.find(normalized)?.let { match ->
            val monthName = normalize(match.groupValues[1])
            val month = MONTHS[monthName]
                ?: throw Coding4WorldBdiException("Unknown month in effective date: $text")
            val day = match.groupValues[2].toInt()
            val year = match.groupValues[3].toInt()
            return createDate(year, month, day, text)
        }

        NUMERIC_DATE.find(normalized)?.let { match ->
            val month = match.groupValues[1].toInt()
            val day = match.groupValues[2].toInt()
            val year = match.groupValues[3].toInt()
            return createDate(year, month, day, text)
        }

        throw Coding4WorldBdiException("Could not parse effective date: $text")
    }

    private fun createDate(year: Int, month: Int, day: Int, source: String): LocalDate =
        try {
            LocalDate.of(year, month, day)
        } catch (exception: DateTimeException) {
            throw Coding4WorldBdiException("Invalid effective date: $source", exception)
        }

    private fun parsePdfUri(cell: Element, pageUri: URI): URI {
        val href = cell.selectFirst("a[href]")?.attr("href")?.trim()
            ?.takeIf(String::isNotEmpty)
            ?: throw Coding4WorldBdiException("The current BDI publication has no PDF link")
        val uri = try {
            pageUri.resolve(href)
        } catch (exception: IllegalArgumentException) {
            throw Coding4WorldBdiException("The current BDI publication has an invalid PDF link: $href", exception)
        }

        if (uri.scheme?.lowercase() !in setOf("http", "https")) {
            throw Coding4WorldBdiException("The current BDI publication has an invalid PDF link: $uri")
        }
        return uri
    }

    private fun directCells(row: Element): List<Element> =
        row.children().filter { it.tagName() == "th" || it.tagName() == "td" }

    private fun normalize(value: String): String = value
        .lowercase()
        .replace(WHITESPACE, " ")
        .trim()

    private companion object {
        const val EXPECTED_COLUMN_COUNT = 3
        const val DATE_COLUMN = 0
        const val PDF_COLUMN = 2

        val WHITESPACE = Regex("\\s+")
        val TEXT_DATE = Regex(
            "(?:since\\s+)?([a-z]+)\\s+(\\d{1,2})(?:st|nd|rd|th)?,?\\s+(\\d{4})",
            RegexOption.IGNORE_CASE,
        )
        val NUMERIC_DATE = Regex("(\\d{1,2})/(\\d{1,2})/(\\d{4})")
        val MONTHS = mapOf(
            "january" to 1,
            "february" to 2,
            "march" to 3,
            "april" to 4,
            "may" to 5,
            "june" to 6,
            "july" to 7,
            "august" to 8,
            "september" to 9,
            "october" to 10,
            "november" to 11,
            "december" to 12,
        )
    }
}
