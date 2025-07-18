package com.example.progetto_tosa.ui.account

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix

object QrUtils {
    /**
     * Genera un Drawable contenente un QR code per la stringa 'text'.
     * @param context per accedere a resources
     * @param text contenuto del QR (es. UID)
     * @param size dimensione in pixel del lato del QR
     */
    fun generateQrDrawable(
        context: Context,
        text: String,
        size: Int = 512
    ): Drawable {
        // 1) crea la matrice di bit
        val bitMatrix: BitMatrix = MultiFormatWriter()
            .encode(text, BarcodeFormat.QR_CODE, size, size)

        // 2) converti in array di pixel
        val width = bitMatrix.width
        val height = bitMatrix.height
        val pixels = IntArray(width * height) { index ->
            val x = index % width
            val y = index / width
            if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
        }

        // 3) crea bitmap e disegna i pixel
        val bmp = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)

        // 4) restituisci come Drawable
        return BitmapDrawable(context.resources, bmp)
    }
}
