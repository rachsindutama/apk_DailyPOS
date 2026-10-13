package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.KasirDatabase
import com.example.data.local.dao.TopProductSummary
import com.example.data.local.dao.TransactionWithItems
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.PromotionEntity
import com.example.data.local.entity.StockMovementEntity
import com.example.data.local.entity.StoreSettingEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionItemEntity
import com.example.data.local.entity.UnitEntity
import com.example.data.local.entity.UserEntity
import com.example.data.model.CartItem
import com.example.data.sync.CloudSyncManager
import com.example.util.DataImportHelper
import com.example.util.ParsedImportProduct
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class KasirRepository(private val database: KasirDatabase) {

    private val categoryDao = database.categoryDao()
    private val productDao = database.productDao()
    private val transactionDao = database.transactionDao()
    private val stockMovementDao = database.stockMovementDao()
    private val promotionDao = database.promotionDao()
    private val storeSettingDao = database.storeSettingDao()
    private val unitDao = database.unitDao()
    private val userDao = database.userDao()

    var syncManager: CloudSyncManager? = null

    // Users Flow
    val currentUserEntity: Flow<UserEntity?> = userDao.getPrimaryUserFlow()
    val allUsers: Flow<List<UserEntity>> = userDao.getAllUsersFlow()

    // Stock Movements & Products Flow
    val allStockMovements: Flow<List<StockMovementEntity>> = stockMovementDao.getAllMovements()

    // Authoritative Products Flow: Always reads the latest stock from Mutasi Stok (stock_movements)
    val allProducts: Flow<List<ProductEntity>> = combine(
        productDao.getAllProducts(),
        stockMovementDao.getAllMovements()
    ) { products, movements ->
        val latestStockByProductId = mutableMapOf<Long, Int>()
        for (move in movements) {
            if (!latestStockByProductId.containsKey(move.productId)) {
                latestStockByProductId[move.productId] = move.currentStock
            }
        }
        products.map { prod ->
            val latestStock = latestStockByProductId[prod.id]
            prod.copy(stock = latestStock ?: 0)
        }
    }

    val lowStockProducts: Flow<List<ProductEntity>> = allProducts.map { list ->
        list.filter { it.stock <= it.minStock }.sortedBy { it.stock }
    }
    val lowStockCount: Flow<Int> = lowStockProducts.map { it.size }
    val allCategories: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()
    val allUnits: Flow<List<UnitEntity>> = unitDao.getAllUnits()
    val allPromotions: Flow<List<PromotionEntity>> = promotionDao.getAllPromotions()
    val activePromotions: Flow<List<PromotionEntity>> = promotionDao.getActivePromotions()
    val storeSettings: Flow<StoreSettingEntity?> = storeSettingDao.getStoreSettings()
    val allTransactions: Flow<List<TransactionWithItems>> = transactionDao.getAllTransactionsWithItems()
    val unsyncedCount: Flow<Int> = transactionDao.getUnsyncedCount()

    fun getProductsByCategory(categoryId: Long): Flow<List<ProductEntity>> {
        return allProducts.map { list ->
            if (categoryId == 0L) list else list.filter { it.categoryId == categoryId }
        }
    }

    fun searchProducts(query: String): Flow<List<ProductEntity>> {
        return allProducts.map { list ->
            if (query.isBlank()) list else {
                val q = query.trim().lowercase()
                list.filter {
                    it.name.lowercase().contains(q) ||
                    it.barcode.lowercase().contains(q) ||
                    it.sku.lowercase().contains(q)
                }
            }
        }
    }

    suspend fun getProductByBarcode(barcode: String): ProductEntity? {
        val prod = productDao.getProductByBarcode(barcode) ?: return null
        val latestStock = stockMovementDao.getLatestStockForProduct(prod.id)
        return prod.copy(stock = latestStock ?: 0)
    }

    suspend fun getProductBySku(sku: String): ProductEntity? {
        val prod = productDao.getProductBySku(sku) ?: return null
        val latestStock = stockMovementDao.getLatestStockForProduct(prod.id)
        return prod.copy(stock = latestStock ?: 0)
    }

    suspend fun getProductByBarcodeOrSku(code: String): ProductEntity? {
        val clean = code.trim()
        if (clean.isBlank()) return null
        val prod = productDao.getProductByBarcode(clean) ?: productDao.getProductBySku(clean) ?: return null
        val latestStock = stockMovementDao.getLatestStockForProduct(prod.id)
        return prod.copy(stock = latestStock ?: 0)
    }

    suspend fun getProductById(id: Long): ProductEntity? {
        val prod = productDao.getProductById(id) ?: return null
        val latestStock = stockMovementDao.getLatestStockForProduct(id)
        return prod.copy(stock = latestStock ?: 0)
    }

    suspend fun getAllProductsDirect(): List<ProductEntity> {
        val prods = productDao.getAllProductsDirect()
        val movements = stockMovementDao.getAllMovementsList()
        val latestStockByProductId = mutableMapOf<Long, Int>()
        for (move in movements) {
            if (!latestStockByProductId.containsKey(move.productId)) {
                latestStockByProductId[move.productId] = move.currentStock
            }
        }
        return prods.map { prod ->
            val latestStock = latestStockByProductId[prod.id]
            prod.copy(stock = latestStock ?: 0)
        }
    }

    suspend fun getLatestStockForProduct(productId: Long): Int? {
        return stockMovementDao.getLatestStockForProduct(productId)
    }

    suspend fun synchronizeAllProductsStockFromMovements() {
        try {
            val allProds = productDao.getAllProductsDirect()
            val allMovements = stockMovementDao.getAllMovementsList()
            if (allMovements.isEmpty()) return

            val latestStockByProduct = mutableMapOf<Long, Int>()
            for (move in allMovements) {
                if (!latestStockByProduct.containsKey(move.productId)) {
                    latestStockByProduct[move.productId] = move.currentStock
                }
            }

            for (prod in allProds) {
                val latestStock = latestStockByProduct[prod.id]
                if (latestStock != null && latestStock != prod.stock) {
                    productDao.setProductStock(prod.id, latestStock, System.currentTimeMillis())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun insertProduct(product: ProductEntity): Long {
        val finalProduct = (if (product.sku.isBlank()) {
            product.copy(sku = DataImportHelper.generateSku("PRD", (System.currentTimeMillis() % 10000).toInt()))
        } else {
            product
        }).copy(stock = 0, isSynced = false, updatedAt = System.currentTimeMillis())

        val id = productDao.insertProduct(finalProduct)
        val productWithId = finalProduct.copy(id = id)

        syncManager?.pushProduct(productWithId)
        return id
    }

    suspend fun updateProduct(product: ProductEntity) {
        val finalProduct = (if (product.sku.isBlank()) {
            product.copy(sku = DataImportHelper.generateSku("PRD", product.id.toInt()))
        } else {
            product
        }).copy(isSynced = false, updatedAt = System.currentTimeMillis())

        productDao.updateProduct(finalProduct)
        syncManager?.pushProduct(finalProduct)
    }

    suspend fun deleteProduct(product: ProductEntity) {
        productDao.deleteProduct(product)
        stockMovementDao.deleteMovementsByProductId(product.id)
        syncManager?.deleteProductFromCloud(product)
    }

    suspend fun deleteStockMovement(movement: StockMovementEntity) {
        stockMovementDao.deleteMovement(movement)
        syncManager?.deleteStockMovementFromCloud(movement)
    }

    suspend fun deleteLocalStockMovementById(id: Long) {
        stockMovementDao.deleteMovementById(id)
    }

    suspend fun deleteMovementByProductAndTimestamp(productId: Long, timestamp: Long) {
        stockMovementDao.deleteMovementByProductAndTimestamp(productId, timestamp)
    }

    suspend fun saveStockMovementFromCloud(movement: StockMovementEntity) {
        val existing = stockMovementDao.getMovementByProductAndTimestamp(movement.productId, movement.timestamp)
            ?: if (movement.id > 0) stockMovementDao.getMovementById(movement.id) else null
            ?: stockMovementDao.getNearDuplicateMovement(movement.productId, movement.timestamp)

        if (existing != null) {
            stockMovementDao.updateMovement(movement.copy(id = existing.id, isSynced = true))
        } else {
            stockMovementDao.insertMovement(movement.copy(isSynced = true))
        }

        val latestStock = stockMovementDao.getLatestStockForProduct(movement.productId)
        if (latestStock != null) {
            productDao.setProductStock(movement.productId, latestStock, movement.timestamp)
        }
    }

    suspend fun deduplicateStockMovements() {
        try {
            val all = stockMovementDao.getAllMovementsList()
            val seen = mutableListOf<StockMovementEntity>()
            val idsToDelete = mutableListOf<Long>()

            for (move in all) {
                val duplicate = seen.firstOrNull { existing ->
                    existing.productId == move.productId &&
                    existing.type == move.type &&
                    existing.quantity == move.quantity &&
                    existing.reason.trim().equals(move.reason.trim(), ignoreCase = true) &&
                    Math.abs(existing.timestamp - move.timestamp) < 120_000L
                }
                if (duplicate != null) {
                    idsToDelete.add(move.id)
                } else {
                    seen.add(move)
                }
            }

            if (idsToDelete.isNotEmpty()) {
                stockMovementDao.deleteMovementsByIds(idsToDelete)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deduplicateProducts() {
        try {
            val all = productDao.getAllProductsDirect()
            if (all.size <= 1) return

            val idsToDelete = mutableSetOf<Long>()

            // 1. Deduplicate by barcode
            val groupedByBarcode = all.filter { it.barcode.isNotBlank() }.groupBy { it.barcode.trim() }
            for ((_, group) in groupedByBarcode) {
                if (group.size > 1) {
                    val sorted = group.sortedByDescending { it.updatedAt }
                    for (duplicate in sorted.drop(1)) {
                        idsToDelete.add(duplicate.id)
                    }
                }
            }

            // 2. Deduplicate by name
            val remainingAfterBarcode = all.filter { it.id !in idsToDelete }
            val groupedByName = remainingAfterBarcode.groupBy { it.name.trim().lowercase() }
            for ((_, group) in groupedByName) {
                if (group.size > 1) {
                    val sorted = group.sortedByDescending { it.updatedAt }
                    for (duplicate in sorted.drop(1)) {
                        idsToDelete.add(duplicate.id)
                    }
                }
            }

            for (id in idsToDelete) {
                val prod = productDao.getProductById(id)
                if (prod != null) {
                    productDao.deleteProduct(prod)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun clearLocalStockMovements() {
        stockMovementDao.clearAllMovements()
    }

    // Cloud to local direct updates (bypasses remote re-push)
    suspend fun saveDirectProductFromCloud(product: ProductEntity) {
        val latestStock = stockMovementDao.getLatestStockForProduct(product.id)
        val effectiveProduct = if (latestStock != null) {
            product.copy(stock = latestStock, isSynced = true)
        } else {
            product.copy(isSynced = true)
        }
        productDao.insertProduct(effectiveProduct)
    }

    suspend fun deleteLocalProductOnly(product: ProductEntity) {
        productDao.deleteProduct(product)
    }

    // Categories
    suspend fun insertCategory(category: CategoryEntity): Long {
        val id = categoryDao.insertCategory(category)
        val catWithId = category.copy(id = id)
        syncManager?.pushCategory(catWithId)
        return id
    }

    suspend fun updateCategory(category: CategoryEntity) {
        categoryDao.updateCategory(category)
        syncManager?.pushCategory(category)
    }

    suspend fun deleteCategory(category: CategoryEntity) {
        categoryDao.deleteCategory(category)
        syncManager?.deleteCategoryFromCloud(category)
    }

    suspend fun saveCategoryFromCloud(category: CategoryEntity) {
        categoryDao.insertCategory(category)
    }

    suspend fun deleteLocalCategoryOnly(category: CategoryEntity) {
        categoryDao.deleteCategory(category)
    }

    suspend fun getAllCategoriesDirect(): List<CategoryEntity> = categoryDao.getAllCategoriesDirect()

    // Units (Master Satuan Dinamis - Bebas Duplikasi)
    suspend fun insertUnit(unit: UnitEntity): Long {
        val trimmedName = unit.name.trim()
        if (trimmedName.isBlank()) return 0L
        val existing = unitDao.getUnitByName(trimmedName)
        if (existing != null) {
            syncManager?.pushUnit(existing)
            return existing.id
        }
        val id = unitDao.insertUnit(unit.copy(name = trimmedName))
        val unitWithId = unit.copy(id = id, name = trimmedName)
        syncManager?.pushUnit(unitWithId)
        return id
    }

    suspend fun updateUnit(unit: UnitEntity) {
        val trimmedName = unit.name.trim()
        unitDao.updateUnit(unit.copy(name = trimmedName))
        syncManager?.pushUnit(unit.copy(name = trimmedName))
    }

    suspend fun deleteUnit(unit: UnitEntity) {
        unitDao.deleteUnit(unit)
        syncManager?.deleteUnitFromCloud(unit)
    }

    suspend fun saveUnitFromCloud(unit: UnitEntity) {
        val trimmedName = unit.name.trim()
        if (trimmedName.isBlank()) return
        val existing = unitDao.getUnitByName(trimmedName)
        if (existing != null) {
            if (existing.id != unit.id) {
                // Delete old local unit that has the same name but different ID to prevent unique index conflict
                unitDao.deleteUnit(existing)
            }
        }
        // Safely insert/replace unit preserving its original cloud ID
        unitDao.insertUnit(unit.copy(name = trimmedName))
    }

    suspend fun deleteLocalUnitOnly(unit: UnitEntity) {
        val existing = unitDao.getUnitByName(unit.name.trim())
        if (existing != null) {
            unitDao.deleteUnit(existing)
        }
    }

    suspend fun getAllUnitsDirect(): List<UnitEntity> {
        ensureDefaultUnitsExist()
        return unitDao.getAllUnitsDirect()
    }

    suspend fun ensureDefaultUnitsExist() {
        val defaultUnits = listOf(
            "pcs", "kg", "gr", "pack", "bungkus",
            "renceng", "dus", "botol", "kaleng", "sachet",
            "liter", "meter", "porsi", "ikat", "butir", "roll"
        )
        for (u in defaultUnits) {
            val existing = unitDao.getUnitByName(u)
            if (existing == null) {
                val newUnit = UnitEntity(name = u, isDefault = true)
                val id = unitDao.insertUnit(newUnit)
                syncManager?.pushUnit(newUnit.copy(id = id))
            } else {
                // Ensure it is pushed under the new ID-based system in Firestore
                syncManager?.pushUnit(existing)
            }
        }
    }

    // Data Import
    suspend fun importProductsWithCategories(items: List<ParsedImportProduct>): com.example.util.ImportResult {
        var importedCount = 0
        var newCatCount = 0
        val errors = mutableListOf<String>()

        val colors = listOf("#006C4C", "#B86200", "#00658E", "#735C00", "#6750A4", "#BA1A1A", "#0284C7", "#D97706")
        var colorIdx = 0

        val categoryMap = categoryDao.getAllCategoriesDirect().associateBy { it.name.trim().lowercase() }.toMutableMap()
        val currentProductCount = productDao.getProductCount()
        val importedProductsList = mutableListOf<ProductEntity>()

        for ((index, item) in items.withIndex()) {
            try {
                val catKey = item.categoryName.trim().lowercase()
                var catId = categoryMap[catKey]?.id ?: 0L
                if (catId == 0L && item.categoryName.isNotBlank()) {
                    val assignedColor = colors[colorIdx % colors.size]
                    colorIdx++
                    catId = categoryDao.insertCategory(
                        CategoryEntity(name = item.categoryName.trim(), colorHex = assignedColor)
                    )
                    val newCat = CategoryEntity(id = catId, name = item.categoryName.trim(), colorHex = assignedColor)
                    categoryMap[catKey] = newCat
                    syncManager?.pushCategory(newCat)
                    newCatCount++
                }

                val existing = (if (item.barcode.isNotBlank()) productDao.getProductByBarcode(item.barcode) else null)
                    ?: (if (item.sku.isNotBlank()) productDao.getProductBySku(item.sku) else null)

                val targetSku = when {
                    item.sku.isNotBlank() -> item.sku
                    existing != null && existing.sku.isNotBlank() -> existing.sku
                    else -> DataImportHelper.generateSku(item.categoryName, currentProductCount + index + 1)
                }

                if (existing != null) {
                    val updated = existing.copy(
                        sku = if (existing.sku.isNotBlank()) existing.sku else targetSku,
                        name = item.name,
                        categoryId = catId,
                        buyPrice = item.buyPrice,
                        sellPrice = item.sellPrice,
                        stock = existing.stock,
                        minStock = item.minStock,
                        unit = item.unit,
                        updatedAt = System.currentTimeMillis(),
                        isSynced = false
                    )
                    productDao.updateProduct(updated)
                    importedProductsList.add(updated)
                } else {
                    val newProduct = ProductEntity(
                        sku = targetSku,
                        barcode = item.barcode,
                        name = item.name,
                        categoryId = catId,
                        buyPrice = item.buyPrice,
                        sellPrice = item.sellPrice,
                        stock = 0,
                        minStock = item.minStock,
                        unit = item.unit,
                        updatedAt = System.currentTimeMillis(),
                        isSynced = false
                    )
                    val insertedId = productDao.insertProduct(newProduct)
                    importedProductsList.add(newProduct.copy(id = insertedId))
                }
                importedCount++
            } catch (e: Exception) {
                errors.add("${item.name}: ${e.message}")
            }
        }

        // Push all imported products to Firestore immediately
        for (prod in importedProductsList) {
            syncManager?.pushProduct(prod)
        }

        return com.example.util.ImportResult(importedCount, newCatCount, errors)
    }

    // Stock In / Out Adjustment
    suspend fun recordStockIn(
        product: ProductEntity,
        qty: Int,
        supplier: String,
        newBuyPrice: Double? = null,
        notes: String = "",
        timestamp: Long = System.currentTimeMillis()
    ) {
        if (qty <= 0) return

        // Duplicate guard: avoid recording identical movement within 3 seconds
        val existingRecent = stockMovementDao.getRecentMovement(
            productId = product.id,
            type = "MASUK",
            quantity = qty,
            minTimestamp = timestamp - 3000L
        )
        if (existingRecent != null) return

        val currentProduct = productDao.getProductById(product.id) ?: product
        val latestStock = stockMovementDao.getLatestStockForProduct(currentProduct.id)
        val previousStock = latestStock ?: 0
        val newStock = previousStock + qty
        val costPrice = if (newBuyPrice != null && newBuyPrice > 0.0) newBuyPrice else currentProduct.buyPrice

        val updatedProduct = currentProduct.copy(
            stock = newStock,
            buyPrice = costPrice,
            updatedAt = timestamp,
            isSynced = false
        )
        productDao.updateProduct(updatedProduct)

        val movement = StockMovementEntity(
            productId = currentProduct.id,
            productName = currentProduct.name,
            type = "MASUK",
            quantity = qty,
            previousStock = previousStock,
            currentStock = newStock,
            reason = "Barang Masuk / Pembelian Supplier: ${supplier.ifBlank { "Umum" }}",
            notes = notes,
            costPrice = costPrice,
            timestamp = timestamp
        )
        val generatedId = stockMovementDao.insertMovement(movement)
        val movementWithId = movement.copy(id = generatedId)

        syncManager?.pushStockMovement(movementWithId)
        syncManager?.pushProduct(updatedProduct)
    }

    suspend fun recordStockOut(
        product: ProductEntity,
        qty: Int,
        reason: String,
        notes: String = "",
        isNsb: Boolean = false,
        lossAmount: Double = 0.0,
        timestamp: Long = System.currentTimeMillis()
    ) {
        if (qty <= 0) return

        // Duplicate guard: avoid recording identical movement within 3 seconds
        val existingRecent = stockMovementDao.getRecentMovement(
            productId = product.id,
            type = "KELUAR",
            quantity = -qty,
            minTimestamp = timestamp - 3000L
        )
        if (existingRecent != null) return

        val currentProduct = productDao.getProductById(product.id) ?: product
        val latestStock = stockMovementDao.getLatestStockForProduct(currentProduct.id)
        val previousStock = latestStock ?: 0
        if (previousStock <= 0 || qty > previousStock) return
        val newStock = (previousStock - qty).coerceAtLeast(0)

        val updatedProduct = currentProduct.copy(
            stock = newStock,
            updatedAt = timestamp,
            isSynced = false
        )
        productDao.updateProduct(updatedProduct)

        val unitCost = if (currentProduct.buyPrice > 0.0) currentProduct.buyPrice else currentProduct.sellPrice
        val isNsbCalc = isNsb ||
            reason.contains("Rusak", ignoreCase = true) ||
            reason.contains("Expired", ignoreCase = true) ||
            reason.contains("Kadaluarsa", ignoreCase = true) ||
            reason.contains("Hilang", ignoreCase = true) ||
            reason.contains("Pecah", ignoreCase = true) ||
            reason.contains("Basi", ignoreCase = true) ||
            reason.contains("Selisih", ignoreCase = true) ||
            reason.contains("NSB", ignoreCase = true) ||
            lossAmount > 0.0
        val lossCalc = if (lossAmount > 0.0) lossAmount else if (isNsbCalc) qty * unitCost else 0.0

        val movement = StockMovementEntity(
            productId = currentProduct.id,
            productName = currentProduct.name,
            type = "KELUAR",
            quantity = -qty,
            previousStock = previousStock,
            currentStock = newStock,
            reason = reason.ifBlank { "Rusak/Kadaluarsa" },
            notes = notes,
            costPrice = unitCost,
            isNsb = isNsbCalc,
            lossAmount = lossCalc,
            timestamp = timestamp
        )
        val generatedId = stockMovementDao.insertMovement(movement)
        val movementWithId = movement.copy(id = generatedId)

        syncManager?.pushStockMovement(movementWithId)
        syncManager?.pushProduct(updatedProduct)
    }

    suspend fun recordStockOpname(
        product: ProductEntity,
        actualStock: Int,
        notes: String = "",
        timestamp: Long = System.currentTimeMillis()
    ) {
        val currentProduct = productDao.getProductById(product.id) ?: product
        val latestStock = stockMovementDao.getLatestStockForProduct(currentProduct.id)
        val previousStock = latestStock ?: 0
        val delta = actualStock - previousStock

        val updatedProduct = currentProduct.copy(
            stock = actualStock,
            updatedAt = timestamp,
            isSynced = false
        )
        productDao.updateProduct(updatedProduct)

        val isLoss = delta < 0
        val unitCost = if (currentProduct.buyPrice > 0.0) currentProduct.buyPrice else currentProduct.sellPrice
        val lossCalc = if (isLoss) Math.abs(delta) * unitCost else 0.0

        val movement = StockMovementEntity(
            productId = currentProduct.id,
            productName = currentProduct.name,
            type = "PENYESUAIAN",
            quantity = delta,
            previousStock = previousStock,
            currentStock = actualStock,
            reason = if (isLoss) "Penyesuaian Selisih Opname (NSB)" else "Penyesuaian Stok Opname Fisik",
            notes = notes,
            costPrice = unitCost,
            isNsb = isLoss,
            lossAmount = lossCalc,
            timestamp = timestamp
        )
        val generatedId = stockMovementDao.insertMovement(movement)
        val movementWithId = movement.copy(id = generatedId)

        syncManager?.pushStockMovement(movementWithId)
        syncManager?.pushProduct(updatedProduct)
    }

    // Checkout Transaction Processing
    suspend fun processCheckout(
        cartItems: List<CartItem>,
        subtotal: Double,
        discountAmount: Double,
        discountNote: String,
        isTaxApplied: Boolean,
        taxPercent: Double,
        taxAmount: Double,
        grandTotal: Double,
        paymentMethod: String,
        cashReceived: Double,
        changeAmount: Double,
        cashierName: String,
        customerName: String
    ): TransactionWithItems {
        val timestamp = System.currentTimeMillis()
        val invoiceDateFormat = SimpleDateFormat("yyMMddHHmmss", Locale.US)
        val invoiceNumber = "INV-${invoiceDateFormat.format(Date(timestamp))}"

        val transaction = TransactionEntity(
            invoiceNumber = invoiceNumber,
            timestamp = timestamp,
            subtotal = subtotal,
            discountAmount = discountAmount,
            discountNote = discountNote,
            taxAmount = taxAmount,
            taxPercent = taxPercent,
            isTaxApplied = isTaxApplied,
            grandTotal = grandTotal,
            paymentMethod = paymentMethod,
            cashReceived = cashReceived,
            changeAmount = changeAmount,
            cashierName = cashierName,
            customerName = customerName,
            isSynced = false
        )

        val (savedTx, transactionItems, prodsAndMoves) = database.withTransaction {
            val transactionId = transactionDao.insertTransaction(transaction)

            val transactionItems = cartItems.map { cartItem ->
                TransactionItemEntity(
                    transactionId = transactionId,
                    productId = cartItem.product.id,
                    barcode = cartItem.product.barcode,
                    sku = cartItem.product.sku,
                    productName = cartItem.product.name,
                    quantity = cartItem.quantity,
                    unitPrice = cartItem.unitPrice,
                    costPrice = cartItem.product.buyPrice,
                    discountAmount = cartItem.totalDiscount,
                    promoNote = cartItem.appliedPromoName,
                    subtotal = cartItem.subtotal
                )
            }
            transactionDao.insertTransactionItems(transactionItems)

            val updatedProds = mutableListOf<ProductEntity>()
            val recordedMovements = mutableListOf<StockMovementEntity>()
            // Deduct inventory stock & record stock movement for each item
            for (item in cartItems) {
                val currentProduct = productDao.getProductById(item.product.id)
                    ?: (if (item.product.barcode.isNotBlank()) productDao.getProductByBarcode(item.product.barcode) else null)
                    ?: (if (item.product.sku.isNotBlank()) productDao.getProductBySku(item.product.sku) else null)

                val targetProductId = currentProduct?.id ?: item.product.id
                // CRITICAL FIX: Read authoritative previous stock directly from latest stock movement (mutasi stok)
                val latestMovementStock = stockMovementDao.getLatestStockForProduct(targetProductId)
                val prevStock = latestMovementStock ?: 0
                val newStock = (prevStock - item.quantity).coerceAtLeast(0)

                productDao.setProductStock(targetProductId, newStock, timestamp)

                val movement = StockMovementEntity(
                    productId = targetProductId,
                    productName = item.product.name,
                    type = "PENJUALAN",
                    quantity = -item.quantity,
                    previousStock = prevStock,
                    currentStock = newStock,
                    reason = "Penjualan Kasir $invoiceNumber",
                    costPrice = item.product.buyPrice,
                    timestamp = timestamp
                )
                val moveId = stockMovementDao.insertMovement(movement)
                recordedMovements.add(movement.copy(id = moveId))

                val updatedProd = (currentProduct ?: item.product).copy(
                    id = targetProductId,
                    stock = newStock,
                    updatedAt = timestamp,
                    isSynced = false
                )
                updatedProds.add(updatedProd)
            }
            Triple(transaction.copy(id = transactionId), transactionItems, Pair(updatedProds, recordedMovements))
        }

        val (updatedProducts, recordedMovements) = prodsAndMoves

        for (prod in updatedProducts) {
            syncManager?.pushProduct(prod)
        }
        for (move in recordedMovements) {
            syncManager?.pushStockMovement(move)
        }

        // IMMEDIATELY push the transaction with ALL its items to cloud so no items ever get lost/bypassed
        syncManager?.pushTransactionWithItems(savedTx, transactionItems)
        syncManager?.triggerAutoSync()

        return TransactionWithItems(
            transaction = savedTx,
            items = transactionItems
        )
    }

    // Reports
    fun getTransactionsBetween(startTimestamp: Long, endTimestamp: Long): Flow<List<TransactionWithItems>> {
        return transactionDao.getTransactionsBetween(startTimestamp, endTimestamp)
    }

    suspend fun getTransactionByInvoice(invoiceNumber: String): TransactionWithItems? {
        return transactionDao.getTransactionByInvoice(invoiceNumber)
    }

    suspend fun saveTransactionWithItemsFromCloud(
        transaction: TransactionEntity,
        items: List<TransactionItemEntity>
    ) {
        val existing = transactionDao.getTransactionByInvoice(transaction.invoiceNumber)

        // CRITICAL PROTECTION: Never overwrite existing local items if the incoming cloud items list is empty!
        if (items.isEmpty() && existing != null && existing.items.isNotEmpty()) {
            syncManager?.pushTransactionWithItems(existing.transaction, existing.items)
            return
        }

        // AUTO-RECONCILIATION: If both remote and local items are missing, reconstruct from stock movements!
        var resolvedItems = items
        if (resolvedItems.isEmpty()) {
            val movements = stockMovementDao.getMovementsForInvoice(transaction.invoiceNumber)
            if (movements.isNotEmpty()) {
                resolvedItems = movements.map { move ->
                    val prod = productDao.getProductById(move.productId)
                    val unitPrice = prod?.sellPrice ?: (move.costPrice * 1.2)
                    val qty = kotlin.math.abs(move.quantity).coerceAtLeast(1)
                    TransactionItemEntity(
                        id = 0,
                        transactionId = existing?.transaction?.id ?: 0L,
                        productId = move.productId,
                        barcode = prod?.barcode ?: "",
                        sku = prod?.sku ?: "",
                        productName = move.productName,
                        quantity = qty,
                        unitPrice = unitPrice,
                        costPrice = move.costPrice,
                        discountAmount = 0.0,
                        promoNote = "",
                        subtotal = unitPrice * qty
                    )
                }
            }
        }

        val targetId = if (existing != null) {
            if (resolvedItems.isNotEmpty()) {
                transactionDao.deleteTransactionItems(existing.transaction.id)
            }
            transactionDao.insertTransaction(transaction.copy(id = existing.transaction.id, isSynced = true))
            existing.transaction.id
        } else {
            transactionDao.insertTransaction(transaction.copy(id = 0, isSynced = true))
        }

        if (resolvedItems.isNotEmpty()) {
            val mappedItems = resolvedItems.map { it.copy(id = 0, transactionId = targetId) }
            transactionDao.insertTransactionItems(mappedItems)

            // If we repaired missing items from stock movements, immediately repair the cloud transaction too!
            if (items.isEmpty()) {
                syncManager?.pushTransactionWithItems(transaction.copy(id = targetId), mappedItems)
            }
        }
    }

    suspend fun deleteLocalTransactionByInvoice(invoiceNumber: String) {
        val existing = transactionDao.getTransactionByInvoice(invoiceNumber)
        if (existing != null) {
            transactionDao.deleteTransactionItems(existing.transaction.id)
            transactionDao.deleteTransaction(existing.transaction.id)
        }
    }

    fun getTopProducts(startTimestamp: Long, endTimestamp: Long, limit: Int = 10): Flow<List<TopProductSummary>> {
        return transactionDao.getTopProducts(startTimestamp, endTimestamp, limit)
    }

    fun getMovementsBetween(startTimestamp: Long, endTimestamp: Long): Flow<List<StockMovementEntity>> {
        return stockMovementDao.getMovementsBetween(startTimestamp, endTimestamp)
    }

    suspend fun reconcileNsbMovements() {
        try {
            deduplicateStockMovements()
            val potentialNsb = stockMovementDao.getAllPotentialNsbMovements()
            for (move in potentialNsb) {
                val reason = move.reason
                val isNsbCandidate = move.isNsb ||
                    move.type == "KELUAR" ||
                    (move.type == "PENYESUAIAN" && move.quantity < 0) ||
                    reason.contains("Rusak", ignoreCase = true) ||
                    reason.contains("Expired", ignoreCase = true) ||
                    reason.contains("Kadaluarsa", ignoreCase = true) ||
                    reason.contains("Hilang", ignoreCase = true) ||
                    reason.contains("Pecah", ignoreCase = true) ||
                    reason.contains("Basi", ignoreCase = true) ||
                    reason.contains("Selisih", ignoreCase = true) ||
                    reason.contains("Opname", ignoreCase = true) ||
                    reason.contains("NSB", ignoreCase = true) ||
                    move.lossAmount > 0.0

                if (isNsbCandidate) {
                    var cost = move.costPrice
                    val p = productDao.getProductById(move.productId)
                    if (cost <= 0.0 && p != null) {
                        cost = if (p.buyPrice > 0.0) p.buyPrice else p.sellPrice
                    }
                    val qtyAbs = Math.abs(move.quantity)
                    val calculatedLoss = if (move.lossAmount > 0.0) move.lossAmount else qtyAbs * cost
                    val updated = move.copy(
                        isNsb = true,
                        costPrice = if (cost > 0.0) cost else move.costPrice,
                        lossAmount = calculatedLoss
                    )
                    stockMovementDao.updateMovement(updated)
                    syncManager?.pushStockMovement(updated)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Promotions
    suspend fun savePromotionFromCloud(promo: PromotionEntity) {
        promotionDao.insertPromotion(promo)
    }

    suspend fun deleteLocalPromotionOnly(promo: PromotionEntity) {
        promotionDao.deletePromotion(promo)
    }

    suspend fun insertPromotion(promo: PromotionEntity): Long {
        val id = promotionDao.insertPromotion(promo)
        val promoWithId = promo.copy(id = id)
        syncManager?.pushPromotion(promoWithId)
        return id
    }

    suspend fun updatePromotion(promo: PromotionEntity) {
        promotionDao.updatePromotion(promo)
        syncManager?.pushPromotion(promo)
    }

    suspend fun deletePromotion(promo: PromotionEntity) {
        promotionDao.deletePromotion(promo)
        syncManager?.deletePromotionFromCloud(promo)
    }

    suspend fun togglePromotion(id: Long, isActive: Boolean) {
        promotionDao.togglePromotionStatus(id, isActive)
        val all = promotionDao.getAllPromotionsDirect()
        val promo = all.find { it.id == id }
        if (promo != null) {
            syncManager?.pushPromotion(promo.copy(isActive = isActive))
        }
    }

    suspend fun getActivePromotionsList(): List<PromotionEntity> = promotionDao.getActivePromotionsList()
    suspend fun getAllPromotionsDirect(): List<PromotionEntity> = promotionDao.getAllPromotionsDirect()

    // Store Settings
    suspend fun updateStoreSettings(settings: StoreSettingEntity) {
        storeSettingDao.insertOrUpdate(settings)
        syncManager?.pushStoreSettings(settings)
    }

    suspend fun updateLocalStoreSettingsOnly(settings: StoreSettingEntity) {
        storeSettingDao.insertOrUpdate(settings)
    }

    suspend fun getStoreSettingsDirect(): StoreSettingEntity {
        return storeSettingDao.getStoreSettingsDirect() ?: StoreSettingEntity()
    }

    // User & Tenant operations
    suspend fun saveUser(user: UserEntity) {
        userDao.insertOrUpdate(user)
    }

    suspend fun getUser(uid: String): UserEntity? {
        return userDao.getUserById(uid)
    }

    suspend fun deleteUser(uid: String) {
        userDao.deleteUser(uid)
    }

    // Unsynced operations for Cloud Sync
    suspend fun getMovementsForInvoice(invoiceNumber: String): List<StockMovementEntity> = stockMovementDao.getMovementsForInvoice(invoiceNumber)

    suspend fun repairMissingTransactionItems() {
        val allTx = transactionDao.getAllTransactionsDirect()
        for (tx in allTx) {
            val items = transactionDao.getItemsForTransaction(tx.id)
            if (items.isEmpty() && tx.grandTotal > 0) {
                val movements = stockMovementDao.getMovementsForInvoice(tx.invoiceNumber)
                if (movements.isNotEmpty()) {
                    val reconstructed = movements.map { move ->
                        val prod = productDao.getProductById(move.productId)
                        val unitPrice = prod?.sellPrice ?: (move.costPrice * 1.2)
                        val qty = kotlin.math.abs(move.quantity).coerceAtLeast(1)
                        TransactionItemEntity(
                            id = 0,
                            transactionId = tx.id,
                            productId = move.productId,
                            barcode = prod?.barcode ?: "",
                            sku = prod?.sku ?: "",
                            productName = move.productName,
                            quantity = qty,
                            unitPrice = unitPrice,
                            costPrice = move.costPrice,
                            discountAmount = 0.0,
                            promoNote = "",
                            subtotal = unitPrice * qty
                        )
                    }
                    transactionDao.insertTransactionItems(reconstructed)
                    syncManager?.pushTransactionWithItems(tx, reconstructed)
                }
            }
        }
    }

    suspend fun getUnsyncedTransactions(): List<TransactionEntity> = transactionDao.getUnsyncedTransactions()
    suspend fun getUnsyncedMovements(): List<StockMovementEntity> = stockMovementDao.getUnsyncedMovements()
    suspend fun getUnsyncedProducts(): List<ProductEntity> = productDao.getUnsyncedProducts()
    suspend fun getItemsForTransaction(transactionId: Long): List<TransactionItemEntity> = transactionDao.getItemsForTransaction(transactionId)

    suspend fun markTransactionsSynced(ids: List<Long>, timestamp: Long) = transactionDao.markTransactionsSynced(ids, timestamp)
    suspend fun markMovementsSynced(ids: List<Long>) = stockMovementDao.markMovementsSynced(ids)
    suspend fun markProductsSynced(ids: List<Long>) = productDao.markProductsAsSynced(ids)
    suspend fun updateLastSyncTime(timestamp: Long) = storeSettingDao.updateLastSyncTime(timestamp)

    suspend fun clearLocalDatabase() {
        database.clearAllTables()
    }

    suspend fun clearAllCategories() {
        categoryDao.clearAllCategories()
    }

    suspend fun clearAllProducts() {
        productDao.clearAllProducts()
    }

    suspend fun clearAllPromotions() {
        promotionDao.clearAllPromotions()
    }

    suspend fun clearAllUnits() {
        unitDao.clearAllUnits()
    }

    suspend fun clearStoreSettings() {
        storeSettingDao.clearStoreSettings()
    }

    suspend fun clearAllProductsAndCategories() {
        productDao.clearAllProducts()
        categoryDao.clearAllCategories()
    }

    suspend fun clearAllTransactionsAndItems() {
        transactionDao.clearAllTransactions()
        transactionDao.clearAllTransactionItems()
    }
}
