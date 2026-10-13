package com.example.util

import com.example.data.local.entity.PromotionEntity
import com.example.data.model.CartItem
import java.util.Calendar

data class CartPromotionResult(
    val updatedItems: List<CartItem>,
    val totalSavings: Double,
    val appliedPromotions: List<String>
)

object PromotionEngine {

    fun calculateCartPromotions(
        cartItems: List<CartItem>,
        promotions: List<PromotionEntity>,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): CartPromotionResult {
        if (cartItems.isEmpty() || promotions.isEmpty()) {
            return CartPromotionResult(
                updatedItems = cartItems.map { it.copy(customDiscount = 0.0, appliedPromoName = "", appliedPromoScheme = "") },
                totalSavings = 0.0,
                appliedPromotions = emptyList()
            )
        }

        val activePromotions = promotions.filter { it.isActive }
        val cal = Calendar.getInstance().apply { timeInMillis = currentTimeMillis }
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)
        val dayOfWeekInt = cal.get(Calendar.DAY_OF_WEEK)
        val currentDayName = when (dayOfWeekInt) {
            Calendar.SUNDAY -> "MINGGU"
            Calendar.MONDAY -> "SENIN"
            Calendar.TUESDAY -> "SELASA"
            Calendar.WEDNESDAY -> "RABU"
            Calendar.THURSDAY -> "KAMIS"
            Calendar.FRIDAY -> "JUMAT"
            Calendar.SATURDAY -> "SABTU"
            else -> ""
        }

        // Temporary tracking maps
        val itemDiscounts = mutableMapOf<Long, Double>() // productId -> discountPerUnit
        val itemPromoNames = mutableMapOf<Long, String>()
        val itemPromoSchemes = mutableMapOf<Long, String>()
        val appliedPromoNamesSet = mutableSetOf<String>()

        val totalCartOriginal = cartItems.sumOf { it.totalOriginal }

