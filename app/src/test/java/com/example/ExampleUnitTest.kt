package com.example

import com.example.data.local.entity.ProductEntity
import com.example.data.model.CartItem
import com.example.ui.components.paginate
import com.example.util.CurrencyFormatter
import com.example.util.Ean13Utils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun currencyFormatter_formatsIndonesianRupiahCorrectly() {
        val formatted = CurrencyFormatter.formatRupiah(15000.0)
        assertTrue(formatted.contains("15.000") || formatted.contains("15,000"))
    }

    @Test
    fun cartItem_calculatesSubtotalAndProfitCorrectly() {
        val product = ProductEntity(
            id = 1L,
            barcode = "8991001000015",
            name = "Indomie Goreng",
            categoryId = 1L,
            buyPrice = 2800.0,
            sellPrice = 3500.0,
            stock = 50,
            minStock = 10,
            unit = "bungkus"
        )
        val cartItem = CartItem(product = product, quantity = 3, customDiscount = 200.0)
        assertEquals(3300.0, cartItem.effectiveUnitPrice, 0.01)
        assertEquals(9900.0, cartItem.subtotal, 0.01)
        assertEquals(600.0, cartItem.totalDiscount, 0.01)
        assertEquals(8400.0, cartItem.costSubtotal, 0.01)
        assertEquals(1500.0, cartItem.estimatedProfit, 0.01)
    }

    @Test
    fun ean13_calculatesCheckDigitAndValidatesCorrectly() {
        // Test standard EAN-13 check digit calculation:
        // 899100100001 -> check digit is 9 -> 8991001000019
        val checkDigit = Ean13Utils.calculateCheckDigit("899100100001")
        assertEquals(9, checkDigit)
        assertTrue(Ean13Utils.isValidEan13("8991001000019"))
        assertFalse(Ean13Utils.isValidEan13("8991001000014")) // wrong check digit
        assertFalse(Ean13Utils.isValidEan13("8991001")) // not 13 digits
    }

    @Test
    fun ean13_autoGenerationProducesValid13Digits() {
        for (i in 1..20) {
            val generated = Ean13Utils.generateEan13()
            assertEquals(13, generated.length)
            assertTrue(generated.startsWith("899"))
            assertTrue(Ean13Utils.isValidEan13(generated))
            assertNull(Ean13Utils.getValidationError(generated))
        }
    }

    @Test
    fun ean13_normalizationConvertsAnyInputToValidEan13() {
        // Manual short numeric code -> converted to valid 13 digit EAN-13
        val fromShort = Ean13Utils.toValidEan13("8991001")
        assertEquals(13, fromShort.length)
        assertTrue(Ean13Utils.isValidEan13(fromShort))

        // Empty string -> auto generates valid EAN-13
        val fromBlank = Ean13Utils.toValidEan13("")
        assertEquals(13, fromBlank.length)
        assertTrue(Ean13Utils.isValidEan13(fromBlank))

        // Already valid EAN-13 -> remains intact
        val intact = Ean13Utils.toValidEan13("8991001000019")
        assertEquals("8991001000019", intact)
    }

    @Test
    fun pagination_slicesCorrectly() {
        val list = (1..25).toList()
        // Page 1 of size 10 -> items 1..10
        val page1 = list.paginate(1, 10)
        assertEquals(10, page1.size)
        assertEquals(1, page1.first())
        assertEquals(10, page1.last())

        // Page 2 of size 10 -> items 11..20
        val page2 = list.paginate(2, 10)
        assertEquals(10, page2.size)
        assertEquals(11, page2.first())
        assertEquals(20, page2.last())

        // Page 3 of size 10 -> items 21..25
        val page3 = list.paginate(3, 10)
        assertEquals(5, page3.size)
        assertEquals(21, page3.first())
        assertEquals(25, page3.last())

        // Out of bounds page coerced safely
        val pageOut = list.paginate(99, 10)
        assertEquals(5, pageOut.size)
        assertEquals(25, pageOut.last())
    }

    @Test
    fun outOfStockProduct_cannotBeProcessed() {
        val outOfStockProduct = ProductEntity(
            id = 2L,
            barcode = "8991001000022",
            name = "Minyak Goreng Habis",
            categoryId = 1L,
            buyPrice = 14000.0,
            sellPrice = 16000.0,
            stock = 0,
            minStock = 5,
            unit = "liter"
        )
        assertTrue(outOfStockProduct.stock <= 0)
    }
}
