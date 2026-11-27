package com.example.util

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageFormat
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Generates a crisp offline QR Code Bitmap for customer loyalty cards (e.g. PRF-1042).
 */
fun generateQrBitmap(
    content: String,
    sizePx: Int = 512,
    foregroundColor: Int = Color.parseColor("#111118"),
    backgroundColor: Int = Color.parseColor("#FAF6EE")
): Bitmap {
    val hints = mapOf(
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
        EncodeHintType.MARGIN to 1,
        EncodeHintType.CHARACTER_SET to "UTF-8"
    )
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
    val pixels = IntArray(sizePx * sizePx)
    for (y in 0 until sizePx) {
        val rowOffset = y * sizePx
        for (x in 0 until sizePx) {
            pixels[rowOffset + x] = if (matrix[x, y]) foregroundColor else backgroundColor
        }
    }
    return Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888).apply {
        setPixels(pixels, 0, sizePx, 0, 0, sizePx, sizePx)
    }
}

/**
 * Pure offline CameraX ImageAnalysis.Analyzer decoding QR codes and retail barcodes via ZXing.
 */
class QrBarcodeAnalyzer(
    private val onCodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val reader = MultiFormatReader().apply {
        setHints(
            mapOf(
                DecodeHintType.POSSIBLE_FORMATS to listOf(
                    BarcodeFormat.QR_CODE,
                    BarcodeFormat.CODE_128,
                    BarcodeFormat.CODE_39,
                    BarcodeFormat.EAN_13,
                    BarcodeFormat.EAN_8,
                    BarcodeFormat.UPC_A
                ),
                DecodeHintType.TRY_HARDER to true
            )
        )
    }

    private var lastAnalyzedTimestamp = 0L

    override fun analyze(image: ImageProxy) {
        val now = System.currentTimeMillis()
        if (now - lastAnalyzedTimestamp < 250L) {
            image.close()
            return
        }
        lastAnalyzedTimestamp = now

        try {
            if (image.format == ImageFormat.YUV_420_888 ||
                image.format == ImageFormat.YUV_422_888 ||
                image.format == ImageFormat.YUV_444_888
            ) {
                val plane = image.planes[0]
                val buffer = plane.buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)

                val width = image.width
                val height = image.height
                val source = PlanarYUVLuminanceSource(
                    bytes,
                    plane.rowStride,
                    height,
                    0,
                    0,
                    width,
                    height,
                    false
                )
                val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
                val result = reader.decodeWithState(binaryBitmap)
                val decodedText = result.text?.trim()
                if (!decodedText.isNullOrEmpty()) {
                    onCodeDetected(decodedText)
                }
            }
        } catch (_: Exception) {
            // No QR/barcode in this frame
        } finally {
            reader.reset()
            image.close()
        }
    }
}