        for (promo in activePromotions) {
            when (promo.promoScheme) {
                "FLASH_SALE" -> {
                    // Check time window
                    val isWithinHour = currentHour in promo.startHour until promo.endHour
                    if (isWithinHour) {
                        for (item in cartItems) {
                            if (matchesTarget(item, promo)) {
                                val disc = calculateDiscountAmount(item.unitPrice, promo.type, promo.value)
                                if (disc > (itemDiscounts[item.product.id] ?: 0.0)) {
                                    itemDiscounts[item.product.id] = disc
                                    val discLabel = formatDiscountLabel(promo.type, promo.value, disc)
                                    itemPromoNames[item.product.id] = "${promo.name.trim()} ($discLabel)"
                                    itemPromoSchemes[item.product.id] = "FLASH_SALE"
                                    appliedPromoNamesSet.add("${promo.name.trim()} ($discLabel)")
                                }
                            }
                        }
                    }
                }

                "TIERED" -> {
                    // Promo bertingkat: misal belanja diatas 50rb diskon 10%, 100rb diskon 20%, 150rb diskon 30%, dst
                    if (promo.tierRules.isNotBlank()) {
                        val tiers = promo.tierRules.split(",").mapNotNull { part ->
                            val tokens = part.trim().split(":")
                            if (tokens.size >= 2) {
                                val minSpend = tokens[0].toDoubleOrNull()
                                val discPct = tokens[1].toDoubleOrNull()
                                if (minSpend != null && discPct != null) Pair(minSpend, discPct) else null
                            } else null
                        }.sortedByDescending { it.first } // Highest spending threshold checked first

                        val qualifyingTier = tiers.firstOrNull { totalCartOriginal >= it.first }
                        if (qualifyingTier != null) {
                            val discPercent = qualifyingTier.second
                            for (item in cartItems) {
                                val disc = (item.unitPrice * (discPercent / 100.0)).coerceAtMost(item.unitPrice)
                                if (disc > (itemDiscounts[item.product.id] ?: 0.0)) {
                                    itemDiscounts[item.product.id] = disc
                                    itemPromoNames[item.product.id] = "${promo.name} (${discPercent.toInt()}%)"
                                    itemPromoSchemes[item.product.id] = "TIERED"
                                    appliedPromoNamesSet.add("${promo.name} (Tier ≥${CurrencyFormatter.formatRupiah(qualifyingTier.first)}: ${discPercent.toInt()}%)")
                                }
                            }
                        }
                    } else {
                        // Fallback quantity-based tiered discount
                        for (item in cartItems) {
                            if (matchesTarget(item, promo) && item.quantity >= promo.minQuantity) {
                                val disc = calculateDiscountAmount(item.unitPrice, promo.type, promo.value)
                                if (disc > (itemDiscounts[item.product.id] ?: 0.0)) {
                                    itemDiscounts[item.product.id] = disc
                                    val discLabel = formatDiscountLabel(promo.type, promo.value, disc)
                                    itemPromoNames[item.product.id] = "${promo.name.trim()} ($discLabel)"
                                    itemPromoSchemes[item.product.id] = "TIERED"
                                    appliedPromoNamesSet.add("${promo.name.trim()} ($discLabel)")
                                }
                            }
                        }
                    }
                }

                "PWP" -> {
                    // Tebus Murah: Setiap belanja di atas Rp X (misal 50rb) bisa tebus murah produk pilihan (bisa >1 produk) sebesar Y% (10/20/30/40/50%)
                    val minSpend = if (promo.minPurchaseAmount > 0.0) promo.minPurchaseAmount else 50000.0
                    val rewardIds = if (promo.multiProductIds.isNotBlank()) {
                        promo.multiProductIds.split(",").mapNotNull { it.trim().toLongOrNull() }
                    } else if (promo.rewardProductId > 0L) {
                        listOf(promo.rewardProductId)
                    } else {
                        emptyList()
                    }

                    if (rewardIds.isNotEmpty()) {
                        val nonTebusSpend = cartItems.filter { it.product.id !in rewardIds }.sumOf { it.totalOriginal }
                        val qualifies = (nonTebusSpend >= minSpend) || (totalCartOriginal >= minSpend && nonTebusSpend > 0)

                        if (qualifies) {
                            for (item in cartItems) {
                                if (item.product.id in rewardIds) {
                                    val disc = if (promo.rewardSpecialPrice > 0.0) {
                                        (item.unitPrice - promo.rewardSpecialPrice).coerceAtLeast(0.0)
                                    } else {
                                        calculateDiscountAmount(item.unitPrice, promo.type, promo.value)
                                    }
                                    if (disc > (itemDiscounts[item.product.id] ?: 0.0)) {
                                        itemDiscounts[item.product.id] = disc
                                        val pctLabel = if (promo.type.equals("PERCENTAGE", ignoreCase = true)) "${promo.value.toInt()}%" else CurrencyFormatter.formatRupiah(disc)
                                        itemPromoNames[item.product.id] = "${promo.name} ($pctLabel)"
                                        itemPromoSchemes[item.product.id] = "PWP"
                                        appliedPromoNamesSet.add("${promo.name} ($pctLabel)")
                                    }
                                }
                            }
                        }
                    } else if (promo.triggerProductId > 0L && promo.rewardProductId > 0L) {
                        // Legacy single trigger item logic
                        val triggerItem = cartItems.firstOrNull { it.product.id == promo.triggerProductId }
                        val rewardItem = cartItems.firstOrNull { it.product.id == promo.rewardProductId }
                        if (triggerItem != null && rewardItem != null && triggerItem.quantity >= promo.triggerMinQty) {
                            val disc = if (promo.rewardSpecialPrice > 0.0) {
                                (rewardItem.unitPrice - promo.rewardSpecialPrice).coerceAtLeast(0.0)
                            } else {
                                calculateDiscountAmount(rewardItem.unitPrice, promo.type, promo.value)
                            }
                            if (disc > (itemDiscounts[rewardItem.product.id] ?: 0.0)) {
                                itemDiscounts[rewardItem.product.id] = disc
                                val discLabel = if (promo.rewardSpecialPrice > 0.0) CurrencyFormatter.formatRupiah(disc) else formatDiscountLabel(promo.type, promo.value, disc)
                                itemPromoNames[rewardItem.product.id] = "${promo.name.trim()} ($discLabel)"
                                itemPromoSchemes[rewardItem.product.id] = "PWP"
                                appliedPromoNamesSet.add("${promo.name.trim()} ($discLabel)")
                            }
                        }
                    }
                }

                "BUNDLING" -> {
                    // Bundling: bisa tambah produk bundling lebih dari 1 (>2 produk)
                    val bundleIds = if (promo.multiProductIds.isNotBlank()) {
                        promo.multiProductIds.split(",").mapNotNull { it.trim().toLongOrNull() }
                    } else if (promo.triggerProductId > 0L && promo.rewardProductId > 0L) {
                        listOf(promo.triggerProductId, promo.rewardProductId)
                    } else {
                        emptyList()
                    }

                    if (bundleIds.size >= 2) {
                        val matchingItems = bundleIds.mapNotNull { bId -> cartItems.firstOrNull { it.product.id == bId } }
                        if (matchingItems.size == bundleIds.size) {
                            val numBundles = matchingItems.minOf { it.quantity }
                            if (numBundles > 0) {
                                for (item in matchingItems) {
                                    val baseDisc = if (promo.type.equals("PERCENTAGE", ignoreCase = true)) {
                                        (item.unitPrice * (promo.value / 100.0)).coerceAtMost(item.unitPrice)
                                    } else {
                                        (promo.value / bundleIds.size).coerceAtMost(item.unitPrice)
                                    }
                                    val disc = if (item.quantity > 0) (baseDisc * numBundles) / item.quantity else baseDisc
                                    if (disc > (itemDiscounts[item.product.id] ?: 0.0)) {
                                        itemDiscounts[item.product.id] = disc
                                        val discLabel = if (promo.type.equals("PERCENTAGE", ignoreCase = true)) {
                                            if (promo.value % 1.0 == 0.0) "${promo.value.toInt()}%" else "${promo.value}%"
                                        } else {
                                            CurrencyFormatter.formatRupiah(baseDisc)
                                        }
                                        itemPromoNames[item.product.id] = "${promo.name.trim()} ($discLabel)"
                                        itemPromoSchemes[item.product.id] = "BUNDLING"
                                        appliedPromoNamesSet.add("${promo.name} ($numBundles Paket)")
                                    }
                                }
                            }
                        }
                    }
                }

                "DAY_SPECIFIC" -> {
                    // Promo hari tertentu: bisa pilih lebih dari 1 produk
                    val isDayActive = promo.activeDays.isBlank() ||
                            promo.activeDays.uppercase().contains(currentDayName) ||
                            (promo.activeDays.uppercase().contains("WEEKEND") && (currentDayName == "SABTU" || currentDayName == "MINGGU"))

                    if (isDayActive) {
                        val multiIds = if (promo.multiProductIds.isNotBlank()) {
                            promo.multiProductIds.split(",").mapNotNull { it.trim().toLongOrNull() }.toSet()
                        } else null

                        for (item in cartItems) {
                            val matches = when {
                                multiIds != null -> item.product.id in multiIds
                                else -> matchesTarget(item, promo)
                            }
                            if (matches) {
                                val disc = calculateDiscountAmount(item.unitPrice, promo.type, promo.value)
                                if (disc > (itemDiscounts[item.product.id] ?: 0.0)) {
                                    itemDiscounts[item.product.id] = disc
                                    val discLabel = formatDiscountLabel(promo.type, promo.value, disc)
                                    itemPromoNames[item.product.id] = "${promo.name.trim()} ($discLabel)"
                                    itemPromoSchemes[item.product.id] = "DAY_SPECIFIC"
                                    appliedPromoNamesSet.add("${promo.name.trim()} ($discLabel)")
                                }
                            }
                        }
                    }
                }

                "CATEGORY" -> {
                    for (item in cartItems) {
                        if (item.product.categoryId == promo.targetId) {
                            val disc = calculateDiscountAmount(item.unitPrice, promo.type, promo.value)
                            if (disc > (itemDiscounts[item.product.id] ?: 0.0)) {
                                itemDiscounts[item.product.id] = disc
                                val discLabel = formatDiscountLabel(promo.type, promo.value, disc)
                                itemPromoNames[item.product.id] = "${promo.name.trim()} ($discLabel)"
                                itemPromoSchemes[item.product.id] = "CATEGORY"
                                appliedPromoNamesSet.add("${promo.name.trim()} ($discLabel)")
                            }
                        }
                    }
                }

                else -> {
                    for (item in cartItems) {
                        if (matchesTarget(item, promo)) {
                            val disc = calculateDiscountAmount(item.unitPrice, promo.type, promo.value)
                            if (disc > (itemDiscounts[item.product.id] ?: 0.0)) {
                                itemDiscounts[item.product.id] = disc
                                val discLabel = formatDiscountLabel(promo.type, promo.value, disc)
                                itemPromoNames[item.product.id] = "${promo.name.trim()} ($discLabel)"
                                itemPromoSchemes[item.product.id] = "GENERAL"
                                appliedPromoNamesSet.add("${promo.name.trim()} ($discLabel)")
                            }
                        }
                    }
                }
            }
        }

