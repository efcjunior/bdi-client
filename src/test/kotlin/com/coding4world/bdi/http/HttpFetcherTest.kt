package com.coding4world.bdi.http

import com.coding4world.bdi.exception.Coding4WorldBdiException
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.net.URI
import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HttpFetcherTest {
    @Test
    fun `fetches text and bytes without using temporary files`() = withServer { baseUri ->
        val fetcher = HttpFetcher()

        assertEquals("coding4world content", fetcher.fetchText(baseUri.resolve("content")))
        assertContentEquals(
            "coding4world content".toByteArray(StandardCharsets.UTF_8),
            fetcher.fetchBytes(baseUri.resolve("content")),
        )
    }

    @Test
    fun `reports non-successful status codes`() = withServer { baseUri ->
        val exception = assertFailsWith<Coding4WorldBdiException> {
            HttpFetcher().fetchText(baseUri.resolve("missing"))
        }

        assertEquals(
            "Unexpected HTTP status 404 while retrieving ${baseUri.resolve("missing")}",
            exception.message,
        )
    }

    private fun withServer(test: (URI) -> Unit) {
        val body = "coding4world content".toByteArray(StandardCharsets.UTF_8)
        val server = HttpServer.create(InetSocketAddress("localhost", 0), 0).apply {
            createContext("/content") { exchange ->
                exchange.responseHeaders.add("Content-Type", "text/plain; charset=UTF-8")
                exchange.sendResponseHeaders(200, body.size.toLong())
                exchange.responseBody.use { it.write(body) }
            }
            createContext("/missing") { exchange ->
                exchange.sendResponseHeaders(404, -1)
                exchange.close()
            }
            start()
        }

        try {
            test(URI("http://localhost:${server.address.port}/"))
        } finally {
            server.stop(0)
        }
    }
}
