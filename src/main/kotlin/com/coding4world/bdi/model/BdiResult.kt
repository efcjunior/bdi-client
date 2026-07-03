package com.coding4world.bdi.model

import java.math.BigDecimal
import java.net.URI
import java.time.Instant
import java.time.LocalDate

/**
 * The BDI currently published by coding4world.
 *
 * [value] is represented as a percentage (for example, `35.08` means 35.08%).
 */
data class BdiResult(
    val value: BigDecimal,
    val validFrom: LocalDate,
    val sourcePdf: URI,
    val fetchedAt: Instant,
)