        val updatedItems = cartItems.map { item ->
            val disc = itemDiscounts[item.product.id] ?: 0.0
            val pName = itemPromoNames[item.product.id] ?: ""
            val pScheme = itemPromoSchemes[item.product.id] ?: ""
            item.copy(
                customDiscount = disc,
                appliedPromoName = pName,
                appliedPromoScheme = pScheme
            )
        }

        val totalSavings = updatedItems.sumOf { it.totalDiscount }

        return CartPromotionResult(
            updatedItems = updatedItems,
            totalSavings = totalSavings,
            appliedPromotions = appliedPromoNamesSet.toList()
        )
    }

    private fun matchesTarget(item: CartItem, promo: PromotionEntity): Boolean {
        return when (promo.targetType) {
            "ALL_PRODUCTS" -> true
            "SPECIFIC_PRODUCT" -> item.product.id == promo.targetId
            "SPECIFIC_CATEGORY", "CATEGORY" -> item.product.categoryId == promo.targetId
            "MULTI_PRODUCTS" -> {
                if (promo.multiProductIds.isNotBlank()) {
                    val ids = promo.multiProductIds.split(",").mapNotNull { it.trim().toLongOrNull() }
                    item.product.id in ids
                } else {
                    item.product.id == promo.targetId
                }
            }
            else -> item.product.id == promo.targetId
        }
    }

    private fun calculateDiscountAmount(unitPrice: Double, type: String, value: Double): Double {
        return if (type.equals("PERCENTAGE", ignoreCase = true)) {
            (unitPrice * (value / 100.0)).coerceAtMost(unitPrice)
        } else {
            value.coerceAtMost(unitPrice)
        }
    }
    private fun formatDiscountLabel(promoType: String, promoValue: Double, actualDiscount: Double): String {
        return if (promoType.equals("PERCENTAGE", ignoreCase = true) && promoValue > 0.0) {
            if (promoValue % 1.0 == 0.0) "${promoValue.toInt()}%" else "${promoValue}%"
        } else {
            val amount = if (actualDiscount > 0.0) actualDiscount else promoValue
            CurrencyFormatter.formatRupiah(amount)
        }
    }

}
