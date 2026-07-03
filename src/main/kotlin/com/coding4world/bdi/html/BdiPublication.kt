package com.coding4world.bdi.html

import java.net.URI
import java.time.LocalDate

internal data class BdiPublication(
    val validFrom: LocalDate,
    val sourcePdf: URI,
)
