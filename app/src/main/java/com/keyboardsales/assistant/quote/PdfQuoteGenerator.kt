package com.keyboardsales.assistant.quote

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.keyboardsales.vitrina.insert.PriceFormatter
import helium314.keyboard.latin.R
import java.io.File
import java.io.FileOutputStream

/**
 * Genera la cotización en PDF con [android.graphics.pdf.PdfDocument] nativo.
 *
 * Elección (decidida con Santi, 2026-08-19): PdfDocument del framework por sobre
 * iText. Motivos: cero dependencia (el presupuesto de peso es <25 MB y el de
 * solo-teclado 12-15 MB), sin conflicto de licencia (iText es AGPL, incompatible
 * con el fork GPL-3.0-only), y el layout "simple con logo" es de una página,
 * totalmente cubierto por dibujo manual.
 *
 * Las medidas acá están en PUNTOS (espacio del documento A4, 595x842 pt), no en
 * dp ni en tokens de pantalla: es tipografía de un documento imprimible, no UI
 * del teclado. Los textos de usuario salen de strings.xml (regla 11).
 *
 * Se escribe a un único archivo fijo (se sobreescribe) para no acumular PDFs en
 * el dispositivo: el documento vive solo local y se comparte directo.
 */
object PdfQuoteGenerator {

    private const val PAGE_WIDTH = 595f
    private const val PAGE_HEIGHT = 842f
    private const val MARGIN = 48f
    private const val CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN

    private const val QUOTES_DIR = "quotes"
    private const val PDF_NAME = "cotizacion.pdf"

    // Columnas de la tabla (desde el borde derecho hacia la izquierda).
    private const val SUBTOTAL_WIDTH = 100f
    private const val UNIT_WIDTH = 100f
    private const val QTY_WIDTH = 55f
    private const val COLUMN_GAP = 12f

    private const val LINE_HEIGHT = 16f
    private const val ROW_PADDING = 10f

    fun generate(context: Context, quote: Quote): File {
        val dir = File(context.filesDir, QUOTES_DIR)
        dir.mkdirs()
        val file = File(dir, PDF_NAME)

        val titlePaint = paint(Typeface.BOLD, 20f)
        val tenantPaint = paint(Typeface.BOLD, 22f)
        val bodyPaint = paint(Typeface.NORMAL, 12f)
        val boldBodyPaint = paint(Typeface.BOLD, 12f)
        val totalPaint = paint(Typeface.BOLD, 14f)
        val smallPaint = paint(Typeface.NORMAL, 10f, Color.GRAY)
        val rulePaint = paint(Typeface.NORMAL, 1f).apply { strokeWidth = 1f }

        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH.toInt(), PAGE_HEIGHT.toInt(), 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas

        val right = MARGIN + CONTENT_WIDTH
        val xSubtotal = right
        val xUnit = right - SUBTOTAL_WIDTH
        val xQty = right - SUBTOTAL_WIDTH - UNIT_WIDTH
        val nameRight = xQty - COLUMN_GAP

        var y = MARGIN + 22f

        // Header: tenant (logo placeholder de texto) a la izquierda, título y fecha a la derecha.
        canvas.drawText(quote.tenant, MARGIN, y, tenantPaint)
        drawRight(canvas, context.getString(R.string.quote_title), right, y, titlePaint)
        y += LINE_HEIGHT
        drawRight(canvas, quote.fecha, right, y, smallPaint)

        y += ROW_PADDING
        drawRule(canvas, y, rulePaint)
        y += ROW_PADDING + LINE_HEIGHT

        quote.cliente?.let {
            canvas.drawText(
                "${context.getString(R.string.quote_customer)}: $it",
                MARGIN,
                y,
                bodyPaint,
            )
            y += LINE_HEIGHT + ROW_PADDING
        }

        // Encabezado de la tabla.
        canvas.drawText(context.getString(R.string.quote_product), MARGIN, y, boldBodyPaint)
        drawRight(canvas, context.getString(R.string.quote_quantity), xQty, y, boldBodyPaint)
        drawRight(canvas, context.getString(R.string.quote_unit_price), xUnit, y, boldBodyPaint)
        drawRight(canvas, context.getString(R.string.quote_subtotal), xSubtotal, y, boldBodyPaint)
        y += ROW_PADDING
        drawRule(canvas, y, rulePaint)
        y += ROW_PADDING + LINE_HEIGHT

        // Filas de ítems: el nombre puede envolver, las celdas numéricas van al primer renglón.
        for (item in quote.items) {
            val nameLines = wrap(item.nombre, nameRight - MARGIN, bodyPaint)
            nameLines.forEachIndexed { index, line ->
                canvas.drawText(line, MARGIN, y + index * LINE_HEIGHT, bodyPaint)
            }
            drawRight(canvas, item.cantidad.toString(), xQty, y, bodyPaint)
            drawRight(canvas, PriceFormatter.formatNumber(item.precioUnitario), xUnit, y, bodyPaint)
            drawRight(canvas, PriceFormatter.formatNumber(item.subtotal()), xSubtotal, y, bodyPaint)
            y += nameLines.size * LINE_HEIGHT + ROW_PADDING
        }

        drawRule(canvas, y, rulePaint)
        y += ROW_PADDING + LINE_HEIGHT

        drawRight(
            canvas,
            "${context.getString(R.string.quote_total)}: ${PriceFormatter.format(quote.total(), quote.moneda)}",
            right,
            y,
            totalPaint,
        )

        // Footer: quién lo generó, en el pie de la página.
        val footer = context.getString(R.string.quote_generated_by, quote.tenant)
        canvas.drawText(footer, MARGIN, PAGE_HEIGHT - MARGIN, smallPaint)

        doc.finishPage(page)
        doc.writeTo(FileOutputStream(file))
        doc.close()
        return file
    }

    private fun paint(style: Int, size: Float, color: Int = Color.BLACK): Paint = Paint().apply {
        isAntiAlias = true
        typeface = if (style == Typeface.BOLD) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
        textSize = size
        this.color = color
    }

    private fun drawRight(canvas: Canvas, text: String, xRight: Float, y: Float, paint: Paint) {
        val previous = paint.textAlign
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(text, xRight, y, paint)
        paint.textAlign = previous
    }

    /** Divide [text] en líneas que no superen [maxWidth]. */
    private fun wrap(text: String, maxWidth: Float, paint: Paint): List<String> {
        val lines = mutableListOf<String>()
        var remaining = text
        while (remaining.isNotEmpty()) {
            val count = paint.breakText(remaining, true, maxWidth, null).coerceAtLeast(1)
            lines.add(remaining.substring(0, count))
            remaining = remaining.substring(count)
        }
        return lines.ifEmpty { listOf("") }
    }

    private fun drawRule(canvas: Canvas, y: Float, paint: Paint) {
        canvas.drawLine(MARGIN, y, MARGIN + CONTENT_WIDTH, y, paint)
    }
}
