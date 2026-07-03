package com.coding4world.bdi.pdf

internal data class PdfTextDocument(
    val blocks: List<PdfTextBlock>,
)

internal data class PdfTextBlock(
    val text: String,
    val page: Int,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
    val width: Float get() = right - left
    val height: Float get() = bottom - top
}
