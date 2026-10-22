package com.scholr.app.data.scan

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/** A square grid of QR modules; `true` is a dark module. */
class QrMatrix(val size: Int, private val bits: BooleanArray) {
    operator fun get(x: Int, y: Int): Boolean = bits[y * size + x]
}

object QrCode {

    /** Encodes [text] with no quiet zone (callers draw their own margin). Null if it can't fit. */
    fun encode(text: String): QrMatrix? = runCatching {
        // Long payloads (collections) get the lowest error correction so the code stays coarse
        // enough to scan from a phone screen; short ones get a sturdier level.
        val level = if (text.length > 400) ErrorCorrectionLevel.L else ErrorCorrectionLevel.M
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to level,
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.MARGIN to 0
        )
        val m = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 0, 0, hints)
        val bits = BooleanArray(m.width * m.height)
        for (y in 0 until m.height) for (x in 0 until m.width) bits[y * m.width + x] = m[x, y]
        QrMatrix(m.width, bits)
    }.getOrNull()

    /** Renders a shareable bitmap: white card, dark rounded modules, quiet zone included. */
    fun toBitmap(matrix: QrMatrix, px: Int = 1024, darkColor: Int = 0xFF2B2250.toInt()): Bitmap {
        val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(0xFFFFFFFF.toInt())
        val quiet = 4
        val cell = px.toFloat() / (matrix.size + quiet * 2)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = darkColor }
        val rect = RectF()
        val r = cell * 0.22f
        for (y in 0 until matrix.size) for (x in 0 until matrix.size) {
            if (!matrix[x, y]) continue
            rect.set((x + quiet) * cell, (y + quiet) * cell, (x + quiet + 1) * cell, (y + quiet + 1) * cell)
            canvas.drawRoundRect(rect, r, r, paint)
        }
        return bmp
    }
}
