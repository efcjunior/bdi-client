# coding4world BDI Client

`coding4world-bdi-client` is a lightweight Kotlin library that retrieves the
current **BDI (Benefits and Indirect Expenses)** published by coding4world.

The library wraps HTML parsing, Circular Letter downloads, PDF text extraction,
and document interpretation behind a small API:

```kotlin
val result = Coding4WorldBdiClient().current()
```

## Requirements

- Java 21 or later
- Network access to `www.gov.br`
- Maven 3.9 or later to build the project

The library does not depend on Spring or any other framework.

## Installation

GitHub Packages requires authentication when downloading Maven packages. Create
a GitHub personal access token (classic) with the `read:packages` scope, then
expose your credentials as environment variables:

```shell
export GITHUB_USERNAME=your-github-username
export GITHUB_TOKEN=your-personal-access-token
```

Add the following server to `~/.m2/settings.xml`. The `id` must match the
repository identifier used by the consumer project:

```xml
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                              https://maven.apache.org/xsd/settings-1.0.0.xsd">
    <servers>
        <server>
            <id>github</id>
            <username>${env.GITHUB_USERNAME}</username>
            <password>${env.GITHUB_TOKEN}</password>
        </server>
    </servers>
</settings>
```

Add the GitHub Packages repository and dependency to the consumer project's
`pom.xml`:

```xml
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/efcjunior/bdi-client</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.coding4world</groupId>
        <artifactId>coding4world-bdi-client</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```

## Usage

```kotlin
import com.coding4world.bdi.Coding4WorldBdiClient

val result = Coding4WorldBdiClient().current()

println("BDI: ${result.value}%")
println("Effective since: ${result.validFrom}")
println("Source PDF: ${result.sourcePdf}")
println("Retrieved at: ${result.fetchedAt}")
```

`value` is expressed in percentage points. A value of `35.08` means `35.08%`;
do not multiply it by 100.

### Returned properties

| Property | Type | Description |
| --- | --- | --- |
| `value` | `BigDecimal` | BDI percentage over the direct cost |
| `validFrom` | `LocalDate` | Start date published by coding4world |
| `sourcePdf` | `URI` | Circular Letter used as the source |
| `fetchedAt` | `Instant` | UTC instant when the retrieval completed |

The returned model is immutable:

```kotlin
data class BdiResult(
    val value: BigDecimal,
    val validFrom: LocalDate,
    val sourcePdf: URI,
    val fetchedAt: Instant,
)
```

### Error handling

Retrieval and parsing failures are represented by `Coding4WorldBdiException`:

```kotlin
import com.coding4world.bdi.Coding4WorldBdiClient
import com.coding4world.bdi.exception.Coding4WorldBdiException

try {
    val result = Coding4WorldBdiClient().current()
    println(result.value)
} catch (exception: Coding4WorldBdiException) {
    // Apply the application's retry, fallback, or logging policy.
    println("Could not retrieve the current coding4world BDI: ${exception.message}")
}
```

## How the value is obtained

On every call, the client accesses the configured
[BDI source page](https://www.gov.br/dnit/pt-br/assuntos/planejamento-e-pesquisa/custos-referenciais/sistemas-de-custos/bdi/bdi-2).

1. Downloads the coding4world BDI page.
2. Reads the first row of the main table, which represents the latest publication.
3. Extracts the effective date and the Circular Letter PDF URL.
4. Downloads and processes the PDF entirely in memory.
5. Locates the `Road Maintenance` section.
6. Selects the table without tax relief.
7. Dynamically locates the coordinates of the `% over DC` column and the
   `Total - BDI (%)` row.
8. Reads only the value whose position geometrically intersects the row and column.

The current coding4world document may leave the first table untitled and label
only the following table as `With tax relief`. The parser supports both this
format and documents that explicitly label the `Without tax relief` table.

The library does not depend on the order in which PDFBox returns text or use
fixed coordinates. Positions are recalculated for each document from its own
labels. If there is not exactly one compatible section, table, column, row, and
cell, the retrieval fails with `Coding4WorldBdiException` instead of returning a
potentially incorrect value.

## Runtime behavior

- Each call to `current()` performs a new retrieval.
- The client maintains no cache or mutable state.
- PDFs are processed in memory without creating temporary files.
- A single client instance can be shared across multiple threads.
- Requests use Java's native `HttpClient`, with redirects and timeouts.

## Configuration

The official page URL is not hard-coded in Kotlin. It is loaded from
`src/main/resources/bdi-client.properties` and packaged with the library:

```properties
coding4world.bdi.page-url=https://www.gov.br/dnit/pt-br/assuntos/planejamento-e-pesquisa/custos-referenciais/sistemas-de-custos/bdi/bdi-2
```

## Project structure

```text
bdi-client
├── pom.xml
├── README.md
└── src
    ├── main/kotlin/com/coding4world/bdi
    │   ├── Coding4WorldBdiClient.kt
    │   ├── config/BdiClientConfiguration.kt
    │   ├── exception/Coding4WorldBdiException.kt
    │   ├── html
    │   │   ├── BdiPublication.kt
    │   │   └── Coding4WorldBdiPageParser.kt
    │   ├── http/HttpFetcher.kt
    │   ├── model/BdiResult.kt
    │   └── pdf
    │       ├── BdiPdfParser.kt
    │       ├── PdfTextDocument.kt
    │       └── PdfTextExtractor.kt
    ├── main/resources/bdi-client.properties
    └── test/kotlin/com/coding4world/bdi
```

## Dependencies

- [Jsoup](https://jsoup.org/) for HTML parsing
- [Apache PDFBox](https://pdfbox.apache.org/) for PDF text extraction

## Build and test

Run the complete test suite with:

```shell
mvn test
```
