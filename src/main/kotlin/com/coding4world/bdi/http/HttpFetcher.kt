package com.coding4world.bdi.http

import com.coding4world.bdi.exception.Coding4WorldBdiException
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration

internal interface ContentFetcher {
    fun fetchText(uri: URI): String

    fun fetchBytes(uri: URI): ByteArray
}

internal class HttpFetcher(
    private val httpClient: HttpClient = defaultHttpClient(),
    private val requestTimeout: Duration = Duration.ofSeconds(30),
) : ContentFetcher {
    override fun fetchText(uri: URI): String =
        send(uri, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))

    override fun fetchBytes(uri: URI): ByteArray =
        send(uri, HttpResponse.BodyHandlers.ofByteArray())

    private fun <T> send(uri: URI, bodyHandler: HttpResponse.BodyHandler<T>): T {
        requireHttpUri(uri)

        val request = HttpRequest.newBuilder(uri)
            .timeout(requestTimeout)
            .header("Accept", "text/html,application/pdf;q=0.9,*/*;q=0.8")
            .header("User-Agent", "coding4world-bdi-client/1.0")
            .GET()
            .build()

        val response = try {
            httpClient.send(request, bodyHandler)
        } catch (exception: InterruptedException) {
            Thread.currentThread().interrupt()
            throw Coding4WorldBdiException("Request to $uri was interrupted", exception)
        } catch (exception: IOException) {
            throw Coding4WorldBdiException("Could not retrieve $uri", exception)
        }

        if (response.statusCode() !in 200..299) {
            throw Coding4WorldBdiException(
                "Unexpected HTTP status ${response.statusCode()} while retrieving $uri",
            )
        }

        return response.body()
    }

    private fun requireHttpUri(uri: URI) {
        if (uri.scheme?.lowercase() !in setOf("http", "https")) {
            throw Coding4WorldBdiException("Only HTTP and HTTPS addresses are supported: $uri")
        }
    }

    private companion object {
        fun defaultHttpClient(): HttpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build()
    }
}
