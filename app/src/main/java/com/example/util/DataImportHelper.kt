package com.example.util

import org.json.JSONArray
import org.json.JSONObject

data class ParsedImportProduct(
    val sku: String = "",
    val barcode: String = "",
    val name: String,
    val categoryName: String,
    val buyPrice: Double,
    val sellPrice: Double,
    val minStock: Int = 5,
    val unit: String = "pcs"
)

data class ImportResult(
    val importedProductCount: Int,
    val createdCategoryCount: Int,
    val errors: List<String>
)

object DataImportHelper {

    fun generateSku(categoryName: String = "", sequenceNumber: Int = 0): String {
        val randomDigits = (1000000..9999999).random()
        return "SKU-$randomDigits"
    }

    fun parseInput(content: String): List<ParsedImportProduct> {
        val trimmed = content.trim()
        return if (trimmed.startsWith("[") || (trimmed.startsWith("{") && trimmed.contains("["))) {
            parseJson(trimmed)
        } else {
            parseCsv(trimmed)
        }
    }

    fun parseCsv(csvText: String): List<ParsedImportProduct> {
        val lines = csvText.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.size < 2) return emptyList()

        val headerLine = lines[0]
        val delimiter = when {
            headerLine.contains(";") -> ";"
            headerLine.contains("\t") -> "\t"
            else -> ","
        }

        val rawHeaders = headerLine.split(delimiter).map { normalizeHeader(it) }
        val skuIdx = rawHeaders.indexOfFirst { it in listOf("sku", "kode_sku", "sku_code", "kode_barang", "kode_item", "item_code") }
        val barcodeIdx = rawHeaders.indexOfFirst {
            it in listOf("barcode", "kode_barcode", "ean", "upc", "bar_code") || (skuIdx == -1 && it in listOf("kode", "code"))
        }
        val nameIdx = rawHeaders.indexOfFirst { it in listOf("name", "nama", "nama_produk", "produk", "item", "barang") }
        val catIdx = rawHeaders.indexOfFirst { it in listOf("category", "kategori", "kategori_produk", "jenis", "group") }
        val buyPriceIdx = rawHeaders.indexOfFirst { it in listOf("buyprice", "harga_beli", "beli", "hpp", "modal") }
        val sellPriceIdx = rawHeaders.indexOfFirst { it in listOf("sellprice", "harga_jual", "jual", "harga", "price") }
        val minStockIdx = rawHeaders.indexOfFirst { it in listOf("minstock", "min_stok", "stok_min", "minimum") }
        val unitIdx = rawHeaders.indexOfFirst { it in listOf("unit", "satuan", "uom") }

        if (nameIdx == -1) return emptyList()

        val result = mutableListOf<ParsedImportProduct>()
        val categoryCounters = mutableMapOf<String, Int>()

        for (i in 1 until lines.size) {
            val line = lines[i]
            val cols = line.split(delimiter).map { it.trim().removeSurrounding("\"") }
            if (cols.isEmpty() || cols.all { it.isBlank() }) continue

            val name = cols.getOrNull(nameIdx) ?: continue
            if (name.isBlank()) continue

            val barcode = if (barcodeIdx >= 0) cols.getOrNull(barcodeIdx)?.trim() ?: "" else ""
            val catName = (if (catIdx >= 0) cols.getOrNull(catIdx)?.trim() ?: "Umum" else "Umum").ifBlank { "Umum" }
            val buyPrice = if (buyPriceIdx >= 0) cols.getOrNull(buyPriceIdx)?.toDoubleOrNull() ?: 0.0 else 0.0
            val sellPrice = if (sellPriceIdx >= 0) cols.getOrNull(sellPriceIdx)?.toDoubleOrNull() ?: 0.0 else 0.0
            val minStock = if (minStockIdx >= 0) cols.getOrNull(minStockIdx)?.toIntOrNull() ?: 5 else 5
            val unit = if (unitIdx >= 0) cols.getOrNull(unitIdx)?.ifBlank { "pcs" } ?: "pcs" else "pcs"

            val counter = categoryCounters.getOrPut(catName.lowercase()) { 0 } + 1
            categoryCounters[catName.lowercase()] = counter

            val explicitSku = if (skuIdx >= 0) cols.getOrNull(skuIdx)?.trim() ?: "" else ""
            val finalSku = if (explicitSku.matches(Regex("^SKU-\\d{7}$"))) {
                explicitSku
            } else {
                generateSku()
            }

            val finalBarcode = Ean13Utils.toValidEan13(barcode)

            result.add(
                ParsedImportProduct(
                    sku = finalSku,
                    barcode = finalBarcode,
                    name = name,
                    categoryName = catName,
                    buyPrice = buyPrice,
                    sellPrice = sellPrice,
                    minStock = minStock,
                    unit = unit
                )
            )
        }

