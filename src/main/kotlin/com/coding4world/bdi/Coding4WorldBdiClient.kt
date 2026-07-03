package com.coding4world.bdi

import com.coding4world.bdi.config.BdiClientConfiguration
import com.coding4world.bdi.html.Coding4WorldBdiPageParser
import com.coding4world.bdi.http.ContentFetcher
import com.coding4world.bdi.http.HttpFetcher
import com.coding4world.bdi.model.BdiResult
import com.coding4world.bdi.pdf.BdiPdfParser
import com.coding4world.bdi.pdf.PdfTextExtractor
import java.net.URI
import java.time.Clock

/** Retrieves the BDI currently published on coding4world's official website. */
class Coding4WorldBdiClient internal constructor(
    private val fetcher: ContentFetcher,
    private val pageParser: Coding4WorldBdiPageParser,
    private val pdfTextExtractor: PdfTextExtractor,
    private val pdfParser: BdiPdfParser,
    private val clock: Clock,
    private val pageUri: URI,
) {
    constructor() : this(
        fetcher = HttpFetcher(),
        pageParser = Coding4WorldBdiPageParser(),
        pdfTextExtractor = PdfTextExtractor(),
        pdfParser = BdiPdfParser(),
        clock = Clock.systemUTC(),
        pageUri = BdiClientConfiguration.bdiPageUri,
    )

    /**
     * Downloads and interprets the current publication.
     *
     * Every invocation performs a fresh lookup; this client holds no mutable state or cache.
     */
    fun current(): BdiResult {
        val pageHtml = fetcher.fetchText(pageUri)
        val publication = pageParser.parse(pageHtml, pageUri)
        val pdfBytes = fetcher.fetchBytes(publication.sourcePdf)
        val pdfDocument = pdfTextExtractor.extract(pdfBytes)

        return BdiResult(
            value = pdfParser.parse(pdfDocument),
            validFrom = publication.validFrom,
            sourcePdf = publication.sourcePdf,
            fetchedAt = clock.instant(),
        )
    }

}
