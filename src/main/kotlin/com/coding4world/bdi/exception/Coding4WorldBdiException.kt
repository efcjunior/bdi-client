package com.coding4world.bdi.exception

/** Base exception for failures while retrieving or interpreting coding4world BDI data. */
open class Coding4WorldBdiException : RuntimeException {
    constructor(message: String) : super(message)

    constructor(message: String, cause: Throwable) : super(message, cause)
}
