package com.example.util

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.nio.ByteBuffer

class BarcodeAnalyzer(
    private val onBarcodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val reader = MultiFormatReader().apply {
        val hints = mapOf(
            DecodeHintType.POSSIBLE_FORMATS to listOf(
                BarcodeFormat.QR_CODE,
                BarcodeFormat.EAN_13,
                BarcodeFormat.EAN_8,
                BarcodeFormat.CODE_128,
                BarcodeFormat.CODE_39,
                BarcodeFormat.UPC_A,
                BarcodeFormat.UPC_E,
                BarcodeFormat.DATA_MATRIX,
                BarcodeFormat.ITF
            ),
            DecodeHintType.TRY_HARDER to true,
            DecodeHintType.CHARACTER_SET to "UTF-8"
        )
        setHints(hints)
    }

    private var lastScannedCode = ""
    private var lastScannedTimestamp = 0L

    override fun analyze(image: ImageProxy) {
        val currentTimestamp = System.currentTimeMillis()
        // Quick throttle: allow different codes after 500ms, repeat same code after 1400ms
        if (currentTimestamp - lastScannedTimestamp < 400) {
            image.close()
            return
        }

        try {
            val yPlane = image.planes[0]
            val buffer: ByteBuffer = yPlane.buffer
            val rowStride = yPlane.rowStride
            val pixelStride = yPlane.pixelStride
            val width = image.width
            val height = image.height

            // Handle row stride correctly to avoid horizontal skew distortion
            val data = if (rowStride == width && pixelStride == 1) {
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                bytes
            } else {
                val bytes = ByteArray(width * height)
                val rowBuffer = ByteArray(rowStride)
                buffer.rewind()
                for (row in 0 until height) {
                    val readLength = minOf(rowStride, buffer.remaining())
                    buffer.get(rowBuffer, 0, readLength)
                    System.arraycopy(rowBuffer, 0, bytes, row * width, width)
                }
                bytes
            }

            // Handle rotation for portrait viewfinder
            val rotationDegrees = image.imageInfo.rotationDegrees
            val (rotatedData, finalWidth, finalHeight) = if (rotationDegrees == 90 || rotationDegrees == 270) {
                val rotated = ByteArray(width * height)
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        rotated[x * height + (height - y - 1)] = data[x + y * width]
                    }
                }
                Triple(rotated, height, width)
            } else {
                Triple(data, width, height)
            }

            val source = PlanarYUVLuminanceSource(
                rotatedData, finalWidth, finalHeight, 0, 0, finalWidth, finalHeight, false
            )
            val bitmap = BinaryBitmap(HybridBinarizer(source))
            val result = reader.decodeWithState(bitmap)
            val barcodeText = result.text?.trim() ?: ""

            if (barcodeText.isNotBlank()) {
                val isSameCode = barcodeText == lastScannedCode
                val isDebounced = currentTimestamp - lastScannedTimestamp > 1400

                if (!isSameCode || isDebounced) {
                    lastScannedCode = barcodeText
                    lastScannedTimestamp = currentTimestamp
                    onBarcodeDetected(barcodeText)
                }
            }
        } catch (_: Exception) {
            // No barcode recognized in current frame
        } finally {
            reader.reset()
            image.close()
        }
    }
}

