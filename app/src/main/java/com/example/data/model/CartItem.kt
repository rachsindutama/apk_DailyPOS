package com.example.data.model

import com.example.data.local.entity.ProductEntity

data class CartItem(
    val product: ProductEntity,
    val quantity: Int = 1,
    val customDiscount: Double = 0.0,
    val appliedPromoName: String = "",
    val appliedPromoScheme: String = ""
) {
    val unitPrice: Double
        get() = product.sellPrice

    val discountPerItem: Double
        get() = customDiscount

    val effectiveUnitPrice: Double
        get() = (unitPrice - discountPerItem).coerceAtLeast(0.0)

    val subtotal: Double
        get() = effectiveUnitPrice * quantity

    val totalOriginal: Double
        get() = unitPrice * quantity

    val totalDiscount: Double
        get() = discountPerItem * quantity

    val costSubtotal: Double
        get() = product.buyPrice * quantity

    val estimatedProfit: Double
        get() = subtotal - costSubtotal
}
