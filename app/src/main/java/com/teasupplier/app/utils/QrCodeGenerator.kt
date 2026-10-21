package com.teasupplier.app.utils

/**
 * Project: Hotel Rudra Billing & Management System
 * Author & Programmer: Pavan Pawar
 * Description: High-speed QR Code Generator encoding standard NPCI UPI payment deep-links
 * into scannable Bitmap images for bill receipts.
 */

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.Locale

object QrCodeGenerator {

    /**
     * Builds a standard NPCI UPI payment deep-link URI and renders it as a QR code bitmap.
     * Format: upi://pay?pa=<UPI_ID>&pn=<PAYEE_NAME>&am=<AMOUNT>&cu=INR&tn=<NOTE>
     */
    fun generateUpiPaymentQr(
        upiId: String,
        payeeName: String = "Hotel Rudra Amruttulya",
        amount: Double = 0.0,
        note: String = "Bill Payment",
        sizePx: Int = 260
    ): Bitmap? {
        val cleanUpi = upiId.trim().ifBlank { "8788138197@ybl" }
        val encodedName = android.net.Uri.encode(payeeName.trim().ifBlank { "Hotel Rudra Amruttulya" })
        val encodedNote = android.net.Uri.encode(note.trim().ifBlank { "Bill Payment" })
        val amountStr = String.format(Locale.US, "%.2f", amount)

        val upiUri = if (amount > 0) {
            "upi://pay?pa=$cleanUpi&pn=$encodedName&am=$amountStr&cu=INR&tn=$encodedNote"
        } else {
            "upi://pay?pa=$cleanUpi&pn=$encodedName&cu=INR&tn=$encodedNote"
        }

        return generateQrBitmap(upiUri, sizePx)
    }

    /**
     * Encodes arbitrary text into a high quality QR Code Bitmap.
     */
    fun generateQrBitmap(content: String, sizePx: Int = 260): Bitmap? {
        return try {
            val hints = hashMapOf<EncodeHintType, Any>(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.MARGIN to 1,
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M
            )
            val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE
                }
            }
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