        return result
    }

    fun parseJson(jsonText: String): List<ParsedImportProduct> {
        val result = mutableListOf<ParsedImportProduct>()
        val categoryCounters = mutableMapOf<String, Int>()

        try {
            val jsonArray = if (jsonText.startsWith("{")) {
                val obj = JSONObject(jsonText)
                when {
                    obj.has("products") -> obj.getJSONArray("products")
                    obj.has("items") -> obj.getJSONArray("items")
                    obj.has("data") -> obj.getJSONArray("data")
                    else -> return emptyList()
                }
            } else {
                JSONArray(jsonText)
            }

            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                val name = item.optString("name", item.optString("nama", item.optString("nama_produk", "")))
                if (name.isBlank()) continue

                val barcode = item.optString("barcode", item.optString("kode_barcode", item.optString("ean", "")))
                val categoryName = item.optString("category", item.optString("kategori", item.optString("kategori_produk", "Umum"))).ifBlank { "Umum" }
                val buyPrice = item.optDouble("buyPrice", item.optDouble("harga_beli", item.optDouble("hpp", 0.0)))
                val sellPrice = item.optDouble("sellPrice", item.optDouble("harga_jual", item.optDouble("harga", 0.0)))
                val minStock = item.optInt("minStock", item.optInt("min_stok", 5))
                val unit = item.optString("unit", item.optString("satuan", "pcs"))

                val counter = categoryCounters.getOrPut(categoryName.lowercase()) { 0 } + 1
                categoryCounters[categoryName.lowercase()] = counter

                val explicitSku = item.optString("sku", item.optString("sku_code", item.optString("kode_sku", item.optString("kode_barang", ""))))
                val finalSku = if (explicitSku.matches(Regex("^SKU-\\d{7}$"))) {
                    explicitSku
                } else {
                    generateSku()
                }

                val finalBarcode = Ean13Utils.toValidEan13(barcode)

                result.add(
                    ParsedImportProduct(
                        sku = finalSku,
                        barcode = finalBarcode,
                        name = name,
                        categoryName = categoryName,
                        buyPrice = buyPrice,
                        sellPrice = sellPrice,
                        minStock = minStock,
                        unit = unit
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    private fun normalizeHeader(header: String): String {
        return header.trim().lowercase().replace(" ", "_").replace("-", "_")
    }

    fun getSampleCsv(): String {
        return """
            sku,barcode,nama,kategori,harga_beli,harga_jual,min_stok,satuan
            SKU-SEM-0001,8991001000019,Minyak Goreng 2L,Sembako,28000,32000,5,pouch
            SKU-SEM-0002,8991002000018,Gula Pasir 1kg,Sembako,15000,17500,6,kg
            SKU-MIN-0001,8992001000016,Kopi Bubuk 100g,Minuman,6000,8000,10,bungkus
            SKU-MIN-0002,8992002000015,Teh Celup Kotak,Minuman,5500,7000,5,kotak
            SKU-KBS-0001,8993001000013,Sabun Cuci Piring,Kebersihan,9000,11500,4,botol
        """.trimIndent()
    }

    fun getSampleJson(): String {
        return """
            [
              {
                "sku": "SKU-SEM-0001",
                "barcode": "8991001000019",
                "nama": "Minyak Goreng 2L",
                "kategori": "Sembako",
                "harga_beli": 28000,
                "harga_jual": 32000,
                "min_stok": 5,
                "satuan": "pouch"
              },
              {
                "sku": "SKU-MIN-0001",
                "barcode": "8992001000016",
                "nama": "Kopi Bubuk 100g",
                "kategori": "Minuman",
                "harga_beli": 6000,
                "harga_jual": 8000,
                "min_stok": 10,
                "satuan": "bungkus"
              }
            ]
        """.trimIndent()
    }
}
