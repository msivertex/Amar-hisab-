package com.example.data.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.File
import java.io.FileOutputStream

/**
 * Generates standard ISO/IEC 18004 UTF-8 scannable QR code Bitmaps and shareable PNG QR receipt cards.
 * Supports full Unicode text across all 32 app languages (Bangla, English, Arabic, Hindi, Urdu, Chinese, etc.).
 */
object QrCodeBitmapGenerator {

    /**
     * Generates a crisp, scannable QR code [Bitmap] for any UTF-8 [content].
     */
    fun generateQrBitmap(
        content: String,
        sizePx: Int = 512,
        fgColor: Int = Color.parseColor("#0F172A"),
        bgColor: Int = Color.WHITE
    ): Bitmap {
        val safeContent = content.trim().ifBlank { "Amar Hisab Verification" }
        val safeSize = sizePx.coerceIn(128, 1024)
        return runCatching {
            val hints = mapOf(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN to 1
            )
            val bitMatrix = QRCodeWriter().encode(
                safeContent,
                BarcodeFormat.QR_CODE,
                safeSize,
                safeSize,
                hints
            )
            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (bitMatrix[x, y]) fgColor else bgColor
                }
            }
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                setPixels(pixels, 0, width, 0, 0, width, height)
            }
        }.getOrElse {
            // Safe white placeholder fallback if anything unexpected occurs
            Bitmap.createBitmap(safeSize, safeSize, Bitmap.Config.ARGB_8888).apply {
                eraseColor(bgColor)
            }
        }
    }

    /**
     * Renders a branded, localized, scannable QR Receipt PNG card directly into [outFile].
     */
    fun renderQrReceiptPngToFile(
        outFile: File,
        qrPayload: String,
        brandHeader: String,
        reportTitle: String,
        periodLabel: String,
        summaryLine: String,
        verificationCaption: String,
        languageBadge: String
    ): File {
        outFile.parentFile?.mkdirs()
        val width = 900
        val height = 1120
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Crisp background & outer card
        canvas.drawColor(Color.parseColor("#F1F5F9"))
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        val cardRect = RectF(44f, 44f, width - 44f, height - 44f)
        canvas.drawRoundRect(cardRect, 36f, 36f, cardPaint)
        canvas.drawRoundRect(cardRect, 36f, 36f, borderPaint)

        // Brand header bar
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#0284C7") }
        val headerRect = RectF(44f, 44f, width - 44f, 224f)
        canvas.drawRoundRect(headerRect, 36f, 36f, headerPaint)
        canvas.drawRect(44f, 180f, width - 44f, 224f, headerPaint)

        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 40f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E0F2FE")
            textSize = 28f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(brandHeader, width / 2f, 118f, brandPaint)
        canvas.drawText(reportTitle, width / 2f, 168f, titlePaint)

        // Period + Language badge
        val periodPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 28f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("$periodLabel  •  $languageBadge", width / 2f, 290f, periodPaint)

        // Centered scannable QR code with subtle frame
        val qrSize = 500
        val qrBitmap = generateQrBitmap(qrPayload, sizePx = qrSize)
        val qrLeft = (width - qrSize) / 2f
        val qrTop = 330f
        val qrFrame = RectF(qrLeft - 20f, qrTop - 20f, qrLeft + qrSize + 20f, qrTop + qrSize + 20f)
        canvas.drawRoundRect(qrFrame, 24f, 24f, borderPaint)
        canvas.drawBitmap(qrBitmap, qrLeft, qrTop, null)
        qrBitmap.recycle()

        // Summary + Verification caption
        val summaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 24f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val captionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#475569")
            textSize = 21f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(summaryLine.take(58), width / 2f, 920f, summaryPaint)
        canvas.drawText(verificationCaption.take(66), width / 2f, 976f, captionPaint)

        FileOutputStream(outFile).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        }
        bitmap.recycle()
        return outFile
    }
}
