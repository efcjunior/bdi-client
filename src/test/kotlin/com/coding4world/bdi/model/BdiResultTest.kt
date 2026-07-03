package com.coding4world.bdi.model

import java.math.BigDecimal
import java.net.URI
import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class BdiResultTest {
    @Test
    fun `represents the published percentage and its provenance`() {
        val result = BdiResult(
            value = BigDecimal("35.08"),
            validFrom = LocalDate.of(2026, 6, 28),
            sourcePdf = URI("https://www.coding4world.com/bdi/example.pdf"),
            fetchedAt = Instant.parse("2026-06-28T12:00:00Z"),
        )

        assertEquals(BigDecimal("35.08"), result.value)
        assertEquals(LocalDate.of(2026, 6, 28), result.validFrom)
        assertEquals(URI("https://www.coding4world.com/bdi/example.pdf"), result.sourcePdf)
        assertEquals(Instant.parse("2026-06-28T12:00:00Z"), result.fetchedAt)
    }
}
