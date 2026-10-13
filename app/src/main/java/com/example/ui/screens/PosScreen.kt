package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.PromotionEntity
import com.example.data.sync.SyncState
import com.example.ui.KasirViewModel
import com.example.ui.components.PaginationControlBar
import com.example.ui.components.paginate
import com.example.ui.components.QrisPaymentDialog
import com.example.ui.components.ReceiptDialog
import com.example.util.CurrencyFormatter
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    viewModel: KasirViewModel,
    onNavigateToInventory: () -> Unit,
    onNavigateToScanner: () -> Unit = { viewModel.openCameraScanner() }
) {
    val products by viewModel.products.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val cart by viewModel.cart.collectAsState()
    val lowStockCount by viewModel.lowStockCount.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()
    val posSearchQuery by viewModel.posSearchQuery.collectAsState()
    val promotions by viewModel.promotions.collectAsState()
    val storeSettings by viewModel.storeSettings.collectAsState()
    val isTaxApplied by viewModel.isTaxApplied.collectAsState()
    val customerName by viewModel.customerName.collectAsState()
    val showReceiptDialog by viewModel.showReceiptDialog.collectAsState()
    val lastCompletedTx by viewModel.lastCompletedTransaction.collectAsState()
    val showQrisDialog by viewModel.showQrisDialog.collectAsState()
    val isCameraScannerOpen by viewModel.isCameraScannerOpen.collectAsState()
    val notificationMsg by viewModel.notificationMessage.collectAsState()

    val isOnline by viewModel.cloudSyncManager.isOnline.collectAsState()
    val syncState by viewModel.cloudSyncManager.syncState.collectAsState()
    val unsyncedCount by viewModel.unsyncedCount.collectAsState()

    var showCheckoutSheet by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var cashPaidInput by remember { mutableStateOf("") }
    var posPage by remember { mutableIntStateOf(1) }
    var posPageSize by remember { mutableIntStateOf(12) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(notificationMsg) {
        notificationMsg?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearNotification()
        }
    }

    // Filter products
    val filteredProducts = products.filter { product ->
        val matchesCategory = selectedCategoryId == 0L || product.categoryId == selectedCategoryId
        val matchesSearch = posSearchQuery.isBlank() ||
                product.name.contains(posSearchQuery, ignoreCase = true) ||
                product.barcode.contains(posSearchQuery, ignoreCase = true)
        matchesCategory && matchesSearch
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                if (!isOnline) {
                    Surface(
                        color = Color(0xFFFFFBEB),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFFDE68A))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = "Mode Offline",
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Mode Offline: Data tersimpan lokal & auto-sinkron saat online.",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF92400E)
                            )
                        }
                    }
                } else if (unsyncedCount > 0) {
                    Surface(
                        color = Color(0xFFEFF6FF),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.cloudSyncManager.triggerAutoSync() }
                            .border(1.dp, Color(0xFFBFDBFE))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CloudSync,
                                    contentDescription = "Sync",
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "$unsyncedCount transaksi siap disinkronkan ke Cloud",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1E40AF)
                                )
                            }
                            Text(
                                text = "Sinkronkan",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2563EB)
                            )
                        }
                    }
                }

                if (lowStockCount > 0) {
                    Surface(
                        color = Color(0xFFFEF2F2),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToInventory() }
                            .border(1.dp, Color(0xFFFCA5A5))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Peringatan",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Text(
                                    text = "$lowStockCount produk menipis di bawah stok minimum!",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF991B1B)
                                )
                            }
                            Text(
                                text = "Periksa",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFEF4444)
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            // Floating Sticky Cart Button (For mobile layout when cart is not empty)
            AnimatedVisibility(visible = cart.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable {
                                cashPaidInput = viewModel.getGrandTotal().toLong().toString()
                                showCheckoutSheet = true
                            },
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFF006C4C),
                        shadowElevation = 8.dp,
                        tonalElevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.2f),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.ShoppingCart,
                                            contentDescription = "Keranjang",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    val totalItems = cart.sumOf { it.quantity }
                                    Text(
                                        text = "$totalItems Barang di Keranjang",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                    val promoDisc = viewModel.getCartPromoDiscount()
                                    if (promoDisc > 0) {
                                        Text(
                                            text = "Hemat ${CurrencyFormatter.formatRupiah(promoDisc)}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFDE047)
                                        )
                                    }
                                    Text(
                                        text = CurrencyFormatter.formatRupiah(viewModel.getGrandTotal()),
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White,
                                modifier = Modifier.testTag("open_cart_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Bayar",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF006C4C)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        tint = Color(0xFF006C4C),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isTabletLayout = maxWidth >= 760.dp

            Row(modifier = Modifier.fillMaxSize()) {
                // Main Product Catalog Column
                Column(
                    modifier = Modifier
                        .weight(if (isTabletLayout) 1.25f else 1f)
                        .fillMaxHeight()
                ) {
                    // Professional Top Toolbar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Kasir Penjualan",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Scan barcode atau pilih dari daftar produk",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    // Compact & Unified Search Bar with Barcode Scanner Icon
                    OutlinedTextField(
                        value = posSearchQuery,
                        onValueChange = {
                            viewModel.setPosSearchQuery(it)
                            posPage = 1
                        },
                        placeholder = { Text("Cari produk atau scan barcode...", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Cari",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                if (posSearchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { viewModel.setPosSearchQuery("") },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Clear,
                                            contentDescription = "Clear",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(2.dp))
                                }
                                IconButton(
                                    onClick = onNavigateToScanner,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("pos_scan_barcode_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = "Scan Barcode",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp)
                            .testTag("pos_search_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Horizontal Category Chips
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item {
                            val isSelected = selectedCategoryId == 0L
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    viewModel.setPosCategory(0L)
                                    posPage = 1
                                },
                                label = {
                                    Text(
                                        "Semua (${products.size})",
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                        }

                        items(categories) { cat ->
                            val isSelected = selectedCategoryId == cat.id
                            val prodCount = products.count { it.categoryId == cat.id }
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    viewModel.setPosCategory(cat.id)
                                    posPage = 1
                                },
                                label = {
                                    Text(
                                        "${cat.name} ($prodCount)",
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                        }
                    }

                    // Product Catalog: Grid or List Mode
                    if (filteredProducts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Empty",
                                    tint = Color(0xFFCBD5E1),
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Tidak ada produk yang cocok",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF334155)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Ubah kata kunci pencarian atau pilih kategori lain",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    } else {
                        val totalPages = kotlin.math.max(1, kotlin.math.ceil(filteredProducts.size.toDouble() / posPageSize).toInt())
                        val clampedPage = posPage.coerceIn(1, totalPages)
                        val paginatedProducts = filteredProducts.paginate(clampedPage, posPageSize)

                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(paginatedProducts, key = { it.id }) { product ->
                                val inCartQty = cart.find { it.product.id == product.id }?.quantity ?: 0
                                ProductRowPosList(
                                    product = product,
                                    inCartQty = inCartQty,
                                    onAddToCart = { viewModel.addToCart(product) }
                                )
                            }

                            item {
                                PaginationControlBar(
                                    currentPage = clampedPage,
                                    totalPages = totalPages,
                                    totalItems = filteredProducts.size,
                                    pageSize = posPageSize,
                                    onPageChange = { posPage = it },
                                    onPageSizeChange = {
                                        posPageSize = it
                                        posPage = 1
                                    },
                                    pageSizeOptions = listOf(6, 12, 24, 48),
                                    itemName = "produk",
                                    modifier = Modifier.padding(top = 6.dp, bottom = 12.dp)
                                )
                            }
                        }
                    }
                }

                // Tablet / Desktop Live Register Order Pane
                if (isTabletLayout) {
                    Surface(
                        modifier = Modifier
                            .weight(0.95f)
                            .fillMaxHeight(),
                        color = Color(0xFFF8FAFC),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            CheckoutSheetContent(
                                cart = cart,
                                promotions = promotions,
                                products = products,
                                subtotal = viewModel.getCartSubtotal(),
                                taxApplied = isTaxApplied,
                                taxPercent = storeSettings.taxRatePercent,
                                taxAmount = viewModel.getTaxAmount(),
                                grandTotal = viewModel.getGrandTotal(),
                                customerName = customerName,
                                onCustomerNameChange = { viewModel.setCustomerName(it) },
                                onItemQtyChange = { id, qty -> viewModel.updateCartItemQuantity(id, qty) },
                                onItemRemove = { viewModel.removeFromCart(it) },
                                onAddTebusProduct = { viewModel.addToCart(it) },
                                onClearCart = { viewModel.clearCart() },
                                onOpenCheckoutPayment = {
                                    cashPaidInput = ""
                                    showPaymentDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Checkout Bottom Sheet
    if (showCheckoutSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCheckoutSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.White,
            scrimColor = Color.Black.copy(alpha = 0.5f),
            dragHandle = { androidx.compose.material3.BottomSheetDefaults.DragHandle(color = Color(0xFFCBD5E1)) }
        ) {
            CheckoutSheetContent(
                cart = cart,
                promotions = promotions,
                products = products,
                subtotal = viewModel.getCartSubtotal(),
                taxApplied = isTaxApplied,
                taxPercent = storeSettings.taxRatePercent,
                taxAmount = viewModel.getTaxAmount(),
                grandTotal = viewModel.getGrandTotal(),
                customerName = customerName,
                onCustomerNameChange = { viewModel.setCustomerName(it) },
                onItemQtyChange = { id, qty -> viewModel.updateCartItemQuantity(id, qty) },
                onItemRemove = { viewModel.removeFromCart(it) },
                onAddTebusProduct = { viewModel.addToCart(it) },
                onClearCart = {
                    viewModel.clearCart()
                    showCheckoutSheet = false
                },
                onOpenCheckoutPayment = {
                    cashPaidInput = ""
                    showPaymentDialog = true
                }
            )
        }
    }

    // Modal Popup Pembayaran
    if (showPaymentDialog) {
        PaymentModalDialog(
            grandTotal = viewModel.getGrandTotal(),
            cashPaidInput = cashPaidInput,
            onCashInputChange = { cashPaidInput = it },
            onDismiss = { showPaymentDialog = false },
            onPayCash = { cashAmount ->
                viewModel.completeCheckout("TUNAI", cashAmount)
                showPaymentDialog = false
                showCheckoutSheet = false
            },
            onPayQris = {
                showPaymentDialog = false
                viewModel.openQrisDialog()
            }
        )
    }

    // Barcode Scanner Screen
    if (isCameraScannerOpen) {
        BarcodeScannerScreen(
            viewModel = viewModel,
            onNavigateToCheckout = { viewModel.closeCameraScanner() }
        )
    }

    // QRIS Dialog
    if (showQrisDialog) {
        QrisPaymentDialog(
            grandTotal = viewModel.getGrandTotal(),
            storeSettings = storeSettings,
            onPaymentConfirmed = {
                viewModel.completeCheckout("QRIS", viewModel.getGrandTotal())
                showCheckoutSheet = false
            },
            onDismiss = { viewModel.closeQrisDialog() }
        )
    }

    // Receipt Dialog
    if (showReceiptDialog && lastCompletedTx != null) {
        ReceiptDialog(
            transactionWithItems = lastCompletedTx!!,
            storeSettings = storeSettings,
            onDismiss = { viewModel.dismissReceiptDialog() }
        )
    }
}

@Composable
fun ProductCardPos(
    product: ProductEntity,
    inCartQty: Int = 0,
    onAddToCart: () -> Unit
) {
    val isOutOfStock = product.stock <= 0
    val isLowStock = product.stock <= product.minStock
    val cardShape = RoundedCornerShape(14.dp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .clickable(enabled = !isOutOfStock) { onAddToCart() }
            .testTag("product_item_${product.id}"),
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (isOutOfStock) Color(0xFFF8FAFC) else Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.5.dp,
            pressedElevation = 4.dp
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (inCartQty > 0) Color(0xFF0F9F68).copy(alpha = 0.5f) else Color(0xFFE2E8F0)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            // Visual Header Row: Product Initials Avatar Badge + In-Cart Count Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val avatarBg = remember(product.name) {
                    val colors = listOf(
                        Color(0xFFE0F2FE) to Color(0xFF0369A1),
                        Color(0xFFDCFCE7) to Color(0xFF15803D),
                        Color(0xFFFEF3C7) to Color(0xFFB45309),
                        Color(0xFFF3E8FF) to Color(0xFF7E22CE),
                        Color(0xFFFEE2E2) to Color(0xFFB91C1C)
                    )
                    colors[kotlin.math.abs(product.name.hashCode()) % colors.size]
                }
                val initials = remember(product.name) {
                    val words = product.name.trim().split("\\s+".toRegex())
                    if (words.size >= 2) {
                        "${words[0].take(1)}${words[1].take(1)}".uppercase()
                    } else {
                        product.name.take(2).uppercase()
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isOutOfStock) Color(0xFFF1F5F9) else avatarBg.first,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = initials,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isOutOfStock) Color(0xFF94A3B8) else avatarBg.second
                        )
                    }
                }

                if (inCartQty > 0) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFECFDF5),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA7F3D0))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF0F9F68),
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "$inCartQty",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF065F46)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Product Name
            Text(
                text = product.name,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp,
                color = if (isOutOfStock) Color(0xFF94A3B8) else Color(0xFF0F172A)
            )

            // Full Barcode (Clean text with barcode icon)
            val barcodeText = if (product.barcode.isNotBlank()) product.barcode else product.sku
            if (barcodeText.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        modifier = Modifier.size(11.dp),
                        tint = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = barcodeText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF64748B),
                        letterSpacing = 0.2.sp,
                        lineHeight = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Stock plain text below barcode (minimal spacing, plain text only)
            val stockText = when {
                isOutOfStock -> "Stok Habis"
                product.unit.isNotBlank() -> "Stok ${product.stock} ${product.unit}"
                else -> "Stok ${product.stock}"
            }
            Text(
                text = stockText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 11.sp,
                color = when {
                    isOutOfStock -> Color(0xFFDC2626)
                    isLowStock -> Color(0xFFD97706)
                    else -> Color(0xFF64748B)
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 1.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Price & Buy Button (Compact Row)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = CurrencyFormatter.formatRupiah(product.sellPrice),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.5.sp,
                    color = if (isOutOfStock) Color(0xFF94A3B8) else Color(0xFF0F9F68),
                    modifier = Modifier.weight(1f, fill = false)
                )

                Button(
                    onClick = onAddToCart,
                    enabled = !isOutOfStock,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(30.dp)
                        .testTag("add_to_cart_btn_${product.id}"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (inCartQty > 0) Color(0xFF0F9F68) else Color(0xFFECFDF5),
                        contentColor = if (inCartQty > 0) Color.White else Color(0xFF065F46)
                    )
                ) {
                    Icon(
                        imageVector = if (inCartQty > 0) Icons.Default.Add else Icons.Default.AddShoppingCart,
                        contentDescription = "Tambah",
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = when {
                            isOutOfStock -> "Habis"
                            inCartQty > 0 -> "+$inCartQty"
                            else -> "Beli"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ProductRowPosList(
    product: ProductEntity,
    inCartQty: Int = 0,
    onAddToCart: () -> Unit
) {
    val isOutOfStock = product.stock <= 0
    val isLowStock = product.stock <= product.minStock
    val cardShape = RoundedCornerShape(12.dp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .clickable(enabled = !isOutOfStock) { onAddToCart() }
            .testTag("product_item_${product.id}"),
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (isOutOfStock) Color(0xFFF8FAFC) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp, pressedElevation = 3.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (inCartQty > 0) Color(0xFF0F9F68).copy(alpha = 0.5f) else Color(0xFFE2E8F0)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val avatarBg = remember(product.name) {
                val colors = listOf(
                    Color(0xFFE0F2FE) to Color(0xFF0369A1),
                    Color(0xFFDCFCE7) to Color(0xFF15803D),
                    Color(0xFFFEF3C7) to Color(0xFFB45309),
                    Color(0xFFF3E8FF) to Color(0xFF7E22CE),
                    Color(0xFFFEE2E2) to Color(0xFFB91C1C)
                )
                colors[kotlin.math.abs(product.name.hashCode()) % colors.size]
            }
            val initials = remember(product.name) {
                val words = product.name.trim().split("\\s+".toRegex())
                if (words.size >= 2) "${words[0].take(1)}${words[1].take(1)}".uppercase()
                else product.name.take(2).uppercase()
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isOutOfStock) Color(0xFFF1F5F9) else avatarBg.first,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = initials,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isOutOfStock) Color(0xFF94A3B8) else avatarBg.second
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isOutOfStock) Color(0xFF94A3B8) else Color(0xFF0F172A)
                )

                val barcodeText = if (product.barcode.isNotBlank()) product.barcode else product.sku
                if (barcodeText.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            modifier = Modifier.size(10.dp),
                            tint = Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = barcodeText,
                            fontSize = 10.sp,
                            color = Color(0xFF64748B),
                            letterSpacing = 0.2.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                val stockText = when {
                    isOutOfStock -> "Stok Habis"
                    product.unit.isNotBlank() -> "Stok ${product.stock} ${product.unit}"
                    else -> "Stok ${product.stock}"
                }
                Text(
                    text = stockText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = when {
                        isOutOfStock -> Color(0xFFDC2626)
                        isLowStock -> Color(0xFFD97706)
                        else -> Color(0xFF64748B)
                    },
                    modifier = Modifier.padding(top = 1.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = CurrencyFormatter.formatRupiah(product.sellPrice),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    color = if (isOutOfStock) Color(0xFF94A3B8) else Color(0xFF0F9F68)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = onAddToCart,
                    enabled = !isOutOfStock,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier
                        .height(28.dp)
                        .testTag("add_to_cart_btn_${product.id}"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (inCartQty > 0) Color(0xFF0F9F68) else Color(0xFFECFDF5),
                        contentColor = if (inCartQty > 0) Color.White else Color(0xFF065F46)
                    )
                ) {
                    Text(
                        text = when {
                            isOutOfStock -> "Habis"
                            inCartQty > 0 -> "+$inCartQty"
                            else -> "+ Beli"
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun CheckoutSheetContent(
    cart: List<com.example.data.model.CartItem>,
    promotions: List<PromotionEntity> = emptyList(),
    products: List<ProductEntity> = emptyList(),
    subtotal: Double,
    taxApplied: Boolean,
    taxPercent: Double,
    taxAmount: Double,
    grandTotal: Double,
    customerName: String,
    onCustomerNameChange: (String) -> Unit,
    onItemQtyChange: (Long, Int) -> Unit,
    onItemRemove: (Long) -> Unit,
    onAddTebusProduct: (ProductEntity) -> Unit = {},
    onClearCart: () -> Unit,
    onOpenCheckoutPayment: () -> Unit
) {
    var dismissedPwpPromos by remember { mutableStateOf(setOf<Long>()) }

    // Hitung promosi Tebus Murah (PWP) aktif yang syarat belanjanya terpenuhi
    val activePwpPromos = remember(promotions, cart, products) {
        val totalCartSpend = cart.sumOf { it.totalOriginal }
        promotions.filter { promo ->
            if (!promo.isActive || promo.promoScheme != "PWP") return@filter false
            val minSpend = if (promo.minPurchaseAmount > 0.0) promo.minPurchaseAmount else 50000.0
            val rewardIds = if (promo.multiProductIds.isNotBlank()) {
                promo.multiProductIds.split(",").mapNotNull { it.trim().toLongOrNull() }
            } else if (promo.rewardProductId > 0L) {
                listOf(promo.rewardProductId)
            } else {
                emptyList()
            }
            val nonTebusSpend = if (rewardIds.isNotEmpty()) {
                cart.filter { it.product.id !in rewardIds }.sumOf { it.totalOriginal }
            } else {
                totalCartSpend
            }
            (nonTebusSpend >= minSpend) || (totalCartSpend >= minSpend)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
    ) {
        // Header (Non-scrolling)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Rincian Belanja Kasir",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            OutlinedButton(
                onClick = onClearCart,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF64748B),
                    containerColor = Color(0xFFF8FAFC)
                ),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                modifier = Modifier
                    .height(30.dp)
                    .testTag("reset_cart_btn")
            ) {
                Text(
                    text = "Reset",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF64748B)
                )
            }
        }

        // Scrollable Cart items and Tebus Murah promotions
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Cart items list
            cart.forEach { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.product.name,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "@ ${CurrencyFormatter.formatRupiah(item.unitPrice)}",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                                if (item.discountPerItem > 0) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "(-${CurrencyFormatter.formatRupiah(item.discountPerItem)})",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF16A34A)
                                    )
                                }
                            }
                            if (item.appliedPromoName.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFDCFCE7),
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Text(
                                        text = item.appliedPromoName,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF16A34A),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        // Qty Stepper
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onItemQtyChange(item.product.id, item.quantity - 1) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Kurang", modifier = Modifier.size(16.dp))
                            }
                            Text(
                                text = "${item.quantity}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 2.dp)
                            )
                            IconButton(
                                onClick = { onItemQtyChange(item.product.id, item.quantity + 1) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Tambah", modifier = Modifier.size(16.dp))
                            }
                        }

                        Text(
                            text = CurrencyFormatter.formatRupiah(item.subtotal),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.width(72.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.End
                        )

                        // Button Hapus Per Item (Warna Abu)
                        IconButton(
                            onClick = { onItemRemove(item.product.id) },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("delete_item_${item.product.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Hapus Item",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Tampilan Banner Tebus Murah Sesuai Desain (Hanya Muncul Jika Syarat Promo Terpenuhi & BELUM Diklaim)
            if (activePwpPromos.isNotEmpty()) {
                activePwpPromos.forEach { promo ->
                    if (promo.id !in dismissedPwpPromos) {
                        val rewardIds = if (promo.multiProductIds.isNotBlank()) {
                            promo.multiProductIds.split(",").mapNotNull { it.trim().toLongOrNull() }
                        } else if (promo.rewardProductId > 0L) {
                            listOf(promo.rewardProductId)
                        } else {
                            emptyList()
                        }
                        val matchedProducts = rewardIds.mapNotNull { id -> products.find { it.id == id } }
                        val rewardProducts = if (matchedProducts.isNotEmpty()) matchedProducts else products.take(3)
                        // Saring produk yang BELUM ada di keranjang kasir (belum diklaim)
                        val unclaimedRewardProducts = rewardProducts.filter { rProd ->
                            cart.none { it.product.id == rProd.id }
                        }

                        // Tampilkan banner hanya jika masih ada produk tebus murah yang belum diklaim
                        if (unclaimedRewardProducts.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            unclaimedRewardProducts.forEach { rProduct ->
                                val tebusPrice = if (promo.rewardSpecialPrice > 0.0) {
                                    promo.rewardSpecialPrice
                                } else if (promo.type.equals("PERCENTAGE", ignoreCase = true)) {
                                    rProduct.sellPrice * (1.0 - (promo.value / 100.0).coerceIn(0.0, 1.0))
                                } else {
                                    (rProduct.sellPrice - promo.value).coerceAtLeast(0.0)
                                }

                                val discountLabel = if (promo.rewardSpecialPrice > 0.0) {
                                    "Spesial"
                                } else if (promo.type.equals("PERCENTAGE", ignoreCase = true)) {
                                    "${promo.value.toInt()}%"
                                } else {
                                    CurrencyFormatter.formatRupiah(promo.value)
                                }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF86EFAC)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp)
                                    ) {
                                        // Header Penawaran Tebus Murah
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.LocalOffer,
                                                contentDescription = null,
                                                tint = Color(0xFF16A34A),
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "PENAWARAN TEBUS MURAH",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = Color(0xFF16A34A),
                                                letterSpacing = 0.5.sp
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        // Nama Produk
                                        Text(
                                            text = rProduct.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Color(0xFF0F172A),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // Baris Harga & Badge Diskon
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = CurrencyFormatter.formatRupiah(rProduct.sellPrice),
                                                fontSize = 12.sp,
                                                color = Color(0xFF94A3B8),
                                                textDecoration = TextDecoration.LineThrough
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = CurrencyFormatter.formatRupiah(tebusPrice),
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF16A34A)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color(0xFFDCFCE7)
                                            ) {
                                                Text(
                                                    text = "-$discountLabel",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF16A34A),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Tombol Aksi: Ambil Promo & Nanti
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Button(
                                                onClick = { onAddTebusProduct(rProduct) },
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                                modifier = Modifier
                                                    .weight(1.3f)
                                                    .height(42.dp)
                                            ) {
                                                Text(
                                                    text = "Ambil Promo",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }

                                            OutlinedButton(
                                                onClick = { dismissedPwpPromos = dismissedPwpPromos + promo.id },
                                                shape = RoundedCornerShape(10.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    contentColor = Color(0xFF475569),
                                                    containerColor = Color.White
                                                ),
                                                modifier = Modifier
                                                    .weight(0.9f)
                                                    .height(42.dp)
                                        ) {
                                                Text(
                                                    text = "Nanti",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF475569)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // STICKY BOTTOM GROUP: Subtotal, Diskon, PPN, Total Tagihan & Button Checkout
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(top = 4.dp)
        ) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Summary Rows
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Subtotal (Sebelum Diskon)", fontSize = 12.sp, color = Color.Gray)
                Text(text = CurrencyFormatter.formatRupiah(subtotal), fontSize = 12.sp)
            }
            val promoDiscount = cart.sumOf { it.totalDiscount }
            if (promoDiscount > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Total Diskon Promosi", fontSize = 12.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.SemiBold)
                    Text(text = "-${CurrencyFormatter.formatRupiah(promoDiscount)}", fontSize = 12.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
                }
            }
            if (taxApplied && taxAmount > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "PPN ($taxPercent%)", fontSize = 12.sp, color = Color.Gray)
                    Text(text = CurrencyFormatter.formatRupiah(taxAmount), fontSize = 12.sp)
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Total Tagihan", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(
                    text = CurrencyFormatter.formatRupiah(grandTotal),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Button Checkout Sticky (Hanya teks Checkout + Ikon)
            Button(
                onClick = onOpenCheckoutPayment,
                enabled = cart.isNotEmpty(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("checkout_payment_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.ShoppingCartCheckout, contentDescription = "Checkout", modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "Checkout",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun PaymentModalDialog(
    grandTotal: Double,
    cashPaidInput: String,
    onCashInputChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onPayCash: (Double) -> Unit,
    onPayQris: () -> Unit
) {
    val cashPaid = cashPaidInput.toDoubleOrNull() ?: 0.0
    val change = (cashPaid - grandTotal).coerceAtLeast(0.0)
    val shortage = (grandTotal - cashPaid).coerceAtLeast(0.0)
    val hasInput = cashPaidInput.isNotBlank()
    val isCashSufficient = hasInput && cashPaid >= grandTotal

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pembayaran",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF0F172A)
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Total Tagihan Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF86EFAC))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Total Tagihan",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = CurrencyFormatter.formatRupiah(grandTotal),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF16A34A)
                        )
                    }
                }

                // Tombol Cepat Nominal Uang Pas & Pecahan Standar Kasir
                val quickAmounts = remember(grandTotal) {
                    val base = grandTotal.toLong()
                    val list = mutableListOf<Long>()
                    list.add(base) // Uang Pas
                    val standardDenoms = listOf(10_000L, 20_000L, 50_000L, 100_000L, 200_000L)
                    for (d in standardDenoms) {
                        if (d > base && !list.contains(d)) {
                            list.add(d)
                        }
                    }
                    val nextRound10k = ((base + 9999) / 10000) * 10000
                    if (nextRound10k > base && !list.contains(nextRound10k)) {
                        list.add(nextRound10k)
                    }
                    list.take(4)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickAmounts.forEach { amount ->
                        val isSelected = cashPaidInput == amount.toString()
                        val label = if (amount == grandTotal.toLong()) "Uang Pas" else CurrencyFormatter.formatRupiah(amount.toDouble())
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Color(0xFFDCFCE7) else Color(0xFFF1F5F9),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFF16A34A) else Color(0xFFCBD5E1)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onCashInputChange(amount.toString()) }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color(0xFF15803D) else Color(0xFF334155),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Kolom Nominal Tunai (Rp) - Default Kosong
                OutlinedTextField(
                    value = cashPaidInput,
                    onValueChange = { newVal ->
                        if (newVal.all { it.isDigit() }) {
                            onCashInputChange(newVal)
                        }
                    },
                    label = { Text("Kolom Nominal Tunai (Rp)") },
                    placeholder = { Text("Masukkan nominal uang tunai...") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF16A34A),
                        focusedLabelColor = Color(0xFF16A34A)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cash_paid_input")
                )

                // Kotak Kembalian
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCashSufficient) Color(0xFFF0FDF4) else Color(0xFFF8FAFC)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isCashSufficient) Color(0xFF86EFAC) else Color(0xFFE2E8F0)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Kembalian:",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = Color(0xFF334155)
                        )
                        Text(
                            text = if (!hasInput) {
                                "Rp 0"
                            } else if (isCashSufficient) {
                                CurrencyFormatter.formatRupiah(change)
                            } else {
                                "Kurang ${CurrencyFormatter.formatRupiah(shortage)}"
                            },
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = if (isCashSufficient) Color(0xFF16A34A) else if (hasInput) Color(0xFFDC2626) else Color(0xFF64748B)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Tombol Pembayaran Tunai & QRIS (Tampilan Rapi Single Line)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onPayCash(cashPaid) },
                        enabled = isCashSufficient,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("pay_cash_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF16A34A),
                            disabledContainerColor = Color(0xFFE2E8F0),
                            disabledContentColor = Color(0xFF94A3B8)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = "Tunai",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Bayar Tunai",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }

                    Button(
                        onClick = onPayQris,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("pay_qris_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "QRIS",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Bayar QRIS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}
