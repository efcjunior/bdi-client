package com.coding4world.bdi.html

import com.coding4world.bdi.exception.Coding4WorldBdiException
import java.net.URI
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class Coding4WorldBdiPageParserTest {
    private val parser = Coding4WorldBdiPageParser()
    private val pageUri = URI("https://www.coding4world.com/bdi/")

    @Test
    fun `reads the first publication from the BDI table`() {
        val result = parser.parse(PAGE_HTML, pageUri)

        assertEquals(LocalDate.of(2026, 6, 28), result.validFrom)
        assertEquals(
            URI("https://www.coding4world.com/bdi/files/current-bdi.pdf"),
            result.sourcePdf,
        )
    }

    @Test
    fun `accepts a numeric effective date`() {
        val html = PAGE_HTML.replace("Since June 28, 2026", "06/28/2026")

        assertEquals(LocalDate.of(2026, 6, 28), parser.parse(html, pageUri).validFrom)
    }

    @Test
    fun `rejects a page without the expected table`() {
        val exception = assertFailsWith<Coding4WorldBdiException> {
            parser.parse("<html><body><table><tr><th>Other</th></tr></table></body></html>", pageUri)
        }

        assertEquals("Could not locate the BDI table on $pageUri", exception.message)
    }

    private companion object {
        val PAGE_HTML = """
            <html>
              <body>
                <table><tr><th>Unrelated navigation table</th></tr></table>
                <table class="plain">
                  <tbody>
                    <tr>
                      <th>Effective Period</th>
                      <th>Reason for Change</th>
                      <th>BDI</th>
                    </tr>
                    <tr>
                      <td><p>Since June 28, 2026</p></td>
                      <td>279th Committee Meeting</td>
                      <td><a href="files/current-bdi.pdf">Circular Letter No. 6044/2026</a></td>
                    </tr>
                    <tr>
                      <td>May 5, 2026 to June 27, 2026</td>
                      <td>278th Committee Meeting</td>
                      <td><a href="files/previous-bdi.pdf">Circular Letter No. 4078/2026</a></td>
                    </tr>
                  </tbody>
                </table>
              </body>
            </html>
        """.trimIndent()
    }
}
