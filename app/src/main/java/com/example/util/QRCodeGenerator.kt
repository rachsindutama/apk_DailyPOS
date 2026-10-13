package com.example.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.EnumMap

object QRCodeGenerator {

    /**
     * Generates a QR Code Bitmap from given string payload.
     */
    fun generateQRCodeBitmap(content: String, width: Int = 512, height: Int = 512): Bitmap? {
        return try {
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
                put(EncodeHintType.MARGIN, 1)
                put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M)
            }

            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height, hints)
            val matrixWidth = bitMatrix.width
            val matrixHeight = bitMatrix.height
            val bitmap = Bitmap.createBitmap(matrixWidth, matrixHeight, Bitmap.Config.ARGB_8888)

            for (x in 0 until matrixWidth) {
                for (y in 0 until matrixHeight) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Generates a standard Indonesian QRIS compatible payload with dynamic transaction amount.
     */
    fun buildQrisPayload(
        merchantName: String,
        nmid: String,
        amount: Double,
        invoiceNumber: String
    ): String {
        val cleanMerchant = merchantName.trim().take(25).uppercase()
        val intAmount = amount.toLong().toString()

        // Standard EMVCo QRIS format assembly
        val sb = StringBuilder()
        sb.append("000201") // Format Indicator
        sb.append("010212") // Point of Initiation Method: 12 (Dynamic QR)

        // Merchant Account Information (Tag 26 - QRIS National)
        val mInfo = "0016ID.CO.QRIS.WWW0118$nmid"
        sb.append("26").append(String.format("%02d", mInfo.length)).append(mInfo)

        sb.append("52045411") // Merchant Category Code (Grocery Stores / Supermarkets)
        sb.append("5303360")  // Currency IDR (360)
        
        // Transaction Amount (Tag 54)
        if (amount > 0) {
            sb.append("54").append(String.format("%02d", intAmount.length)).append(intAmount)
        }

        sb.append("5802ID") // Country Code ID

        // Merchant Name (Tag 59)
        sb.append("59").append(String.format("%02d", cleanMerchant.length)).append(cleanMerchant)

        // Merchant City (Tag 60)
        val city = "INDONESIA"
        sb.append("60").append(String.format("%02d", city.length)).append(city)

        // Additional Data (Tag 62) - Invoice Ref
        val invoiceRef = invoiceNumber.take(20)
        val addData = "01${String.format("%02d", invoiceRef.length)}$invoiceRef"
        sb.append("62").append(String.format("%02d", addData.length)).append(addData)

        // Tag 63: CRC16 Checksum
        val rawWithoutCrc = sb.toString() + "6304"
        val crc = calculateCrc16(rawWithoutCrc)
        return rawWithoutCrc + crc
    }

    private fun calculateCrc16(input: String): String {
        var crc = 0xFFFF
        val polynomial = 0x1021
        val bytes = input.toByteArray(Charsets.ISO_8859_1)

        for (b in bytes) {
            for (i in 0 until 8) {
                val bit = (b.toInt() shr (7 - i) and 1) == 1
                val c15 = (crc shr 15 and 1) == 1
                crc = crc shl 1
                if (c15 xor bit) crc = crc xor polynomial
            }
        }
        crc = crc and 0xFFFF
        return String.format("%04X", crc)
    }
}
