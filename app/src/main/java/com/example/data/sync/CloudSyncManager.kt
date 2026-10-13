package com.example.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.PromotionEntity
import com.example.data.local.entity.StockMovementEntity
import com.example.data.local.entity.StoreSettingEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionItemEntity
import com.example.data.local.entity.UnitEntity
import com.example.data.repository.KasirRepository
import com.example.util.DataImportHelper
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    data class Success(val message: String, val syncedCount: Int, val timestamp: Long) : SyncState()
    data class Error(val errorMessage: String) : SyncState()
    object Offline : SyncState()
}

class CloudSyncManager(
    private val context: Context,
    private val repository: KasirRepository,
    private val scope: CoroutineScope
) {
    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow(System.currentTimeMillis())
    val lastSyncTimestamp: StateFlow<Long> = _lastSyncTimestamp.asStateFlow()

    private val activeListeners = mutableListOf<ListenerRegistration>()
    private var currentListeningUid: String? = null

    init {
        registerNetworkCallback()
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            val user = auth.currentUser
            if (user != null) {
                startRealtimeSync(user.uid)
                triggerAutoSync()
            } else {
                stopRealtimeSync()
            }
        }
        // Periodic background sync: every 30s, auto-sync when online and unsynced items exist in Room
        scope.launch(Dispatchers.IO) {
            while (true) {
                kotlinx.coroutines.delay(30_000)
                try {
                    if (_isOnline.value && FirebaseAuth.getInstance().currentUser != null) {
                        val pendingCount = repository.getUnsyncedTransactions().size +
                                           repository.getUnsyncedMovements().size +
                                           repository.getUnsyncedProducts().size
                        if (pendingCount > 0) {
                            syncDataToCloud()
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    private fun registerNetworkCallback() {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (connectivityManager == null) {
            _isOnline.value = true
            return
        }

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        val activeNetwork = connectivityManager.activeNetwork
        val caps = connectivityManager.getNetworkCapabilities(activeNetwork)
        val initialConnected = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        _isOnline.value = initialConnected

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                connectivityManager.registerDefaultNetworkCallback(
                    object : ConnectivityManager.NetworkCallback() {
                        override fun onAvailable(network: Network) {
                            _isOnline.value = true
                            triggerAutoSync()
                        }

                        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                            val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                            if (hasInternet && !_isOnline.value) {
                                _isOnline.value = true
                                triggerAutoSync()
                            }
                        }

                        override fun onLost(network: Network) {
                            _isOnline.value = false
                            _syncState.value = SyncState.Offline
                        }
                    }
                )
            } else {
                connectivityManager.registerNetworkCallback(
                    networkRequest,
                    object : ConnectivityManager.NetworkCallback() {
                        override fun onAvailable(network: Network) {
                            _isOnline.value = true
                            triggerAutoSync()
                        }

                        override fun onLost(network: Network) {
                            _isOnline.value = false
                            _syncState.value = SyncState.Offline
                        }
                    }
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun startRealtimeSync(uid: String) {
        if (currentListeningUid == uid && activeListeners.isNotEmpty()) return
        stopRealtimeSync()
        currentListeningUid = uid

        val firestore = FirebaseFirestore.getInstance()

        // Clean up legacy "stock" fields from all existing products in Firestore
        scope.launch(Dispatchers.IO) {
            try {
                val prods = firestore.collection("users").document(uid).collection("products").get().awaitTask()
                for (doc in prods.documents) {
                    if (doc.contains("stock")) {
                        try {
                            doc.reference.update("stock", FieldValue.delete())
                        } catch (_: Exception) {}
                    }
                }
            } catch (_: Exception) {}
        }

        // 1. Real-time Products Listener
        val productListener = firestore.collection("users").document(uid).collection("products")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                scope.launch(Dispatchers.IO) {
                    for (change in snapshot.documentChanges) {
                        val doc = change.document
                        val docId = doc.id
                        when (change.type) {
                            DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                val remoteUpdatedAt = doc.getLong("updatedAt") ?: 0L
                                val id = doc.getLong("id") ?: 0L
                                val rawSku = doc.getString("sku") ?: ""
                                val sku = when {
                                    rawSku.isNotBlank() -> rawSku
                                    docId.startsWith("SKU-") -> docId
                                    else -> DataImportHelper.generateSku("PRD", id.toInt().coerceAtLeast(1))
                                }
                                val barcode = doc.getString("barcode") ?: ""
                                val existing = if (id > 0) repository.getProductById(id)
                                    else if (barcode.isNotBlank()) repository.getProductByBarcode(barcode)
                                    else if (sku.isNotBlank()) repository.getProductBySku(sku)
                                    else if (docId.startsWith("PROD_")) docId.removePrefix("PROD_").toLongOrNull()?.let { repository.getProductById(it) }
                                    else null

                                if (existing != null) {
                                    // CRITICAL: Only update local product if remote is strictly newer!
                                    if (remoteUpdatedAt > existing.updatedAt) {
                                        val prod = existing.copy(
                                            barcode = if (barcode.isNotBlank()) barcode else existing.barcode,
                                            sku = if (sku.isNotBlank()) sku else existing.sku,
                                            name = doc.getString("name") ?: existing.name,
                                            categoryId = doc.getLong("categoryId") ?: existing.categoryId,
                                            buyPrice = doc.getDouble("buyPrice") ?: existing.buyPrice,
                                            sellPrice = doc.getDouble("sellPrice") ?: existing.sellPrice,
                                            minStock = doc.getLong("minStock")?.toInt() ?: existing.minStock,
                                            unit = doc.getString("unit") ?: existing.unit,
                                            updatedAt = remoteUpdatedAt,
                                            isSynced = true
                                        )
                                        repository.saveDirectProductFromCloud(prod)
                                    }
                                } else {
                                    val name = doc.getString("name") ?: ""
                                    val existingByName = if (name.isNotBlank()) {
                                        repository.getAllProductsDirect().firstOrNull { it.name.trim().equals(name.trim(), ignoreCase = true) }
                                    } else null

                                    if (existingByName != null) {
                                        if (remoteUpdatedAt > existingByName.updatedAt) {
                                            val prod = existingByName.copy(
                                                barcode = if (barcode.isNotBlank()) barcode else existingByName.barcode,
                                                sku = if (sku.isNotBlank()) sku else existingByName.sku,
                                                sellPrice = doc.getDouble("sellPrice") ?: existingByName.sellPrice,
                                                buyPrice = doc.getDouble("buyPrice") ?: existingByName.buyPrice,
                                                minStock = doc.getLong("minStock")?.toInt() ?: existingByName.minStock,
                                                unit = doc.getString("unit") ?: existingByName.unit,
                                                updatedAt = remoteUpdatedAt,
                                                isSynced = true
                                            )
                                            repository.saveDirectProductFromCloud(prod)
                                        }
                                    } else {
                                        val prod = ProductEntity(
                                            id = if (id > 0) id else 0L,
                                            barcode = barcode,
                                            sku = sku,
                                            name = if (name.isNotBlank()) name else "Produk",
                                            categoryId = doc.getLong("categoryId") ?: 0L,
                                            buyPrice = doc.getDouble("buyPrice") ?: 0.0,
                                            sellPrice = doc.getDouble("sellPrice") ?: 0.0,
                                            stock = 0, // Stock is solely populated from stock movements
                                            minStock = doc.getLong("minStock")?.toInt() ?: 5,
                                            unit = doc.getString("unit") ?: "pcs",
                                            updatedAt = remoteUpdatedAt,
                                            isSynced = true
                                        )
                                        repository.saveDirectProductFromCloud(prod)
                                    }
                                }

                                // Delete legacy 'stock' field on Firestore product doc if still present
                                if (doc.contains("stock")) {
                                    try {
                                        doc.reference.update("stock", FieldValue.delete())
                                    } catch (_: Exception) {}
                                }
                            }
                            DocumentChange.Type.REMOVED -> {
                                val id = doc.getLong("id") ?: 0L
                                val barcode = doc.getString("barcode") ?: ""
                                val rawSku = doc.getString("sku") ?: ""
                                val existing = if (id > 0) repository.getProductById(id)
                                    else if (barcode.isNotBlank()) repository.getProductByBarcode(barcode)
                                    else null
                                if (existing != null) {
                                    repository.deleteLocalProductOnly(existing)
                                }
                            }
                        }
                    }
                }
            }
        activeListeners.add(productListener)

        // 2. Real-time Categories Listener
        val categoryListener = firestore.collection("users").document(uid).collection("categories")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                scope.launch(Dispatchers.IO) {
                    for (change in snapshot.documentChanges) {
                        val doc = change.document
                        val id = doc.getLong("id") ?: 0L
                        val name = doc.getString("name") ?: ""
                        val colorHex = doc.getString("colorHex") ?: "#006C4C"
                        if (name.isBlank()) continue
                        when (change.type) {
                            DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                repository.saveCategoryFromCloud(CategoryEntity(id = id, name = name, colorHex = colorHex))
                            }
                            DocumentChange.Type.REMOVED -> {
                                repository.deleteLocalCategoryOnly(CategoryEntity(id = id, name = name, colorHex = colorHex))
                            }
                        }
                    }
                }
            }
        activeListeners.add(categoryListener)

        // 3. Real-time Store Settings Listener
        val settingsListener = firestore.collection("users").document(uid).collection("settings").document("store")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                scope.launch(Dispatchers.IO) {
                    val uType = snapshot.getString("userType") ?: "admin"
                    val tType = snapshot.getString("tenantType") ?: uType
                    val rRole = snapshot.getString("role") ?: uType
                    val sName = snapshot.getString("storeName") ?: "Toko Kelontong Berkah"

                    val settings = StoreSettingEntity(
                        id = 1,
                        storeName = sName,
                        slogan = snapshot.getString("slogan") ?: "",
                        address = snapshot.getString("address") ?: "",
                        phoneNumber = snapshot.getString("phoneNumber") ?: "",
                        receiptHeader = snapshot.getString("receiptHeader") ?: "SELAMAT BERBELANJA",
                        receiptFooter = snapshot.getString("receiptFooter") ?: "",
                        printerPaperSize = snapshot.getString("printerPaperSize") ?: "58mm",
                        isTaxEnabledByDefault = snapshot.getBoolean("isTaxEnabledByDefault") ?: false,
                        taxRatePercent = snapshot.getDouble("taxRatePercent") ?: 11.0,
                        qrisMerchantName = snapshot.getString("qrisMerchantName") ?: "",
                        qrisNmid = snapshot.getString("qrisNmid") ?: "",
                        userType = uType,
                        tenantType = tType,
                        role = rRole
                    )
                    repository.updateLocalStoreSettingsOnly(settings)
                    val email = FirebaseAuth.getInstance().currentUser?.email ?: ""
                    repository.saveUser(
                        com.example.data.local.entity.UserEntity(
                            uid = uid,
                            email = email,
                            name = sName,
                            userType = uType,
                            tenantType = tType,
                            role = rRole,
                            storeName = sName,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
            }
        activeListeners.add(settingsListener)

        // 4. Real-time Stock Movements Listener (Synchronize additions & deletions)
        val movementListener = firestore.collection("users").document(uid).collection("stock_movements")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                scope.launch(Dispatchers.IO) {
                    for (change in snapshot.documentChanges) {
                        val doc = change.document
                        val id = doc.getLong("id") ?: 0L
                        val productId = doc.getLong("productId") ?: 0L
                        val timestamp = doc.getLong("timestamp") ?: 0L

                        when (change.type) {
                            DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                val reasonStr = doc.getString("reason") ?: ""
                                val typeStr = doc.getString("type") ?: "PENYESUAIAN"
                                val rawLoss = doc.getDouble("lossAmount") ?: 0.0
                                val rawIsNsb = doc.getBoolean("isNsb") ?: false
                                val rawCost = doc.getDouble("costPrice") ?: 0.0
                                val qtyVal = doc.getLong("quantity")?.toInt() ?: 0

                                val isNsbCalc = rawIsNsb ||
                                    rawLoss > 0.0 ||
                                    reasonStr.contains("Rusak", ignoreCase = true) ||
                                    reasonStr.contains("Expired", ignoreCase = true) ||
                                    reasonStr.contains("Kadaluarsa", ignoreCase = true) ||
                                    reasonStr.contains("Hilang", ignoreCase = true) ||
                                    reasonStr.contains("Pecah", ignoreCase = true) ||
                                    reasonStr.contains("Basi", ignoreCase = true) ||
                                    reasonStr.contains("Selisih", ignoreCase = true) ||
                                    reasonStr.contains("NSB", ignoreCase = true) ||
                                    typeStr == "KELUAR" ||
                                    (typeStr == "PENYESUAIAN" && qtyVal < 0)

                                val lossCalc = if (rawLoss > 0.0) rawLoss else if (isNsbCalc) Math.abs(qtyVal) * rawCost else 0.0

                                val movement = StockMovementEntity(
                                    id = id,
                                    productId = productId,
                                    productName = doc.getString("productName") ?: "",
                                    type = typeStr,
                                    quantity = qtyVal,
                                    previousStock = doc.getLong("previousStock")?.toInt() ?: 0,
                                    currentStock = doc.getLong("currentStock")?.toInt() ?: 0,
                                    reason = reasonStr,
                                    notes = doc.getString("notes") ?: "",
                                    costPrice = rawCost,
                                    isNsb = isNsbCalc,
                                    lossAmount = lossCalc,
                                    timestamp = timestamp,
                                    isSynced = true
                                )
                                repository.saveStockMovementFromCloud(movement)
                            }
                            DocumentChange.Type.REMOVED -> {
                                if (productId > 0 && timestamp > 0) {
                                    repository.deleteMovementByProductAndTimestamp(productId, timestamp)
                                }
                                if (id > 0) {
                                    repository.deleteLocalStockMovementById(id)
                                }
                            }
                        }
                    }
                }
            }
        activeListeners.add(movementListener)

        // 5. Real-time Units Listener
        val unitsListener = firestore.collection("users").document(uid).collection("units")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                scope.launch(Dispatchers.IO) {
                    for (change in snapshot.documentChanges) {
                        val doc = change.document
                        val id = doc.getLong("id") ?: 0L
                        val name = doc.getString("name") ?: ""
                        val isDefault = doc.getBoolean("isDefault") ?: false
                        if (name.isBlank()) continue
                        when (change.type) {
                            DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                repository.saveUnitFromCloud(UnitEntity(id = id, name = name, isDefault = isDefault))
                            }
                            DocumentChange.Type.REMOVED -> {
                                repository.deleteLocalUnitOnly(UnitEntity(id = id, name = name, isDefault = isDefault))
                            }
                        }
                    }
                }
            }
        activeListeners.add(unitsListener)

        // 6. Real-time Transactions Listener (Sales invoices)
        val transactionsListener = firestore.collection("users").document(uid).collection("transactions")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                scope.launch(Dispatchers.IO) {
                    for (change in snapshot.documentChanges) {
                        val doc = change.document
                        val invoiceNumber = doc.getString("invoiceNumber") ?: doc.id
                        if (invoiceNumber.isBlank()) continue

                        when (change.type) {
                            DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                val tx = TransactionEntity(
                                    id = doc.getLong("id") ?: 0L,
                                    invoiceNumber = invoiceNumber,
                                    timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                                    subtotal = doc.getDouble("subtotal") ?: 0.0,
                                    discountAmount = doc.getDouble("discountAmount") ?: 0.0,
                                    discountNote = doc.getString("discountNote") ?: "",
                                    taxAmount = doc.getDouble("taxAmount") ?: 0.0,
                                    taxPercent = doc.getDouble("taxPercent") ?: 0.0,
                                    isTaxApplied = doc.getBoolean("isTaxApplied") ?: false,
                                    grandTotal = doc.getDouble("grandTotal") ?: 0.0,
                                    paymentMethod = doc.getString("paymentMethod") ?: "TUNAI",
                                    cashReceived = doc.getDouble("cashReceived") ?: 0.0,
                                    changeAmount = doc.getDouble("changeAmount") ?: 0.0,
                                    cashierName = doc.getString("cashierName") ?: "Kasir",
                                    customerName = doc.getString("customerName") ?: "Pelanggan Umum",
                                    isSynced = true,
                                    syncTimestamp = System.currentTimeMillis()
                                )

                                val rawItems = doc.get("items") as? List<Map<String, Any>> ?: emptyList()
                                val items = rawItems.map { itemMap ->
                                    TransactionItemEntity(
                                        id = (itemMap["id"] as? Number)?.toLong() ?: 0L,
                                        transactionId = tx.id,
                                        productId = (itemMap["productId"] as? Number)?.toLong() ?: 0L,
                                        barcode = itemMap["barcode"] as? String ?: "",
                                        sku = itemMap["sku"] as? String ?: "",
                                        productName = itemMap["productName"] as? String ?: "",
                                        quantity = (itemMap["quantity"] as? Number)?.toInt() ?: 1,
                                        unitPrice = (itemMap["unitPrice"] as? Number)?.toDouble() ?: 0.0,
                                        costPrice = (itemMap["costPrice"] as? Number)?.toDouble() ?: 0.0,
                                        discountAmount = (itemMap["discountAmount"] as? Number)?.toDouble() ?: 0.0,
                                        promoNote = itemMap["promoNote"] as? String ?: "",
                                        subtotal = (itemMap["subtotal"] as? Number)?.toDouble() ?: 0.0
                                    )
                                }
                                repository.saveTransactionWithItemsFromCloud(tx, items)
                            }
                            DocumentChange.Type.REMOVED -> {
                                repository.deleteLocalTransactionByInvoice(invoiceNumber)
                            }
                        }
                    }
                }
            }
        activeListeners.add(transactionsListener)

        // 7. Real-time Promotions Listener
        val promotionsListener = firestore.collection("users").document(uid).collection("promotions")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                scope.launch(Dispatchers.IO) {
                    for (change in snapshot.documentChanges) {
                        val doc = change.document
                        val id = doc.getLong("id") ?: 0L
                        val name = doc.getString("name") ?: ""
                        if (id == 0L && name.isBlank()) continue

                        val promo = PromotionEntity(
                            id = id,
                            name = name,
                            promoScheme = doc.getString("promoScheme") ?: "CATEGORY",
                            type = doc.getString("type") ?: "PERCENTAGE",
                            value = doc.getDouble("value") ?: 0.0,
                            targetType = doc.getString("targetType") ?: "SPECIFIC_PRODUCT",
                            targetId = doc.getLong("targetId") ?: 0L,
                            targetName = doc.getString("targetName") ?: "",
                            minPurchaseAmount = doc.getDouble("minPurchaseAmount") ?: 0.0,
                            isActive = doc.getBoolean("isActive") ?: true,
                            note = doc.getString("note") ?: "",
                            minQuantity = doc.getLong("minQuantity")?.toInt() ?: 1,
                            startHour = doc.getLong("startHour")?.toInt() ?: 0,
                            endHour = doc.getLong("endHour")?.toInt() ?: 24,
                            activeDays = doc.getString("activeDays") ?: "",
                            tierRules = doc.getString("tierRules") ?: "",
                            multiProductIds = doc.getString("multiProductIds") ?: "",
                            multiProductNames = doc.getString("multiProductNames") ?: "",
                            triggerProductId = doc.getLong("triggerProductId") ?: 0L,
                            triggerProductName = doc.getString("triggerProductName") ?: "",
                            triggerMinQty = doc.getLong("triggerMinQty")?.toInt() ?: 1,
                            rewardProductId = doc.getLong("rewardProductId") ?: 0L,
                            rewardProductName = doc.getString("rewardProductName") ?: "",
                            rewardSpecialPrice = doc.getDouble("rewardSpecialPrice") ?: 0.0
                        )

                        when (change.type) {
                            DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                repository.savePromotionFromCloud(promo)
                            }
                            DocumentChange.Type.REMOVED -> {
                                repository.deleteLocalPromotionOnly(promo)
                            }
                        }
                    }
                }
            }
        activeListeners.add(promotionsListener)

        // Initial trigger
        triggerAutoSync()
    }

    fun stopRealtimeSync() {
        for (listener in activeListeners) {
            try {
                listener.remove()
            } catch (_: Exception) {}
        }
        activeListeners.clear()
        currentListeningUid = null
    }

    fun triggerAutoSync() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        scope.launch(Dispatchers.IO) {
            syncDataToCloud()
        }
    }

    private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            if (task.isSuccessful) {
                continuation.resume(task.result)
            } else {
                continuation.resumeWithException(task.exception ?: RuntimeException("Task failed"))
            }
        }
    }

    // Direct Instant Push Methods
    fun pushTransactionWithItems(tx: TransactionEntity, items: List<TransactionItemEntity>) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        scope.launch(Dispatchers.IO) {
            try {
                var resolvedItems = items
                if (resolvedItems.isEmpty()) {
                    val movements = repository.getMovementsForInvoice(tx.invoiceNumber)
                    if (movements.isNotEmpty()) {
                        resolvedItems = movements.map { move ->
                            val prod = repository.getProductById(move.productId)
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
                    }
                }

                // CRITICAL SAFETY: Never push an empty items array for a transaction with non-zero sales!
                if (resolvedItems.isEmpty() && tx.grandTotal > 0) {
                    return@launch
                }

                val firestore = FirebaseFirestore.getInstance()
                val txMap = hashMapOf(
                    "id" to tx.id,
                    "invoiceNumber" to tx.invoiceNumber,
                    "timestamp" to tx.timestamp,
                    "subtotal" to tx.subtotal,
                    "discountAmount" to tx.discountAmount,
                    "discountNote" to tx.discountNote,
                    "taxAmount" to tx.taxAmount,
                    "taxPercent" to tx.taxPercent,
                    "isTaxApplied" to tx.isTaxApplied,
                    "grandTotal" to tx.grandTotal,
                    "paymentMethod" to tx.paymentMethod,
                    "cashReceived" to tx.cashReceived,
                    "changeAmount" to tx.changeAmount,
                    "cashierName" to tx.cashierName,
                    "customerName" to tx.customerName,
                    "items" to resolvedItems.map { item ->
                        hashMapOf(
                            "id" to item.id,
                            "productId" to item.productId,
                            "productName" to item.productName,
                            "barcode" to item.barcode,
                            "sku" to item.sku,
                            "quantity" to item.quantity,
                            "unitPrice" to item.unitPrice,
                            "costPrice" to item.costPrice,
                            "discountAmount" to item.discountAmount,
                            "promoNote" to item.promoNote,
                            "subtotal" to item.subtotal
                        )
                    }
                )

                firestore.collection("users").document(uid).collection("transactions")
                    .document(tx.invoiceNumber)
                    .set(txMap, com.google.firebase.firestore.SetOptions.merge())
                    .awaitTask()

                repository.markTransactionsSynced(listOf(tx.id), System.currentTimeMillis())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun pushProduct(product: ProductEntity) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        scope.launch(Dispatchers.IO) {
            try {
                val firestore = FirebaseFirestore.getInstance()
                val docId = product.sku.ifBlank { if (product.barcode.isNotBlank()) product.barcode else "PROD_${product.id}" }
                val prodMap = hashMapOf<String, Any>(
                    "id" to product.id,
                    "barcode" to product.barcode,
                    "sku" to product.sku,
                    "name" to product.name,
                    "categoryId" to product.categoryId,
                    "buyPrice" to product.buyPrice,
                    "sellPrice" to product.sellPrice,
                    "minStock" to product.minStock,
                    "unit" to product.unit,
                    "updatedAt" to product.updatedAt
                )
                val prodDocRef = firestore.collection("users").document(uid).collection("products").document(docId)
                prodDocRef.set(prodMap, com.google.firebase.firestore.SetOptions.merge()).awaitTask()
                try {
                    // Remove legacy "stock" field from product doc in Firestore so stock is solely derived from mutasi
                    prodDocRef.update("stock", FieldValue.delete()).awaitTask()
                } catch (_: Exception) {}

                // Clean up legacy conflicting document IDs if present
                if (docId != "PROD_${product.id}") {
                    try {
                        firestore.collection("users").document(uid).collection("products")
                            .document("PROD_${product.id}")
                            .delete()
                    } catch (_: Exception) {}
                }
                if (product.barcode.isNotBlank() && docId != product.barcode) {
                    try {
                        firestore.collection("users").document(uid).collection("products")
                            .document(product.barcode)
                            .delete()
                    } catch (_: Exception) {}
                }

                repository.markProductsSynced(listOf(product.id))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteProductFromCloud(product: ProductEntity) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        scope.launch(Dispatchers.IO) {
            try {
                val firestore = FirebaseFirestore.getInstance()
                val docId = product.sku.ifBlank { "PROD_${product.id}" }
                firestore.collection("users").document(uid).collection("products")
                    .document(docId)
                    .delete()
                    .awaitTask()
                if (product.barcode.isNotBlank() && product.barcode != docId) {
                    firestore.collection("users").document(uid).collection("products")
                        .document(product.barcode)
                        .delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun pushCategory(category: CategoryEntity) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        scope.launch(Dispatchers.IO) {
            try {
                val firestore = FirebaseFirestore.getInstance()
                val docId = "CAT_${category.id}"
                val catMap = hashMapOf(
                    "id" to category.id,
                    "name" to category.name,
                    "colorHex" to category.colorHex
                )
                firestore.collection("users").document(uid).collection("categories")
                    .document(docId)
                    .set(catMap)
                    .awaitTask()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteCategoryFromCloud(category: CategoryEntity) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        scope.launch(Dispatchers.IO) {
            try {
                val firestore = FirebaseFirestore.getInstance()
                val docId = "CAT_${category.id}"
                firestore.collection("users").document(uid).collection("categories")
                    .document(docId)
                    .delete()
                    .awaitTask()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun pushPromotion(promotion: PromotionEntity) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        scope.launch(Dispatchers.IO) {
            try {
                val firestore = FirebaseFirestore.getInstance()
                val docId = "PROMO_${promotion.id}"
                val promoMap = hashMapOf(
                    "id" to promotion.id,
                    "name" to promotion.name,
                    "promoScheme" to promotion.promoScheme,
                    "type" to promotion.type,
                    "value" to promotion.value,
                    "targetType" to promotion.targetType,
                    "targetId" to promotion.targetId,
                    "targetName" to promotion.targetName,
                    "isActive" to promotion.isActive,
                    "minPurchaseAmount" to promotion.minPurchaseAmount,
                    "tierRules" to promotion.tierRules,
                    "multiProductIds" to promotion.multiProductIds,
                    "multiProductNames" to promotion.multiProductNames,
                    "startHour" to promotion.startHour,
                    "endHour" to promotion.endHour,
                    "activeDays" to promotion.activeDays,
                    "triggerProductId" to promotion.triggerProductId,
                    "triggerProductName" to promotion.triggerProductName,
                    "rewardProductId" to promotion.rewardProductId,
                    "rewardProductName" to promotion.rewardProductName,
                    "rewardSpecialPrice" to promotion.rewardSpecialPrice,
                    "note" to promotion.note
                )
                firestore.collection("users").document(uid).collection("promotions")
                    .document(docId)
                    .set(promoMap)
                    .awaitTask()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deletePromotionFromCloud(promotion: PromotionEntity) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        scope.launch(Dispatchers.IO) {
            try {
                val firestore = FirebaseFirestore.getInstance()
                val docId = "PROMO_${promotion.id}"
                firestore.collection("users").document(uid).collection("promotions")
                    .document(docId)
                    .delete()
                    .awaitTask()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun pushStockMovement(movement: StockMovementEntity) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        scope.launch(Dispatchers.IO) {
            try {
                val firestore = FirebaseFirestore.getInstance()
                val docId = "MOVE_${movement.productId}_${movement.timestamp}"
                val moveMap = hashMapOf(
                    "id" to movement.id,
                    "productId" to movement.productId,
                    "productName" to movement.productName,
                    "type" to movement.type,
                    "quantity" to movement.quantity,
                    "previousStock" to movement.previousStock,
                    "currentStock" to movement.currentStock,
                    "reason" to movement.reason,
                    "notes" to movement.notes,
                    "costPrice" to movement.costPrice,
                    "isNsb" to movement.isNsb,
                    "lossAmount" to movement.lossAmount,
                    "timestamp" to movement.timestamp
                )
                firestore.collection("users").document(uid).collection("stock_movements")
                    .document(docId)
                    .set(moveMap)
                    .awaitTask()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteStockMovementFromCloud(movement: StockMovementEntity) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        scope.launch(Dispatchers.IO) {
            try {
                val firestore = FirebaseFirestore.getInstance()
                val docId = "MOVE_${movement.productId}_${movement.timestamp}"
                firestore.collection("users").document(uid).collection("stock_movements")
                    .document(docId)
                    .delete()
                    .awaitTask()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun pushUnit(unit: UnitEntity) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        scope.launch(Dispatchers.IO) {
            try {
                val firestore = FirebaseFirestore.getInstance()
                val docId = "UNIT_${unit.id}"
                val unitMap = hashMapOf(
                    "id" to unit.id,
                    "name" to unit.name.trim(),
                    "isDefault" to unit.isDefault
                )
                firestore.collection("users").document(uid).collection("units")
                    .document(docId)
                    .set(unitMap)
                    .awaitTask()

                // Auto-delete any older name-based documents that have this same unit id to prevent duplicates
                val snapshot = firestore.collection("users").document(uid).collection("units")
                    .whereEqualTo("id", unit.id)
                    .get()
                    .awaitTask()
                for (doc in snapshot.documents) {
                    if (doc.id != docId) {
                        firestore.collection("users").document(uid).collection("units")
                            .document(doc.id)
                            .delete()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteUnitFromCloud(unit: UnitEntity) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        scope.launch(Dispatchers.IO) {
            try {
                val firestore = FirebaseFirestore.getInstance()
                val docId = "UNIT_${unit.id}"
                firestore.collection("users").document(uid).collection("units")
                    .document(docId)
                    .delete()
                    .awaitTask()

                // Also delete old name-based document if any
                val legacyDocId = "UNIT_${unit.name.lowercase().trim().replace(" ", "_")}"
                firestore.collection("users").document(uid).collection("units")
                    .document(legacyDocId)
                    .delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun pushStoreSettings(settings: StoreSettingEntity) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        scope.launch(Dispatchers.IO) {
            try {
                val firestore = FirebaseFirestore.getInstance()
                val settingsMap = hashMapOf(
                    "storeName" to settings.storeName,
                    "slogan" to settings.slogan,
                    "address" to settings.address,
                    "phoneNumber" to settings.phoneNumber,
                    "receiptHeader" to settings.receiptHeader,
                    "receiptFooter" to settings.receiptFooter,
                    "printerPaperSize" to settings.printerPaperSize,
                    "isTaxEnabledByDefault" to settings.isTaxEnabledByDefault,
                    "taxRatePercent" to settings.taxRatePercent,
                    "qrisMerchantName" to settings.qrisMerchantName,
                    "qrisNmid" to settings.qrisNmid,
                    "userType" to settings.userType,
                    "tenantType" to settings.tenantType,
                    "role" to settings.role
                )
                firestore.collection("users").document(uid).collection("settings").document("store")
                    .set(settingsMap, com.google.firebase.firestore.SetOptions.merge())
                    .awaitTask()

                val email = FirebaseAuth.getInstance().currentUser?.email ?: ""
                val tenantProfile = hashMapOf(
                    "uid" to uid,
                    "email" to email,
                    "userType" to settings.userType,
                    "tenantType" to settings.tenantType,
                    "role" to settings.role,
                    "storeName" to settings.storeName,
                    "updatedAt" to System.currentTimeMillis()
                )
                firestore.collection("users").document(uid)
                    .set(tenantProfile, com.google.firebase.firestore.SetOptions.merge())
                    .awaitTask()

                repository.saveUser(
                    com.example.data.local.entity.UserEntity(
                        uid = uid,
                        email = email,
                        name = settings.storeName,
                        userType = settings.userType,
                        tenantType = settings.tenantType,
                        role = settings.role,
                        storeName = settings.storeName,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun syncDataToCloud(): Boolean {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: run {
            _syncState.value = SyncState.Error("Kasir belum login")
            return false
        }
        if (!_isOnline.value) {
            _syncState.value = SyncState.Offline
            return false
        }
        val uid = currentUser.uid
        _syncState.value = SyncState.Syncing

        return try {
            val unsyncedTx = repository.getUnsyncedTransactions()
            val unsyncedMovements = repository.getUnsyncedMovements()
            val unsyncedProducts = repository.getUnsyncedProducts()
            val allCategories = repository.getAllCategoriesDirect()
            val allPromotions = repository.getAllPromotionsDirect()

            val firestore = FirebaseFirestore.getInstance()
            val now = System.currentTimeMillis()

            // 1. Sync Products (Push all products to ensure cloud is never empty)
            val allProducts = repository.getAllProductsDirect()
            val productsToPush = if (unsyncedProducts.isNotEmpty()) unsyncedProducts else allProducts
            if (productsToPush.isNotEmpty()) {
                val batch = firestore.batch()
                for (product in productsToPush) {
                    val docId = product.sku.ifBlank { if (product.barcode.isNotBlank()) product.barcode else "PROD_${product.id}" }
                    val prodRef = firestore.collection("users").document(uid).collection("products").document(docId)
                    val prodMap = hashMapOf<String, Any>(
                        "id" to product.id,
                        "barcode" to product.barcode,
                        "sku" to product.sku,
                        "name" to product.name,
                        "categoryId" to product.categoryId,
                        "buyPrice" to product.buyPrice,
                        "sellPrice" to product.sellPrice,
                        "minStock" to product.minStock,
                        "unit" to product.unit,
                        "updatedAt" to product.updatedAt,
                        "stock" to FieldValue.delete()
                    )
                    batch.set(prodRef, prodMap, com.google.firebase.firestore.SetOptions.merge())
                }
                batch.commit().awaitTask()
                repository.markProductsSynced(productsToPush.map { it.id })
            }

            // 2. Sync Categories
            if (allCategories.isNotEmpty()) {
                val batch = firestore.batch()
                for (cat in allCategories) {
                    val catRef = firestore.collection("users").document(uid).collection("categories").document("CAT_${cat.id}")
                    val catMap = hashMapOf(
                        "id" to cat.id,
                        "name" to cat.name,
                        "colorHex" to cat.colorHex
                    )
                    batch.set(catRef, catMap, com.google.firebase.firestore.SetOptions.merge())
                }
                batch.commit().awaitTask()
            }

            // 3. Sync Promotions
            if (allPromotions.isNotEmpty()) {
                val batch = firestore.batch()
                for (promo in allPromotions) {
                    val promoRef = firestore.collection("users").document(uid).collection("promotions").document("PROMO_${promo.id}")
                    val promoMap = hashMapOf(
                        "id" to promo.id,
                        "name" to promo.name,
                        "promoScheme" to promo.promoScheme,
                        "type" to promo.type,
                        "value" to promo.value,
                        "targetType" to promo.targetType,
                        "targetId" to promo.targetId,
                        "targetName" to promo.targetName,
                        "isActive" to promo.isActive,
                        "minPurchaseAmount" to promo.minPurchaseAmount,
                        "tierRules" to promo.tierRules,
                        "multiProductIds" to promo.multiProductIds,
                        "multiProductNames" to promo.multiProductNames,
                        "startHour" to promo.startHour,
                        "endHour" to promo.endHour,
                        "activeDays" to promo.activeDays,
                        "triggerProductId" to promo.triggerProductId,
                        "triggerProductName" to promo.triggerProductName,
                        "rewardProductId" to promo.rewardProductId,
                        "rewardProductName" to promo.rewardProductName,
                        "rewardSpecialPrice" to promo.rewardSpecialPrice,
                        "note" to promo.note
                    )
                    batch.set(promoRef, promoMap)
                }
                batch.commit().awaitTask()
            }

            // 3.5 Sync Units
            val allUnits = repository.getAllUnitsDirect()
            if (allUnits.isNotEmpty()) {
                val batch = firestore.batch()
                for (unit in allUnits) {
                    val docId = "UNIT_${unit.id}"
                    val unitRef = firestore.collection("users").document(uid).collection("units").document(docId)
                    val unitMap = hashMapOf(
                        "id" to unit.id,
                        "name" to unit.name.trim(),
                        "isDefault" to unit.isDefault
                    )
                    batch.set(unitRef, unitMap)
                }
                batch.commit().awaitTask()

                // Optional: Clean up old legacy name-based unit documents during bulk sync
                try {
                    val snapshot = firestore.collection("users").document(uid).collection("units").get().awaitTask()
                    for (doc in snapshot.documents) {
                        val id = doc.getLong("id")
                        if (id != null) {
                            val expectedId = "UNIT_$id"
                            if (doc.id != expectedId && doc.id.startsWith("UNIT_")) {
                                firestore.collection("users").document(uid).collection("units")
                                    .document(doc.id)
                                    .delete()
                            }
                        }
                    }
                } catch (_: Exception) {}
            }

            // 4. Sync Stock Movements
            if (unsyncedMovements.isNotEmpty()) {
                val batch = firestore.batch()
                for (movement in unsyncedMovements) {
                    val moveRef = firestore.collection("users").document(uid).collection("stock_movements")
                        .document("MOVE_${movement.productId}_${movement.timestamp}")
                    val moveMap = hashMapOf(
                        "id" to movement.id,
                        "productId" to movement.productId,
                        "productName" to movement.productName,
                        "type" to movement.type,
                        "quantity" to movement.quantity,
                        "previousStock" to movement.previousStock,
                        "currentStock" to movement.currentStock,
                        "reason" to movement.reason,
                        "notes" to movement.notes,
                        "costPrice" to movement.costPrice,
                        "isNsb" to movement.isNsb,
                        "lossAmount" to movement.lossAmount,
                        "timestamp" to movement.timestamp
                    )
                    batch.set(moveRef, moveMap)
                }
                batch.commit().awaitTask()
                repository.markMovementsSynced(unsyncedMovements.map { it.id })
            }

            // 5. Sync Transactions
            repository.repairMissingTransactionItems()
            if (unsyncedTx.isNotEmpty()) {
                val syncedTxIds = mutableListOf<Long>()
                for (tx in unsyncedTx) {
                    try {
                        var items = repository.getItemsForTransaction(tx.id)
                        if (items.isEmpty()) {
                            items = repository.getTransactionByInvoice(tx.invoiceNumber)?.items ?: emptyList()
                        }
                        if (items.isEmpty()) {
                            val movements = repository.getMovementsForInvoice(tx.invoiceNumber)
                            if (movements.isNotEmpty()) {
                                items = movements.map { move ->
                                    val prod = repository.getProductById(move.productId)
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
                            }
                        }

                        // CRITICAL: NEVER push an empty items list for a transaction with non-zero sales!
                        if (items.isEmpty() && tx.grandTotal > 0) {
                            continue
                        }

                        val txMap = hashMapOf(
                            "id" to tx.id,
                            "invoiceNumber" to tx.invoiceNumber,
                            "timestamp" to tx.timestamp,
                            "subtotal" to tx.subtotal,
                            "discountAmount" to tx.discountAmount,
                            "discountNote" to tx.discountNote,
                            "taxAmount" to tx.taxAmount,
                            "taxPercent" to tx.taxPercent,
                            "isTaxApplied" to tx.isTaxApplied,
                            "grandTotal" to tx.grandTotal,
                            "paymentMethod" to tx.paymentMethod,
                            "cashReceived" to tx.cashReceived,
                            "changeAmount" to tx.changeAmount,
                            "cashierName" to tx.cashierName,
                            "customerName" to tx.customerName,
                            "items" to items.map { item ->
                                hashMapOf(
                                    "id" to item.id,
                                    "productId" to item.productId,
                                    "productName" to item.productName,
                                    "barcode" to item.barcode,
                                    "sku" to item.sku,
                                    "quantity" to item.quantity,
                                    "unitPrice" to item.unitPrice,
                                    "costPrice" to item.costPrice,
                                    "discountAmount" to item.discountAmount,
                                    "promoNote" to item.promoNote,
                                    "subtotal" to item.subtotal
                                )
                            }
                        )
                        firestore.collection("users").document(uid).collection("transactions")
                            .document(tx.invoiceNumber)
                            .set(txMap, com.google.firebase.firestore.SetOptions.merge())
                            .awaitTask()
                        syncedTxIds.add(tx.id)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                if (syncedTxIds.isNotEmpty()) {
                    repository.markTransactionsSynced(syncedTxIds, now)
                }
            }

            // 6. Sync Store Settings
            val storeSettings = repository.getStoreSettingsDirect()
            val settingsMap = hashMapOf(
                "storeName" to storeSettings.storeName,
                "slogan" to storeSettings.slogan,
                "address" to storeSettings.address,
                "phoneNumber" to storeSettings.phoneNumber,
                "receiptHeader" to storeSettings.receiptHeader,
                "receiptFooter" to storeSettings.receiptFooter,
                "printerPaperSize" to storeSettings.printerPaperSize,
                "isTaxEnabledByDefault" to storeSettings.isTaxEnabledByDefault,
                "taxRatePercent" to storeSettings.taxRatePercent,
                "qrisMerchantName" to storeSettings.qrisMerchantName,
                "qrisNmid" to storeSettings.qrisNmid,
                "userType" to storeSettings.userType,
                "tenantType" to storeSettings.tenantType,
                "role" to storeSettings.role
            )
            firestore.collection("users").document(uid).collection("settings").document("store")
                .set(settingsMap, com.google.firebase.firestore.SetOptions.merge())
                .awaitTask()

            val email = currentUser.email ?: ""
            val tenantProfile = hashMapOf(
                "uid" to uid,
                "email" to email,
                "userType" to storeSettings.userType,
                "tenantType" to storeSettings.tenantType,
                "role" to storeSettings.role,
                "storeName" to storeSettings.storeName,
                "updatedAt" to now
            )
            firestore.collection("users").document(uid)
                .set(tenantProfile, com.google.firebase.firestore.SetOptions.merge())
                .awaitTask()

            repository.updateLastSyncTime(now)
            _lastSyncTimestamp.value = now

            val totalSynced = unsyncedProducts.size + unsyncedMovements.size + unsyncedTx.size
            _syncState.value = SyncState.Success(
                message = if (totalSynced > 0) "$totalSynced data berhasil disinkronkan ke Cloud" else "Cloud real-time aktif & mutakhir",
                syncedCount = totalSynced,
                timestamp = now
            )
            true
        } catch (e: Exception) {
            e.printStackTrace()
            _syncState.value = SyncState.Error(e.message ?: "Gagal sinkronisasi cloud")
            false
        }
    }

    // Pull tenant store's existing data upon login
    suspend fun pullDataFromCloud(): Boolean {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return false
        val uid = currentUser.uid
        val firestore = FirebaseFirestore.getInstance()

        _syncState.value = SyncState.Syncing

        return try {
            // 1. Pull Settings & Tenant Profile
            val settingsSnap = firestore.collection("users").document(uid).collection("settings").document("store").get().awaitTask()
            val userProfileSnap = firestore.collection("users").document(uid).get().awaitTask()

            val fetchedUserType = settingsSnap.getString("userType")
                ?: userProfileSnap.getString("userType")
                ?: "admin"
            val fetchedTenantType = settingsSnap.getString("tenantType")
                ?: userProfileSnap.getString("tenantType")
                ?: fetchedUserType
            val fetchedRole = settingsSnap.getString("role")
                ?: userProfileSnap.getString("role")
                ?: fetchedUserType

            if (settingsSnap.exists() || userProfileSnap.exists()) {
                val storeNameVal = settingsSnap.getString("storeName")
                    ?: userProfileSnap.getString("storeName")
                    ?: "Toko Kelontong Berkah"

                val settings = StoreSettingEntity(
                    id = 1,
                    storeName = storeNameVal,
                    slogan = settingsSnap.getString("slogan") ?: "",
                    address = settingsSnap.getString("address") ?: "",
                    phoneNumber = settingsSnap.getString("phoneNumber") ?: "",
                    receiptHeader = settingsSnap.getString("receiptHeader") ?: "SELAMAT BERBELANJA",
                    receiptFooter = settingsSnap.getString("receiptFooter") ?: "",
                    printerPaperSize = settingsSnap.getString("printerPaperSize") ?: "58mm",
                    isTaxEnabledByDefault = settingsSnap.getBoolean("isTaxEnabledByDefault") ?: false,
                    taxRatePercent = settingsSnap.getDouble("taxRatePercent") ?: 11.0,
                    qrisMerchantName = settingsSnap.getString("qrisMerchantName") ?: "",
                    qrisNmid = settingsSnap.getString("qrisNmid") ?: "",
                    userType = fetchedUserType,
                    tenantType = fetchedTenantType,
                    role = fetchedRole
                )
                repository.updateLocalStoreSettingsOnly(settings)

                repository.saveUser(
                    com.example.data.local.entity.UserEntity(
                        uid = uid,
                        email = currentUser.email ?: "",
                        name = storeNameVal,
                        userType = fetchedUserType,
                        tenantType = fetchedTenantType,
                        role = fetchedRole,
                        storeName = storeNameVal,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }

            // 2. Pull Categories
            val categoriesSnap = firestore.collection("users").document(uid).collection("categories").get().awaitTask()
            for (doc in categoriesSnap.documents) {
                val cat = CategoryEntity(
                    id = doc.getLong("id") ?: continue,
                    name = doc.getString("name") ?: "",
                    colorHex = doc.getString("colorHex") ?: "#006C4C"
                )
                repository.saveCategoryFromCloud(cat)
            }
            if (categoriesSnap.isEmpty) {
                val localCats = repository.getAllCategoriesDirect()
                if (localCats.isNotEmpty()) {
                    val batch = firestore.batch()
                    for (cat in localCats) {
                        val catRef = firestore.collection("users").document(uid).collection("categories").document("CAT_${cat.id}")
                        val catMap = hashMapOf("id" to cat.id, "name" to cat.name, "colorHex" to cat.colorHex)
                        batch.set(catRef, catMap, com.google.firebase.firestore.SetOptions.merge())
                    }
                    batch.commit().awaitTask()
                }
            }

            // 3. Pull Products
            val productsSnap = firestore.collection("users").document(uid).collection("products").get().awaitTask()
            for (doc in productsSnap.documents) {
                val rawSku = doc.getString("sku") ?: ""
                val resolvedSku = when {
                    rawSku.isNotBlank() -> rawSku
                    doc.id.startsWith("SKU-") -> doc.id
                    else -> DataImportHelper.generateSku("PRD", (doc.getLong("id") ?: 1L).toInt())
                }
                val prodId = doc.getLong("id") ?: 0L
                val barcode = doc.getString("barcode") ?: ""
                val remoteUpdatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                val existing = if (prodId > 0) repository.getProductById(prodId)
                    else if (barcode.isNotBlank()) repository.getProductByBarcode(barcode)
                    else if (resolvedSku.isNotBlank()) repository.getProductBySku(resolvedSku)
                    else null

                if (existing != null) {
                    if (remoteUpdatedAt > existing.updatedAt) {
                        val prod = existing.copy(
                            barcode = if (barcode.isNotBlank()) barcode else existing.barcode,
                            sku = if (resolvedSku.isNotBlank()) resolvedSku else existing.sku,
                            name = doc.getString("name") ?: existing.name,
                            categoryId = doc.getLong("categoryId") ?: existing.categoryId,
                            buyPrice = doc.getDouble("buyPrice") ?: existing.buyPrice,
                            sellPrice = doc.getDouble("sellPrice") ?: existing.sellPrice,
                            minStock = doc.getLong("minStock")?.toInt() ?: existing.minStock,
                            unit = doc.getString("unit") ?: existing.unit,
                            updatedAt = remoteUpdatedAt,
                            isSynced = true
                        )
                        repository.saveDirectProductFromCloud(prod)
                    }
                } else {
                    val prod = ProductEntity(
                        id = if (prodId > 0) prodId else 0L,
                        barcode = barcode,
                        sku = resolvedSku,
                        name = doc.getString("name") ?: "",
                        categoryId = doc.getLong("categoryId") ?: 0L,
                        buyPrice = doc.getDouble("buyPrice") ?: 0.0,
                        sellPrice = doc.getDouble("sellPrice") ?: 0.0,
                        stock = 0, // Stock will be updated strictly from stock movements
                        minStock = doc.getLong("minStock")?.toInt() ?: 5,
                        unit = doc.getString("unit") ?: "pcs",
                        updatedAt = remoteUpdatedAt,
                        isSynced = true
                    )
                    repository.saveDirectProductFromCloud(prod)
                }

                // If remote document still has legacy 'stock' field, delete it from Firestore
                if (doc.contains("stock")) {
                    try {
                        doc.reference.update("stock", FieldValue.delete())
                    } catch (_: Exception) {}
                }
            }
            if (productsSnap.isEmpty) {
                val localProducts = repository.getAllProductsDirect()
                if (localProducts.isNotEmpty()) {
                    val batch = firestore.batch()
                    for (product in localProducts) {
                        val docId = product.sku.ifBlank { if (product.barcode.isNotBlank()) product.barcode else "PROD_${product.id}" }
                        val prodRef = firestore.collection("users").document(uid).collection("products").document(docId)
                        val prodMap = hashMapOf<String, Any>(
                            "id" to product.id,
                            "barcode" to product.barcode,
                            "sku" to product.sku,
                            "name" to product.name,
                            "categoryId" to product.categoryId,
                            "buyPrice" to product.buyPrice,
                            "sellPrice" to product.sellPrice,
                            "minStock" to product.minStock,
                            "unit" to product.unit,
                            "updatedAt" to product.updatedAt,
                            "stock" to FieldValue.delete()
                        )
                        batch.set(prodRef, prodMap, com.google.firebase.firestore.SetOptions.merge())
                    }
                    batch.commit().awaitTask()
                }
            }

            // 3.5 Pull Units
            val unitsSnap = firestore.collection("users").document(uid).collection("units").get().awaitTask()
            for (doc in unitsSnap.documents) {
                val unit = UnitEntity(
                    id = doc.getLong("id") ?: continue,
                    name = doc.getString("name") ?: "",
                    isDefault = doc.getBoolean("isDefault") ?: false
                )
                repository.saveUnitFromCloud(unit)
            }
            if (unitsSnap.isEmpty) {
                val localUnits = repository.getAllUnitsDirect()
                if (localUnits.isNotEmpty()) {
                    val batch = firestore.batch()
                    for (unit in localUnits) {
                        val unitRef = firestore.collection("users").document(uid).collection("units").document("UNIT_${unit.id}")
                        val unitMap = hashMapOf("id" to unit.id, "name" to unit.name, "isDefault" to unit.isDefault)
                        batch.set(unitRef, unitMap, com.google.firebase.firestore.SetOptions.merge())
                    }
                    batch.commit().awaitTask()
                }
            }

            // 4. Pull & Reconcile Stock Movements
            val movementsSnap = firestore.collection("users").document(uid).collection("stock_movements").get().awaitTask()
            val remoteMovements = mutableListOf<StockMovementEntity>()
            for (doc in movementsSnap.documents) {
                val reasonStr = doc.getString("reason") ?: ""
                val typeStr = doc.getString("type") ?: "PENYESUAIAN"
                val rawLoss = doc.getDouble("lossAmount") ?: 0.0
                val rawIsNsb = doc.getBoolean("isNsb") ?: false
                val rawCost = doc.getDouble("costPrice") ?: 0.0
                val qtyVal = doc.getLong("quantity")?.toInt() ?: 0

                val isNsbCalc = rawIsNsb ||
                    rawLoss > 0.0 ||
                    reasonStr.contains("Rusak", ignoreCase = true) ||
                    reasonStr.contains("Expired", ignoreCase = true) ||
                    reasonStr.contains("Kadaluarsa", ignoreCase = true) ||
                    reasonStr.contains("Hilang", ignoreCase = true) ||
                    reasonStr.contains("Pecah", ignoreCase = true) ||
                    reasonStr.contains("Basi", ignoreCase = true) ||
                    reasonStr.contains("Selisih", ignoreCase = true) ||
                    reasonStr.contains("NSB", ignoreCase = true) ||
                    typeStr == "KELUAR" ||
                    (typeStr == "PENYESUAIAN" && qtyVal < 0)

                val lossCalc = if (rawLoss > 0.0) rawLoss else if (isNsbCalc) Math.abs(qtyVal) * rawCost else 0.0

                val move = StockMovementEntity(
                    id = doc.getLong("id") ?: 0L,
                    productId = doc.getLong("productId") ?: 0L,
                    productName = doc.getString("productName") ?: "",
                    type = typeStr,
                    quantity = qtyVal,
                    previousStock = doc.getLong("previousStock")?.toInt() ?: 0,
                    currentStock = doc.getLong("currentStock")?.toInt() ?: 0,
                    reason = reasonStr,
                    notes = doc.getString("notes") ?: "",
                    costPrice = rawCost,
                    isNsb = isNsbCalc,
                    lossAmount = lossCalc,
                    timestamp = doc.getLong("timestamp") ?: 0L,
                    isSynced = true
                )
                remoteMovements.add(move)
            }
            repository.clearLocalStockMovements()
            for (move in remoteMovements) {
                repository.saveStockMovementFromCloud(move)
            }
            repository.deduplicateStockMovements()
            repository.synchronizeAllProductsStockFromMovements()

            // 5. Pull & Reconcile Transactions
            val transactionsSnap = firestore.collection("users").document(uid).collection("transactions").get().awaitTask()
            for (doc in transactionsSnap.documents) {
                val invoiceNumber = doc.getString("invoiceNumber") ?: doc.id
                if (invoiceNumber.isBlank()) continue

                val tx = TransactionEntity(
                    id = doc.getLong("id") ?: 0L,
                    invoiceNumber = invoiceNumber,
                    timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                    subtotal = doc.getDouble("subtotal") ?: 0.0,
                    discountAmount = doc.getDouble("discountAmount") ?: 0.0,
                    discountNote = doc.getString("discountNote") ?: "",
                    taxAmount = doc.getDouble("taxAmount") ?: 0.0,
                    taxPercent = doc.getDouble("taxPercent") ?: 0.0,
                    isTaxApplied = doc.getBoolean("isTaxApplied") ?: false,
                    grandTotal = doc.getDouble("grandTotal") ?: 0.0,
                    paymentMethod = doc.getString("paymentMethod") ?: "TUNAI",
                    cashReceived = doc.getDouble("cashReceived") ?: 0.0,
                    changeAmount = doc.getDouble("changeAmount") ?: 0.0,
                    cashierName = doc.getString("cashierName") ?: "Kasir",
                    customerName = doc.getString("customerName") ?: "Pelanggan Umum",
                    isSynced = true,
                    syncTimestamp = System.currentTimeMillis()
                )

                val rawItems = doc.get("items") as? List<Map<String, Any>> ?: emptyList()
                val items = rawItems.map { itemMap ->
                    TransactionItemEntity(
                        id = (itemMap["id"] as? Number)?.toLong() ?: 0L,
                        transactionId = tx.id,
                        productId = (itemMap["productId"] as? Number)?.toLong() ?: 0L,
                        barcode = itemMap["barcode"] as? String ?: "",
                        sku = itemMap["sku"] as? String ?: "",
                        productName = itemMap["productName"] as? String ?: "",
                        quantity = (itemMap["quantity"] as? Number)?.toInt() ?: 1,
                        unitPrice = (itemMap["unitPrice"] as? Number)?.toDouble() ?: 0.0,
                        costPrice = (itemMap["costPrice"] as? Number)?.toDouble() ?: 0.0,
                        discountAmount = (itemMap["discountAmount"] as? Number)?.toDouble() ?: 0.0,
                        promoNote = itemMap["promoNote"] as? String ?: "",
                        subtotal = (itemMap["subtotal"] as? Number)?.toDouble() ?: 0.0
                    )
                }
                repository.saveTransactionWithItemsFromCloud(tx, items)
            }
            repository.repairMissingTransactionItems()

            // 6. Pull Promotions
            val promotionsSnap = firestore.collection("users").document(uid).collection("promotions").get().awaitTask()
            for (doc in promotionsSnap.documents) {
                val id = doc.getLong("id") ?: continue
                val name = doc.getString("name") ?: ""
                val promo = PromotionEntity(
                    id = id,
                    name = name,
                    promoScheme = doc.getString("promoScheme") ?: "CATEGORY",
                    type = doc.getString("type") ?: "PERCENTAGE",
                    value = doc.getDouble("value") ?: 0.0,
                    targetType = doc.getString("targetType") ?: "SPECIFIC_PRODUCT",
                    targetId = doc.getLong("targetId") ?: 0L,
                    targetName = doc.getString("targetName") ?: "",
                    minPurchaseAmount = doc.getDouble("minPurchaseAmount") ?: 0.0,
                    isActive = doc.getBoolean("isActive") ?: true,
                    note = doc.getString("note") ?: "",
                    minQuantity = doc.getLong("minQuantity")?.toInt() ?: 1,
                    startHour = doc.getLong("startHour")?.toInt() ?: 0,
                    endHour = doc.getLong("endHour")?.toInt() ?: 24,
                    activeDays = doc.getString("activeDays") ?: "",
                    tierRules = doc.getString("tierRules") ?: "",
                    multiProductIds = doc.getString("multiProductIds") ?: "",
                    multiProductNames = doc.getString("multiProductNames") ?: "",
                    triggerProductId = doc.getLong("triggerProductId") ?: 0L,
                    triggerProductName = doc.getString("triggerProductName") ?: "",
                    triggerMinQty = doc.getLong("triggerMinQty")?.toInt() ?: 1,
                    rewardProductId = doc.getLong("rewardProductId") ?: 0L,
                    rewardProductName = doc.getString("rewardProductName") ?: "",
                    rewardSpecialPrice = doc.getDouble("rewardSpecialPrice") ?: 0.0
                )
                repository.savePromotionFromCloud(promo)
            }

            repository.synchronizeAllProductsStockFromMovements()
            _syncState.value = SyncState.Success("Data SaaS dipulihkan dari Cloud", 0, System.currentTimeMillis())
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun toggleSimulatedNetwork(forcedOnline: Boolean) {
        _isOnline.value = forcedOnline
        if (!forcedOnline) {
            _syncState.value = SyncState.Offline
        } else {
            triggerAutoSync()
        }
    }

    suspend fun resetSpecificDatabase(category: String): Boolean {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val uid = currentUser?.uid
        val firestore = FirebaseFirestore.getInstance()

        return try {
            // 1. Clear Local Database
            when (category) {
                "Semua Database" -> {
                    repository.clearLocalDatabase()
                    repository.ensureDefaultUnitsExist()
                }
                "categories" -> {
                    repository.clearAllCategories()
                }
                "products" -> {
                    repository.clearAllProducts()
                }
                "promotions" -> {
                    repository.clearAllPromotions()
                }
                "settings" -> {
                    repository.clearStoreSettings()
                }
                "stock_movements" -> {
                    repository.clearLocalStockMovements()
                }
                "transactions" -> {
                    repository.clearAllTransactionsAndItems()
                }
                "units" -> {
                    repository.clearAllUnits()
                    repository.ensureDefaultUnitsExist()
                }
            }

            // 2. Clear Cloud Firestore Database
            if (uid != null) {
                when (category) {
                    "Semua Database" -> {
                        deleteCollectionOnCloud("categories")
                        deleteCollectionOnCloud("products")
                        deleteCollectionOnCloud("units")
                        deleteTransactionsOnCloud()
                        deleteCollectionOnCloud("stock_movements")
                        deleteCollectionOnCloud("promotions")
                        try {
                            firestore.collection("users").document(uid).collection("settings").document("store").delete().awaitTask()
                        } catch (_: Exception) {}
                    }
                    "categories" -> {
                        deleteCollectionOnCloud("categories")
                    }
                    "products" -> {
                        deleteCollectionOnCloud("products")
                    }
                    "promotions" -> {
                        deleteCollectionOnCloud("promotions")
                    }
                    "settings" -> {
                        try {
                            firestore.collection("users").document(uid).collection("settings").document("store").delete().awaitTask()
                        } catch (_: Exception) {}
                    }
                    "stock_movements" -> {
                        deleteCollectionOnCloud("stock_movements")
                    }
                    "transactions" -> {
                        deleteTransactionsOnCloud()
                    }
                    "units" -> {
                        deleteCollectionOnCloud("units")
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private suspend fun deleteCollectionOnCloud(collectionName: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val firestore = FirebaseFirestore.getInstance()
        try {
            val snapshot = firestore.collection("users").document(uid).collection(collectionName).get().awaitTask()
            if (!snapshot.isEmpty) {
                val batch = firestore.batch()
                for (doc in snapshot.documents) {
                    batch.delete(doc.reference)
                }
                batch.commit().awaitTask()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun deleteTransactionsOnCloud() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val firestore = FirebaseFirestore.getInstance()
        try {
            val snapshot = firestore.collection("users").document(uid).collection("transactions").get().awaitTask()
            for (doc in snapshot.documents) {
                val itemsSnap = doc.reference.collection("items").get().awaitTask()
                val batch = firestore.batch()
                for (itemDoc in itemsSnap.documents) {
                    batch.delete(itemDoc.reference)
                }
                batch.delete(doc.reference)
                batch.commit().awaitTask()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun restoreSpecificDatabase(category: String): Boolean {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return false
        val uid = currentUser.uid
        val firestore = FirebaseFirestore.getInstance()

        return try {
            when (category) {
                "Semua Database" -> {
                    pullDataFromCloud()
                }
                "categories" -> {
                    val categoriesSnap = firestore.collection("users").document(uid).collection("categories").get().awaitTask()
                    repository.clearAllCategories()
                    for (doc in categoriesSnap.documents) {
                        val cat = CategoryEntity(
                            id = doc.getLong("id") ?: continue,
                            name = doc.getString("name") ?: "",
                            colorHex = doc.getString("colorHex") ?: "#006C4C"
                        )
                        repository.saveCategoryFromCloud(cat)
                    }
                }
                "products" -> {
                    val productsSnap = firestore.collection("users").document(uid).collection("products").get().awaitTask()
                    repository.clearAllProducts()
                    for (doc in productsSnap.documents) {
                        val rawSku = doc.getString("sku") ?: ""
                        val resolvedSku = when {
                            rawSku.isNotBlank() -> rawSku
                            doc.id.startsWith("SKU-") -> doc.id
                            else -> com.example.util.DataImportHelper.generateSku("PRD", (doc.getLong("id") ?: 1L).toInt())
                        }
                        val prod = ProductEntity(
                            id = doc.getLong("id") ?: continue,
                            barcode = doc.getString("barcode") ?: "",
                            sku = resolvedSku,
                            name = doc.getString("name") ?: "",
                            categoryId = doc.getLong("categoryId") ?: 0L,
                            buyPrice = doc.getDouble("buyPrice") ?: 0.0,
                            sellPrice = doc.getDouble("sellPrice") ?: 0.0,
                            stock = 0,
                            minStock = doc.getLong("minStock")?.toInt() ?: 5,
                            unit = doc.getString("unit") ?: "pcs",
                            updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis(),
                            isSynced = true
                        )
                        repository.saveDirectProductFromCloud(prod)

                        if (doc.contains("stock")) {
                            try {
                                doc.reference.update("stock", FieldValue.delete())
                            } catch (_: Exception) {}
                        }
                    }
                    repository.synchronizeAllProductsStockFromMovements()
                }
                "promotions" -> {
                    val promotionsSnap = firestore.collection("users").document(uid).collection("promotions").get().awaitTask()
                    repository.clearAllPromotions()
                    for (doc in promotionsSnap.documents) {
                        val id = doc.getLong("id") ?: continue
                        val name = doc.getString("name") ?: ""
                        val promo = PromotionEntity(
                            id = id,
                            name = name,
                            promoScheme = doc.getString("promoScheme") ?: "CATEGORY",
                            type = doc.getString("type") ?: "PERCENTAGE",
                            value = doc.getDouble("value") ?: 0.0,
                            targetType = doc.getString("targetType") ?: "SPECIFIC_PRODUCT",
                            targetId = doc.getLong("targetId") ?: 0L,
                            targetName = doc.getString("targetName") ?: "",
                            minPurchaseAmount = doc.getDouble("minPurchaseAmount") ?: 0.0,
                            isActive = doc.getBoolean("isActive") ?: true,
                            note = doc.getString("note") ?: "",
                            minQuantity = doc.getLong("minQuantity")?.toInt() ?: 1,
                            startHour = doc.getLong("startHour")?.toInt() ?: 0,
                            endHour = doc.getLong("endHour")?.toInt() ?: 24,
                            activeDays = doc.getString("activeDays") ?: "",
                            tierRules = doc.getString("tierRules") ?: "",
                            multiProductIds = doc.getString("multiProductIds") ?: "",
                            multiProductNames = doc.getString("multiProductNames") ?: "",
                            triggerProductId = doc.getLong("triggerProductId") ?: 0L,
                            triggerProductName = doc.getString("triggerProductName") ?: "",
                            triggerMinQty = doc.getLong("triggerMinQty")?.toInt() ?: 1,
                            rewardProductId = doc.getLong("rewardProductId") ?: 0L,
                            rewardProductName = doc.getString("rewardProductName") ?: "",
                            rewardSpecialPrice = doc.getDouble("rewardSpecialPrice") ?: 0.0
                        )
                        repository.savePromotionFromCloud(promo)
                    }
                }
                "settings" -> {
                    val settingsDoc = firestore.collection("users").document(uid).collection("settings").document("store").get().awaitTask()
                    if (settingsDoc.exists()) {
                        val sName = settingsDoc.getString("storeName") ?: ""
                        if (sName.isNotBlank()) {
                            val settingsObj = com.example.data.local.entity.StoreSettingEntity(
                                id = 1,
                                storeName = sName,
                                slogan = settingsDoc.getString("slogan") ?: "",
                                address = settingsDoc.getString("address") ?: "",
                                phoneNumber = settingsDoc.getString("phoneNumber") ?: "",
                                receiptHeader = settingsDoc.getString("receiptHeader") ?: "SELAMAT BERBELANJA",
                                receiptFooter = settingsDoc.getString("receiptFooter") ?: "Terima Kasih",
                                printerPaperSize = settingsDoc.getString("printerPaperSize") ?: "58mm",
                                isTaxEnabledByDefault = settingsDoc.getBoolean("isTaxEnabledByDefault") ?: false,
                                taxRatePercent = settingsDoc.getDouble("taxRatePercent") ?: 11.0,
                                qrisMerchantName = settingsDoc.getString("qrisMerchantName") ?: "",
                                qrisNmid = settingsDoc.getString("qrisNmid") ?: "",
                                lastSyncTimestamp = settingsDoc.getLong("lastSyncTimestamp") ?: System.currentTimeMillis()
                            )
                            repository.updateLocalStoreSettingsOnly(settingsObj)
                        }
                    }
                }
                "stock_movements" -> {
                    val movementsSnap = firestore.collection("users").document(uid).collection("stock_movements").get().awaitTask()
                    val remoteMovements = mutableListOf<StockMovementEntity>()
                    for (doc in movementsSnap.documents) {
                        val reasonStr = doc.getString("reason") ?: ""
                        val typeStr = doc.getString("type") ?: "PENYESUAIAN"
                        val rawLoss = doc.getDouble("lossAmount") ?: 0.0
                        val rawIsNsb = doc.getBoolean("isNsb") ?: false
                        val rawCost = doc.getDouble("costPrice") ?: 0.0
                        val qtyVal = doc.getLong("quantity")?.toInt() ?: 0

                        val isNsbCalc = rawIsNsb ||
                            rawLoss > 0.0 ||
                            reasonStr.contains("Rusak", ignoreCase = true) ||
                            reasonStr.contains("Expired", ignoreCase = true) ||
                            reasonStr.contains("Kadaluarsa", ignoreCase = true) ||
                            reasonStr.contains("Hilang", ignoreCase = true) ||
                            reasonStr.contains("Pecah", ignoreCase = true) ||
                            reasonStr.contains("Basi", ignoreCase = true) ||
                            reasonStr.contains("Selisih", ignoreCase = true) ||
                            reasonStr.contains("NSB", ignoreCase = true) ||
                            typeStr == "KELUAR" ||
                            (typeStr == "PENYESUAIAN" && qtyVal < 0)

                        val lossCalc = if (rawLoss > 0.0) rawLoss else if (isNsbCalc) Math.abs(qtyVal) * rawCost else 0.0

                        val move = StockMovementEntity(
                            id = doc.getLong("id") ?: 0L,
                            productId = doc.getLong("productId") ?: 0L,
                            productName = doc.getString("productName") ?: "",
                            type = typeStr,
                            quantity = qtyVal,
                            previousStock = doc.getLong("previousStock")?.toInt() ?: 0,
                            currentStock = doc.getLong("currentStock")?.toInt() ?: 0,
                            reason = reasonStr,
                            notes = doc.getString("notes") ?: "",
                            costPrice = rawCost,
                            isNsb = isNsbCalc,
                            lossAmount = lossCalc,
                            timestamp = doc.getLong("timestamp") ?: 0L,
                            isSynced = true
                        )
                        remoteMovements.add(move)
                    }
                    repository.clearLocalStockMovements()
                    for (move in remoteMovements) {
                        repository.saveStockMovementFromCloud(move)
                    }
                    repository.deduplicateStockMovements()
                }
                "transactions" -> {
                    val transactionsSnap = firestore.collection("users").document(uid).collection("transactions").get().awaitTask()
                    repository.clearAllTransactionsAndItems()
                    for (doc in transactionsSnap.documents) {
                        val tx = com.example.data.local.entity.TransactionEntity(
                            id = doc.getLong("id") ?: continue,
                            invoiceNumber = doc.getString("invoiceNumber") ?: "",
                            timestamp = doc.getLong("timestamp") ?: 0L,
                            subtotal = doc.getDouble("subtotal") ?: 0.0,
                            discountAmount = doc.getDouble("discountAmount") ?: 0.0,
                            discountNote = doc.getString("discountNote") ?: "",
                            taxAmount = doc.getDouble("taxAmount") ?: 0.0,
                            taxPercent = doc.getDouble("taxPercent") ?: 0.0,
                            isTaxApplied = doc.getBoolean("isTaxApplied") ?: false,
                            grandTotal = doc.getDouble("grandTotal") ?: 0.0,
                            paymentMethod = doc.getString("paymentMethod") ?: "TUNAI",
                            cashReceived = doc.getDouble("cashReceived") ?: 0.0,
                            changeAmount = doc.getDouble("changeAmount") ?: 0.0,
                            isSynced = true,
                            syncTimestamp = doc.getLong("syncTimestamp") ?: System.currentTimeMillis()
                        )
                        val itemsList = mutableListOf<com.example.data.local.entity.TransactionItemEntity>()
                        val itemsSnap = firestore.collection("users").document(uid).collection("transactions").document(doc.id).collection("items").get().awaitTask()
                        for (itemDoc in itemsSnap.documents) {
                            val item = com.example.data.local.entity.TransactionItemEntity(
                                id = itemDoc.getLong("id") ?: continue,
                                transactionId = tx.id,
                                productId = itemDoc.getLong("productId") ?: 0L,
                                productName = itemDoc.getString("productName") ?: "",
                                barcode = itemDoc.getString("barcode") ?: "",
                                quantity = itemDoc.getLong("quantity")?.toInt() ?: 1,
                                unitPrice = itemDoc.getDouble("unitPrice") ?: 0.0,
                                costPrice = itemDoc.getDouble("costPrice") ?: 0.0,
                                subtotal = itemDoc.getDouble("subtotal") ?: 0.0
                            )
                            itemsList.add(item)
                        }
                        repository.saveTransactionWithItemsFromCloud(tx, itemsList)
                    }
                }
                "units" -> {
                    val unitsSnap = firestore.collection("users").document(uid).collection("units").get().awaitTask()
                    repository.clearAllUnits()
                    for (doc in unitsSnap.documents) {
                        val unit = UnitEntity(
                            id = doc.getLong("id") ?: continue,
                            name = doc.getString("name") ?: "pcs"
                        )
                        repository.saveUnitFromCloud(unit)
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
