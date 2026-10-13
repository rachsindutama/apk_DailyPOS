package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "promotions")
data class PromotionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    // Promo Scheme: FLASH_SALE, TIERED, PWP, BUNDLING, DAY_SPECIFIC, CATEGORY
    val promoScheme: String = "CATEGORY",
    val type: String = "PERCENTAGE", // PERCENTAGE, NOMINAL
    val value: Double = 0.0,
    val targetType: String = "SPECIFIC_PRODUCT", // ALL_PRODUCTS, SPECIFIC_PRODUCT, SPECIFIC_CATEGORY
    val targetId: Long = 0L,
    val targetName: String = "",
    val minPurchaseAmount: Double = 0.0,
    val isActive: Boolean = true,
    val note: String = "",
    // Tiered discount
    val minQuantity: Int = 1,
    // Flash sale active hours (0 - 24)
    val startHour: Int = 0,
    val endHour: Int = 24,
    // Day-specific promo (e.g. "JUMAT", "SABTU,MINGGU", "SENIN")
    val activeDays: String = "",
    // Tiered rules: format "50000:10,100000:20,150000:30" (minSpend:discountPercent)
    val tierRules: String = "",
    // Multi-products support (comma-separated IDs & names for Bundling, Tebus Murah, Hari Tertentu)
    val multiProductIds: String = "",
    val multiProductNames: String = "",
    // PWP (Tebus Murah) & Bundling
    val triggerProductId: Long = 0L,
    val triggerProductName: String = "",
    val triggerMinQty: Int = 1,
    val rewardProductId: Long = 0L,
    val rewardProductName: String = "",
    val rewardSpecialPrice: Double = 0.0
)
