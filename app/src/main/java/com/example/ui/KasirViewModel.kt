package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.KasirApplication
import com.example.data.local.dao.TopProductSummary
import com.example.data.local.dao.TransactionWithItems
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.PromotionEntity
import com.example.data.local.entity.StockMovementEntity
import com.example.data.local.entity.StoreSettingEntity
import com.example.data.local.entity.UnitEntity
import com.example.data.model.CartItem
import com.example.data.sync.SyncState
import com.example.util.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class ReportPeriod {
    SEMUA, HARIAN, BULANAN, TAHUNAN, RENTANG_TANGGAL
}

data class SalesReportSummary(
    val grossSales: Double = 0.0,
    val totalDiscount: Double = 0.0,
    val totalTax: Double = 0.0,
    val netSales: Double = 0.0,
    val totalCostHpp: Double = 0.0,
    val estimatedProfit: Double = 0.0, // Laba Bersih Akhir setelah memotong HPP dan Kerugian NSB
    val grossOperationalProfit: Double = 0.0, // Laba Kotor Operasional (sebelum NSB)
    val totalTransactions: Int = 0,
    val totalItemsSold: Int = 0,
    val cashSales: Double = 0.0,
    val qrisSales: Double = 0.0,
    val transferSales: Double = 0.0,
    val totalNsbLoss: Double = 0.0,
    val totalNsbItemsCount: Int = 0,
    val nsbTransactionCount: Int = 0,
    // Category Breakdowns: Hilang, Rusak, Penyesuaian
    val nsbHilangLoss: Double = 0.0,
    val nsbHilangItems: Int = 0,
    val nsbHilangCount: Int = 0,
    val nsbRusakLoss: Double = 0.0,
    val nsbRusakItems: Int = 0,
    val nsbRusakCount: Int = 0,
    val nsbPenyesuaianLoss: Double = 0.0,
    val nsbPenyesuaianItems: Int = 0,
    val nsbPenyesuaianCount: Int = 0
)

class KasirViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as KasirApplication
    private val repository = app.repository
    val cloudSyncManager = app.cloudSyncManager

    // Authentication State
    private val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
    private val _currentUser = MutableStateFlow<com.google.firebase.auth.FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<com.google.firebase.auth.FirebaseUser?> = _currentUser.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser
        }
        viewModelScope.launch(Dispatchers.IO) {
            repository.ensureDefaultUnitsExist()
            repository.synchronizeAllProductsStockFromMovements()
            val allPromos = repository.getAllPromotionsDirect()
            for (p in allPromos) {
                if (p.name.contains("Tebus Murah Belanja Hemat", ignoreCase = true)) {
                    repository.deletePromotion(p)
                }
            }
        }
    }

    fun seedStarterNsbData() {
        // No-op: Hardcoded starter NSB seeding removed
    }

    fun signInWithEmailAndPassword(email: String, password: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                viewModelScope.launch(Dispatchers.IO) {
                    cloudSyncManager.pullDataFromCloud()
                    launch(Dispatchers.Main) {
                        onSuccess()
                    }
                }
            }
            .addOnFailureListener {
                onFailure(it.message ?: "Login gagal")
            }
    }

    fun registerWithEmailAndPassword(
        email: String,
        password: String,
        userType: String = "admin",
        tenantType: String = "admin",
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                viewModelScope.launch(Dispatchers.IO) {
                    val uid = result.user?.uid
                    val currentSettings = repository.getStoreSettingsDirect()
                    val newSettings = currentSettings.copy(
                        userType = userType.lowercase().trim(),
                        tenantType = tenantType.lowercase().trim(),
                        role = userType.lowercase().trim()
                    )
                    repository.updateStoreSettings(newSettings)
                    if (uid != null) {
                        repository.saveUser(
                            com.example.data.local.entity.UserEntity(
                                uid = uid,
                                email = email,
                                userType = userType.lowercase().trim(),
                                tenantType = tenantType.lowercase().trim(),
                                role = userType.lowercase().trim(),
                                storeName = newSettings.storeName,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                    launch(Dispatchers.Main) {
                        onSuccess()
                    }
                }
            }
            .addOnFailureListener {
                onFailure(it.message ?: "Pendaftaran gagal")
            }
    }

    fun updateUserTenantType(userType: String, tenantType: String = userType) {
        val current = storeSettings.value
        val updated = current.copy(
            userType = userType.lowercase().trim(),
            tenantType = tenantType.lowercase().trim(),
            role = userType.lowercase().trim()
        )
        updateStoreSettings(updated)
        viewModelScope.launch(Dispatchers.IO) {
            val uid = auth.currentUser?.uid
            if (uid != null) {
                repository.saveUser(
                    com.example.data.local.entity.UserEntity(
                        uid = uid,
                        email = auth.currentUser?.email ?: "",
                        name = updated.storeName,
                        userType = updated.userType,
                        tenantType = updated.tenantType,
                        role = updated.role,
                        storeName = updated.storeName,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearLocalDatabase()
            auth.signOut()
        }
    }

    // Settings
    val storeSettings: StateFlow<StoreSettingEntity> = repository.storeSettings
        .combine(MutableStateFlow(StoreSettingEntity())) { current, default ->
            current ?: default
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StoreSettingEntity())

    // Master Data
    val categories: StateFlow<List<CategoryEntity>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val units: StateFlow<List<UnitEntity>> = repository.allUnits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val products: StateFlow<List<ProductEntity>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lowStockProducts: StateFlow<List<ProductEntity>> = repository.lowStockProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lowStockCount: StateFlow<Int> = repository.lowStockCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val promotions: StateFlow<List<PromotionEntity>> = repository.allPromotions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stockMovements: StateFlow<List<StockMovementEntity>> = combine(
        repository.allStockMovements,
        products
    ) { rawMovements, productList ->
        val productMap = productList.associateBy { it.id }
        val seen = mutableListOf<StockMovementEntity>()
        rawMovements.map { move ->
            val prod = productMap[move.productId]
            val unitCost = when {
                move.costPrice > 0.0 -> move.costPrice
                prod != null && prod.buyPrice > 0.0 -> prod.buyPrice
                prod != null && prod.sellPrice > 0.0 -> prod.sellPrice
                else -> 0.0
            }
            val isNsb = move.isNsb ||
                move.type == "KELUAR" ||
                (move.type == "PENYESUAIAN" && move.quantity < 0) ||
                move.lossAmount > 0.0 ||
                move.reason.contains("Hilang", ignoreCase = true) ||
                move.reason.contains("Rusak", ignoreCase = true) ||
                move.reason.contains("Kadaluarsa", ignoreCase = true) ||
                move.reason.contains("Expired", ignoreCase = true) ||
                move.reason.contains("Pecah", ignoreCase = true) ||
                move.reason.contains("Basi", ignoreCase = true) ||
                move.reason.contains("Selisih", ignoreCase = true) ||
                move.reason.contains("NSB", ignoreCase = true) ||
                move.reason.contains("Opname", ignoreCase = true)

            val qtyAbs = Math.abs(move.quantity)
            val loss = if (move.lossAmount > 0.0) {
                move.lossAmount
            } else if (isNsb) {
                qtyAbs * unitCost
            } else {
                0.0
            }

            move.copy(
                costPrice = if (unitCost > 0.0) unitCost else move.costPrice,
                isNsb = isNsb,
                lossAmount = loss
            )
        }.filter { move ->
            val isDup = seen.any { prev ->
                prev.productId == move.productId &&
                prev.type == move.type &&
                prev.quantity == move.quantity &&
                prev.reason.trim().equals(move.reason.trim(), ignoreCase = true) &&
                Math.abs(prev.timestamp - move.timestamp) < 60_000L
            }
            if (isDup) false else { seen.add(move); true }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions: StateFlow<List<TransactionWithItems>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unsyncedCount: StateFlow<Int> = repository.unsyncedCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // POS State
    private val _selectedCategoryId = MutableStateFlow(0L)
    val selectedCategoryId: StateFlow<Long> = _selectedCategoryId.asStateFlow()

    private val _posSearchQuery = MutableStateFlow("")
    val posSearchQuery: StateFlow<String> = _posSearchQuery.asStateFlow()

    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    private val _isTaxApplied = MutableStateFlow(false)
    val isTaxApplied: StateFlow<Boolean> = _isTaxApplied.asStateFlow()

    private val _cartGlobalDiscount = MutableStateFlow(0.0)
    val cartGlobalDiscount: StateFlow<Double> = _cartGlobalDiscount.asStateFlow()

    private val _cartDiscountNote = MutableStateFlow("")
    val cartDiscountNote: StateFlow<String> = _cartDiscountNote.asStateFlow()

    private val _appliedPromotionsSummary = MutableStateFlow<List<String>>(emptyList())
    val appliedPromotionsSummary: StateFlow<List<String>> = _appliedPromotionsSummary.asStateFlow()

    private val _totalPromoSavings = MutableStateFlow(0.0)
    val totalPromoSavings: StateFlow<Double> = _totalPromoSavings.asStateFlow()

    private val _customerName = MutableStateFlow("Pelanggan Umum")
    val customerName: StateFlow<String> = _customerName.asStateFlow()

    private val _lastCompletedTransaction = MutableStateFlow<TransactionWithItems?>(null)
    val lastCompletedTransaction: StateFlow<TransactionWithItems?> = _lastCompletedTransaction.asStateFlow()

    private val _showReceiptDialog = MutableStateFlow(false)
    val showReceiptDialog: StateFlow<Boolean> = _showReceiptDialog.asStateFlow()

    private val _showQrisDialog = MutableStateFlow(false)
    val showQrisDialog: StateFlow<Boolean> = _showQrisDialog.asStateFlow()

    private val _isCameraScannerOpen = MutableStateFlow(false)
    val isCameraScannerOpen: StateFlow<Boolean> = _isCameraScannerOpen.asStateFlow()

    private val _notificationMessage = MutableStateFlow<String?>(null)
    val notificationMessage: StateFlow<String?> = _notificationMessage.asStateFlow()

    // Reports Filters
    private val _reportPeriod = MutableStateFlow(ReportPeriod.HARIAN)
    val reportPeriod: StateFlow<ReportPeriod> = _reportPeriod.asStateFlow()

    private val _customStartDate = MutableStateFlow(getStartOfDay(System.currentTimeMillis()))
    val customStartDate: StateFlow<Long> = _customStartDate.asStateFlow()

    private val _customEndDate = MutableStateFlow(getEndOfDay(System.currentTimeMillis()))
    val customEndDate: StateFlow<Long> = _customEndDate.asStateFlow()

    // Date range timestamps based on selected period
    private val reportDateRange = combine(
        _reportPeriod, _customStartDate, _customEndDate
    ) { period, customStart, customEnd ->
        val now = System.currentTimeMillis()
        when (period) {
            ReportPeriod.SEMUA -> Pair(0L, Long.MAX_VALUE)
            ReportPeriod.HARIAN -> Pair(getStartOfDay(now), getEndOfDay(now))
            ReportPeriod.BULANAN -> Pair(getStartOfMonth(now), getEndOfMonth(now))
            ReportPeriod.TAHUNAN -> Pair(getStartOfYear(now), getEndOfYear(now))
            ReportPeriod.RENTANG_TANGGAL -> Pair(customStart, customEnd)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val reportTransactions: StateFlow<List<TransactionWithItems>> = reportDateRange
        .flatMapLatest { (start, end) ->
            repository.getTransactionsBetween(start, end)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val reportTopProducts: StateFlow<List<TopProductSummary>> = reportDateRange
        .flatMapLatest { (start, end) ->
            repository.getTopProducts(start, end, 10)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val reportNsbMovements: StateFlow<List<StockMovementEntity>> = combine(
        reportDateRange.flatMapLatest { (start, end) ->
            repository.getMovementsBetween(start, end)
        },
        products
    ) { movements, productList ->
        val productMap = productList.associateBy { it.id }
        movements.mapNotNull { move ->
            val reason = move.reason
            val isNsbCandidate = move.isNsb ||
                move.type == "KELUAR" ||
                (move.type == "PENYESUAIAN" && move.quantity < 0) ||
                move.lossAmount > 0.0 ||
                reason.contains("Hilang", ignoreCase = true) ||
                reason.contains("Rusak", ignoreCase = true) ||
                reason.contains("Kadaluarsa", ignoreCase = true) ||
                reason.contains("Expired", ignoreCase = true) ||
                reason.contains("Pecah", ignoreCase = true) ||
                reason.contains("Basi", ignoreCase = true) ||
                reason.contains("Selisih", ignoreCase = true) ||
                reason.contains("NSB", ignoreCase = true) ||
                reason.contains("Opname", ignoreCase = true)

            if (!isNsbCandidate) {
                null
            } else {
                val prod = productMap[move.productId]
                val unitCost = when {
                    move.costPrice > 0.0 -> move.costPrice
                    prod != null && prod.buyPrice > 0.0 -> prod.buyPrice
                    prod != null && prod.sellPrice > 0.0 -> prod.sellPrice
                    else -> 0.0
                }
                val qtyAbs = Math.abs(move.quantity)
                val loss = if (move.lossAmount > 0.0) {
                    move.lossAmount
                } else {
                    qtyAbs * unitCost
                }
                move.copy(
                    isNsb = true,
                    costPrice = if (unitCost > 0.0) unitCost else move.costPrice,
                    lossAmount = loss
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allNsbMovements: StateFlow<List<StockMovementEntity>> = stockMovements
        .map { movements ->
            movements.filter { it.isNsb }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reportSummary: StateFlow<SalesReportSummary> = combine(
        reportTransactions,
        reportNsbMovements
    ) { txList, nsbList ->
        calculateReportSummary(txList, nsbList)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SalesReportSummary())

    init {
        // Reconcile products, backfill NSB calculations, and remove duplicates
        viewModelScope.launch(Dispatchers.IO) {
            repository.deduplicateProducts()
            repository.reconcileNsbMovements()
            repository.deduplicateStockMovements()
        }

        // Init tax setting default from store settings
        viewModelScope.launch {
            repository.storeSettings.collect { setting ->
                if (setting != null) {
                    _isTaxApplied.value = setting.isTaxEnabledByDefault
                }
            }
        }

        // Auto-recalculate cart promotions whenever promotion definitions change
        viewModelScope.launch {
            repository.allPromotions.collect {
                recalculateCartPromotions()
            }
        }
    }

    // POS Methods
    fun setPosCategory(categoryId: Long) {
        _selectedCategoryId.value = categoryId
    }

    fun setPosSearchQuery(query: String) {
        _posSearchQuery.value = query
    }

    fun openCameraScanner() {
        _isCameraScannerOpen.value = true
    }

    fun closeCameraScanner() {
        _isCameraScannerOpen.value = false
    }

    private val _lastScannedProduct = MutableStateFlow<ProductEntity?>(null)
    val lastScannedProduct: StateFlow<ProductEntity?> = _lastScannedProduct.asStateFlow()

    private val _scannerScanError = MutableStateFlow<String?>(null)
    val scannerScanError: StateFlow<String?> = _scannerScanError.asStateFlow()

    fun clearLastScannedProduct() {
        _lastScannedProduct.value = null
        _scannerScanError.value = null
    }

    fun onBarcodeScanned(barcode: String, onResult: (ProductEntity?) -> Unit = {}) {
        viewModelScope.launch {
            val clean = barcode.trim()
            val product = repository.getProductByBarcodeOrSku(clean)
            if (product != null) {
                // Strict check: Stock is exhausted (0 or negative)
                if (product.stock <= 0) {
                    _lastScannedProduct.value = null
                    _scannerScanError.value = "Stok produk '${product.name}' HABIS (0 ${product.unit}). Tidak dapat diproses!"
                    _notificationMessage.value = "Gagal: Stok '${product.name}' habis (0 ${product.unit})!"
                    onResult(null)
                    return@launch
                }

                // Strict check: Cart quantity already reaches available stock
                val existingItem = _cart.value.find { it.product.id == product.id }
                val currentQtyInCart = existingItem?.quantity ?: 0
                if (currentQtyInCart >= product.stock) {
                    _lastScannedProduct.value = null
                    _scannerScanError.value = "Stok '${product.name}' tidak mencukupi! Tersisa ${product.stock} ${product.unit}, sudah ada $currentQtyInCart di keranjang."
                    _notificationMessage.value = "Gagal: Stok '${product.name}' sudah mencapai batas maksimal stok (${product.stock} ${product.unit})!"
                    onResult(null)
                    return@launch
                }

                addToCart(product)
                _lastScannedProduct.value = product
                _scannerScanError.value = null
                _notificationMessage.value = "Ditambahkan: ${product.name}"
                onResult(product)
            } else {
                _lastScannedProduct.value = null
                _scannerScanError.value = "Barcode/SKU $clean tidak terdaftar!"
                _notificationMessage.value = "Barcode/SKU $clean tidak terdaftar di produk!"
                onResult(null)
            }
        }
    }

    fun clearNotification() {
        _notificationMessage.value = null
    }

    fun recalculateCartPromotions() {
        val result = com.example.util.PromotionEngine.calculateCartPromotions(_cart.value, promotions.value)
        _cart.value = result.updatedItems
        _appliedPromotionsSummary.value = result.appliedPromotions
        _totalPromoSavings.value = result.totalSavings
    }

    fun addToCart(product: ProductEntity) {
        val currentProd = products.value.find { it.id == product.id } ?: product
        if (currentProd.stock <= 0) {
            _notificationMessage.value = "Tidak dapat menambahkan! Stok '${currentProd.name}' habis (0 ${currentProd.unit})."
            return
        }

        val currentList = _cart.value.toMutableList()
        val existingIndex = currentList.indexOfFirst { it.product.id == currentProd.id }

        if (existingIndex >= 0) {
            val item = currentList[existingIndex]
            if (item.quantity >= currentProd.stock) {
                _notificationMessage.value = "Stok '${currentProd.name}' hanya tersisa ${currentProd.stock} ${currentProd.unit}!"
                return
            }
            currentList[existingIndex] = item.copy(product = currentProd, quantity = item.quantity + 1)
        } else {
            currentList.add(CartItem(product = currentProd, quantity = 1))
        }
        _cart.value = currentList
        recalculateCartPromotions()
    }

    fun updateCartItemQuantity(productId: Long, newQuantity: Int) {
        if (newQuantity <= 0) {
            removeFromCart(productId)
            return
        }
        val currentProd = products.value.find { it.id == productId }
        val currentList = _cart.value.toMutableList()
        val index = currentList.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            val item = currentList[index]
            val maxStock = currentProd?.stock ?: item.product.stock
            if (newQuantity > maxStock) {
                currentList[index] = currentList[index].copy(product = currentProd ?: item.product, quantity = maxStock)
                _notificationMessage.value = "Jumlah melebihi stok! Maksimal stok '${item.product.name}' adalah $maxStock ${item.product.unit}."
            } else {
                currentList[index] = currentList[index].copy(product = currentProd ?: item.product, quantity = newQuantity)
            }
            _cart.value = currentList
            recalculateCartPromotions()
        }
    }

    fun removeFromCart(productId: Long) {
        _cart.value = _cart.value.filter { it.product.id != productId }
        recalculateCartPromotions()
    }

    fun clearCart() {
        _cart.value = emptyList()
        _cartGlobalDiscount.value = 0.0
        _cartDiscountNote.value = ""
        _customerName.value = "Pelanggan Umum"
        _appliedPromotionsSummary.value = emptyList()
        _totalPromoSavings.value = 0.0
    }

    fun setTaxApplied(applied: Boolean) {
        _isTaxApplied.value = applied
    }

    fun setCartGlobalDiscount(amount: Double, note: String) {
        _cartGlobalDiscount.value = amount
        _cartDiscountNote.value = note
    }

    fun setCustomerName(name: String) {
        _customerName.value = name
    }

    // Calculations
    fun getOriginalSubtotal(): Double {
        return _cart.value.sumOf { it.totalOriginal }
    }

    fun getCartSubtotal(): Double {
        return getOriginalSubtotal()
    }

    fun getCartPromoDiscount(): Double {
        return _cart.value.sumOf { it.totalDiscount }
    }

    fun getCartTotalDiscount(): Double {
        return getCartPromoDiscount() + _cartGlobalDiscount.value
    }

    fun getSubtotalAfterDiscount(): Double {
        return (getOriginalSubtotal() - getCartTotalDiscount()).coerceAtLeast(0.0)
    }

    fun getTaxAmount(): Double {
        if (!_isTaxApplied.value) return 0.0
        val taxRate = storeSettings.value.taxRatePercent
        return getSubtotalAfterDiscount() * (taxRate / 100.0)
    }

    fun getGrandTotal(): Double {
        return getSubtotalAfterDiscount() + getTaxAmount()
    }

    fun openQrisDialog() {
        _showQrisDialog.value = true
    }

    fun closeQrisDialog() {
        _showQrisDialog.value = false
    }

    fun dismissReceiptDialog() {
        _showReceiptDialog.value = false
        _lastCompletedTransaction.value = null
    }

    fun showReceiptForTransaction(transactionWithItems: TransactionWithItems) {
        _lastCompletedTransaction.value = transactionWithItems
        _showReceiptDialog.value = true
    }

    fun completeCheckout(
        paymentMethod: String,
        cashReceived: Double
    ) {
        val currentCart = _cart.value
        if (currentCart.isEmpty()) return

        val subtotal = getOriginalSubtotal()
        val totalDiscount = getCartTotalDiscount()
        val taxPercent = if (_isTaxApplied.value) storeSettings.value.taxRatePercent else 0.0
        val taxAmount = getTaxAmount()
        val grandTotal = getGrandTotal()
        val finalCashReceived = if (paymentMethod == "TUNAI") cashReceived else grandTotal
        val changeAmount = (finalCashReceived - grandTotal).coerceAtLeast(0.0)

        val promoNames = _appliedPromotionsSummary.value
        val promoText = if (promoNames.isNotEmpty()) "Promo: ${promoNames.joinToString(", ")}" else ""
        val fullDiscountNote = listOfNotNull(
            promoText.ifBlank { null },
            _cartDiscountNote.value.ifBlank { null }
        ).joinToString(" | ")

        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.processCheckout(
                cartItems = currentCart,
                subtotal = subtotal,
                discountAmount = totalDiscount,
                discountNote = fullDiscountNote,
                isTaxApplied = _isTaxApplied.value,
                taxPercent = taxPercent,
                taxAmount = taxAmount,
                grandTotal = grandTotal,
                paymentMethod = paymentMethod,
                cashReceived = finalCashReceived,
                changeAmount = changeAmount,
                cashierName = "Kasir 1",
                customerName = _customerName.value.ifBlank { "Pelanggan Umum" }
            )

            // Check if any product has reached minimum stock
            for (cartItem in currentCart) {
                val updatedProduct = repository.getProductById(cartItem.product.id)
                if (updatedProduct != null && updatedProduct.stock <= updatedProduct.minStock) {
                    NotificationHelper.showLowStockNotification(
                        context = app,
                        productName = updatedProduct.name,
                        currentStock = updatedProduct.stock,
                        minStock = updatedProduct.minStock,
                        unit = updatedProduct.unit
                    )
                }
            }

            // Cloud sync trigger in background
            cloudSyncManager.triggerAutoSync()

            _lastCompletedTransaction.value = result
            _showReceiptDialog.value = true
            _showQrisDialog.value = false
            clearCart()
        }
    }

    // Inventory & Stock Management
    fun importProducts(
        items: List<com.example.util.ParsedImportProduct>,
        onComplete: (com.example.util.ImportResult) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.importProductsWithCategories(items)
            cloudSyncManager.triggerAutoSync()
            launch(Dispatchers.Main) {
                onComplete(result)
            }
        }
    }

    fun addNewProduct(product: ProductEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertProduct(product)
            cloudSyncManager.triggerAutoSync()
        }
    }

    fun addProduct(product: ProductEntity) = addNewProduct(product)

    fun updateProduct(product: ProductEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateProduct(product)
            cloudSyncManager.triggerAutoSync()
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteProduct(product)
            cloudSyncManager.triggerAutoSync()
        }
    }

    fun recordStockIn(
        product: ProductEntity,
        qty: Int,
        supplier: String,
        newBuyPrice: Double?,
        notes: String,
        timestamp: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.recordStockIn(product, qty, supplier, newBuyPrice, notes, timestamp)
            cloudSyncManager.triggerAutoSync()
        }
    }

    fun recordStockOut(
        product: ProductEntity,
        qty: Int,
        reason: String,
        notes: String,
        isNsb: Boolean = false,
        lossAmount: Double = 0.0,
        timestamp: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.recordStockOut(product, qty, reason, notes, isNsb, lossAmount, timestamp)
            val updated = repository.getProductById(product.id)
            if (updated != null && updated.stock <= updated.minStock) {
                NotificationHelper.showLowStockNotification(
                    app, updated.name, updated.stock, updated.minStock, updated.unit
                )
            }
            cloudSyncManager.triggerAutoSync()
        }
    }

    fun recordStockOpname(
        product: ProductEntity,
        actualStock: Int,
        notes: String,
        timestamp: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.recordStockOpname(product, actualStock, notes, timestamp)
            if (actualStock <= product.minStock) {
                NotificationHelper.showLowStockNotification(
                    app, product.name, actualStock, product.minStock, product.unit
                )
            }
            cloudSyncManager.triggerAutoSync()
        }
    }

    fun deleteStockMovement(movement: StockMovementEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteStockMovement(movement)
        }
    }

    fun clearStockMovements() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearLocalStockMovements()
        }
    }

    fun pullDataFromCloud(onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = cloudSyncManager.pullDataFromCloud()
            launch(Dispatchers.Main) {
                onComplete(success)
            }
        }
    }

    fun resetSpecificDatabase(category: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = cloudSyncManager.resetSpecificDatabase(category)
            if (category == "Semua Database" && success) {
                seedStarterNsbData()
            }
            launch(Dispatchers.Main) {
                onComplete(success)
            }
        }
    }

    fun restoreSpecificDatabase(category: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = cloudSyncManager.restoreSpecificDatabase(category)
            launch(Dispatchers.Main) {
                onComplete(success)
            }
        }
    }

    // Categories
    fun addCategory(name: String, colorHex: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertCategory(CategoryEntity(name = name, colorHex = colorHex))
        }
    }

    fun updateCategory(category: CategoryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateCategory(category)
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCategory(category)
        }
    }

    // Units (Master Satuan Dinamis)
    fun addUnit(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertUnit(UnitEntity(name = name.trim()))
        }
    }

    fun updateUnit(unit: UnitEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateUnit(unit)
        }
    }

    fun deleteUnit(unit: UnitEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteUnit(unit)
        }
    }

    // Promotions
    fun addPromotion(promotion: PromotionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertPromotion(promotion)
            recalculateCartPromotions()
        }
    }

    fun updatePromotion(promotion: PromotionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updatePromotion(promotion)
            recalculateCartPromotions()
        }
    }

    fun deletePromotion(promotion: PromotionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deletePromotion(promotion)
            recalculateCartPromotions()
        }
    }

    fun togglePromotion(id: Long, isActive: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.togglePromotion(id, isActive)
            recalculateCartPromotions()
        }
    }

    // Settings
    fun updateStoreSettings(settings: StoreSettingEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateStoreSettings(settings)
        }
    }

    // Reports Date Filter
    fun setReportPeriod(period: ReportPeriod) {
        _reportPeriod.value = period
    }

    fun setCustomReportDateRange(startDate: Long, endDate: Long) {
        _customStartDate.value = startDate
        _customEndDate.value = endDate
        _reportPeriod.value = ReportPeriod.RENTANG_TANGGAL
    }

    private fun calculateReportSummary(
        transactions: List<TransactionWithItems>,
        nsbMovements: List<StockMovementEntity> = emptyList()
    ): SalesReportSummary {
        var gross = 0.0
        var discount = 0.0
        var tax = 0.0
        var net = 0.0
        var costHpp = 0.0
        var itemsSold = 0
        var cash = 0.0
        var qris = 0.0
        var transfer = 0.0

        for (tx in transactions) {
            val t = tx.transaction
            gross += t.subtotal
            discount += t.discountAmount
            tax += t.taxAmount
            net += t.grandTotal

            when (t.paymentMethod) {
                "TUNAI" -> cash += t.grandTotal
                "QRIS" -> qris += t.grandTotal
                else -> transfer += t.grandTotal
            }

            for (item in tx.items) {
                itemsSold += item.quantity
                costHpp += (item.costPrice * item.quantity)
            }
        }

        var totalNsbLoss = 0.0
        var totalNsbItems = 0
        var nsbHilangLoss = 0.0
        var nsbHilangItems = 0
        var nsbHilangCount = 0
        var nsbRusakLoss = 0.0
        var nsbRusakItems = 0
        var nsbRusakCount = 0
        var nsbPenyesuaianLoss = 0.0
        var nsbPenyesuaianItems = 0
        var nsbPenyesuaianCount = 0

        for (move in nsbMovements) {
            val loss = if (move.lossAmount > 0.0) {
                move.lossAmount
            } else {
                Math.abs(move.quantity) * move.costPrice
            }
            val qty = Math.abs(move.quantity)
            totalNsbLoss += loss
            totalNsbItems += qty

            val r = move.reason.lowercase()
            when {
                r.contains("hilang") -> {
                    nsbHilangLoss += loss
                    nsbHilangItems += qty
                    nsbHilangCount++
                }
                r.contains("rusak") || r.contains("kadaluarsa") || r.contains("expired") || r.contains("pecah") || r.contains("basi") -> {
                    nsbRusakLoss += loss
                    nsbRusakItems += qty
                    nsbRusakCount++
                }
                else -> {
                    nsbPenyesuaianLoss += loss
                    nsbPenyesuaianItems += qty
                    nsbPenyesuaianCount++
                }
            }
        }

        val grossOperationalProfit = (net - tax) - costHpp
        val finalProfit = grossOperationalProfit - totalNsbLoss

        return SalesReportSummary(
            grossSales = gross,
            totalDiscount = discount,
            totalTax = tax,
            netSales = net,
            totalCostHpp = costHpp,
            estimatedProfit = finalProfit,
            grossOperationalProfit = grossOperationalProfit,
            totalTransactions = transactions.size,
            totalItemsSold = itemsSold,
            cashSales = cash,
            qrisSales = qris,
            transferSales = transfer,
            totalNsbLoss = totalNsbLoss,
            totalNsbItemsCount = totalNsbItems,
            nsbTransactionCount = nsbMovements.size,
            nsbHilangLoss = nsbHilangLoss,
            nsbHilangItems = nsbHilangItems,
            nsbHilangCount = nsbHilangCount,
            nsbRusakLoss = nsbRusakLoss,
            nsbRusakItems = nsbRusakItems,
            nsbRusakCount = nsbRusakCount,
            nsbPenyesuaianLoss = nsbPenyesuaianLoss,
            nsbPenyesuaianItems = nsbPenyesuaianItems,
            nsbPenyesuaianCount = nsbPenyesuaianCount
        )
    }

    private fun getStartOfDay(timestamp: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun getEndOfDay(timestamp: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return cal.timeInMillis
    }

    private fun getStartOfMonth(timestamp: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun getEndOfMonth(timestamp: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return cal.timeInMillis
    }

    private fun getStartOfYear(timestamp: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun getEndOfYear(timestamp: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.MONTH, Calendar.DECEMBER)
            set(Calendar.DAY_OF_MONTH, 31)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return cal.timeInMillis
    }
}
