package com.coding4world.bdi.config

import com.coding4world.bdi.exception.Coding4WorldBdiException
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.util.Properties

internal object BdiClientConfiguration {
    val bdiPageUri: URI by lazy { load() }

    internal fun load(
        resourceLoader: (String) -> InputStream? = BdiClientConfiguration::class.java.classLoader::getResourceAsStream,
    ): URI {
        val stream = resourceLoader(RESOURCE_NAME)
            ?: throw Coding4WorldBdiException("Configuration resource '$RESOURCE_NAME' was not found")
        val properties = Properties()

        try {
            stream.use { properties.load(it) }
        } catch (exception: IOException) {
            throw Coding4WorldBdiException("Could not read configuration resource '$RESOURCE_NAME'", exception)
        }

        val value = properties.getProperty(BDI_PAGE_URL_PROPERTY)?.trim()
            ?.takeIf(String::isNotEmpty)
            ?: throw Coding4WorldBdiException(
                "Required property '$BDI_PAGE_URL_PROPERTY' was not found in '$RESOURCE_NAME'",
            )
        val uri = try {
            URI(value)
        } catch (exception: IllegalArgumentException) {
            throw Coding4WorldBdiException("Invalid URI in property '$BDI_PAGE_URL_PROPERTY': $value", exception)
        }

        if (uri.scheme?.lowercase() !in setOf("http", "https")) {
            throw Coding4WorldBdiException("Property '$BDI_PAGE_URL_PROPERTY' must use HTTP or HTTPS")
        }
        return uri
    }

    private const val RESOURCE_NAME = "bdi-client.properties"
    private const val BDI_PAGE_URL_PROPERTY = "coding4world.bdi.page-url"
}
