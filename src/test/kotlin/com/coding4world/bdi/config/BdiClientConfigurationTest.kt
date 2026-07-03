package com.coding4world.bdi.config

import com.coding4world.bdi.exception.Coding4WorldBdiException
import java.io.ByteArrayInputStream
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BdiClientConfigurationTest {
    @Test
    fun `loads the coding4world page URI from the packaged resource`() {
        assertEquals(
            URI(
                "https://www.gov.br/dnit/pt-br/assuntos/planejamento-e-pesquisa/" +
                    "custos-referenciais/sistemas-de-custos/bdi/bdi-2",
            ),
            BdiClientConfiguration.bdiPageUri,
        )
    }

    @Test
    fun `reports a missing configuration resource`() {
        val exception = assertFailsWith<Coding4WorldBdiException> {
            BdiClientConfiguration.load { null }
        }

        assertEquals(
            "Configuration resource 'bdi-client.properties' was not found",
            exception.message,
        )
    }

    @Test
    fun `rejects a non-http address`() {
        val content = "coding4world.bdi.page-url=file:///tmp/bdi.html"

        val exception = assertFailsWith<Coding4WorldBdiException> {
            BdiClientConfiguration.load {
                ByteArrayInputStream(content.toByteArray(Charsets.ISO_8859_1))
            }
        }

        assertEquals(
            "Property 'coding4world.bdi.page-url' must use HTTP or HTTPS",
            exception.message,
        )
    }
}
