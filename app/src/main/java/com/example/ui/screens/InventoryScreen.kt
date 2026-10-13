package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.UnitEntity
import com.example.ui.KasirViewModel
import com.example.ui.components.PaginationControlBar
import com.example.ui.components.paginate
import com.example.util.CurrencyFormatter
import com.example.util.Ean13Utils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel: KasirViewModel,
    onNavigateToCategories: () -> Unit,
    initialTab: Int = 0
) {
    val context = LocalContext.current
    val products by viewModel.products.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val units by viewModel.units.collectAsState()
    val lowStockCount by viewModel.lowStockCount.collectAsState()
    val allNsbMovements by viewModel.allNsbMovements.collectAsState()
    val summary by viewModel.reportSummary.collectAsState()

    var selectedSubTab by remember { mutableIntStateOf(initialTab) } // 0: Inventaris Produk, 1: Mutasi Stok, 2: NSB
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf(0L) }
    var stockFilterState by remember { mutableStateOf("SEMUA") } // "SEMUA", "KRITIS", "HABIS"
    var filterCategoryDropdownExpanded by remember { mutableStateOf(false) }
    var inventoryPage by remember { mutableIntStateOf(1) }
    var inventoryPageSize by remember { mutableIntStateOf(10) }

    // Dialog States
    var showAddProductDialog by remember { mutableStateOf(false) }
    var showRecordNsbDialog by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<ProductEntity?>(null) }
    var productToDelete by remember { mutableStateOf<ProductEntity?>(null) }

    val criticalCount = products.count { it.stock in 1..it.minStock }
    val outOfStockCount = products.count { it.stock <= 0 }
    val totalCount = products.size

    val filteredProducts = products.filter { product ->
        val matchesCategory = selectedCategoryId == 0L || product.categoryId == selectedCategoryId
        val matchesSearch = searchQuery.isBlank() ||
                product.name.contains(searchQuery, ignoreCase = true) ||
                product.barcode.contains(searchQuery, ignoreCase = true) ||
                product.sku.contains(searchQuery, ignoreCase = true)
        val matchesStock = when (stockFilterState) {
            "KRITIS" -> product.stock in 1..product.minStock
            "HABIS" -> product.stock <= 0
            else -> true
        }
        matchesCategory && matchesSearch && matchesStock
    }

    val totalAssetValue = products.sumOf { it.buyPrice * it.stock }

    Scaffold(
        floatingActionButton = {
            if (selectedSubTab == 0) {
                FloatingActionButton(
                    onClick = { showAddProductDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_product_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah Produk")
                }
            } else if (selectedSubTab == 2) {
                FloatingActionButton(
                    onClick = { showRecordNsbDialog = true },
                    containerColor = Color(0xFFDC2626),
                    contentColor = Color.White,
                    modifier = Modifier.testTag("record_nsb_fab")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Catat NSB", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Catat NSB", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Dynamic Screen Header Title & Subtitle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = when (selectedSubTab) {
                            1 -> "Mutasi Stok"
                            2 -> "Nota Selisih Barang (NSB)"
                            else -> "Inventaris Produk"
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = when (selectedSubTab) {
                            1 -> "Pencatatan mutasi stok masuk & stok keluar"
                            2 -> "Rekap kerugian barang hilang, rusak & penyesuaian"
                            else -> "Kelola harga jual, stok barang & batas minimum"
                        },
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            // 3 Sub Tabs: Inventaris Produk - Mutasi Stok - NSB
            TabRow(
                selectedTabIndex = selectedSubTab,
                containerColor = Color.White,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedSubTab == 0,
                    onClick = { selectedSubTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Inventaris", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = selectedSubTab == 1,
                    onClick = { selectedSubTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Mutasi Stok", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = selectedSubTab == 2,
                    onClick = { selectedSubTab = 2 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("NSB", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            // Sub Tab Content
            when (selectedSubTab) {
                0 -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // 1. Dashboard Card: Compact & Simple
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Total Nilai Aset (HPP)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF64748B)
                            )
                        }
                        Text(
                            text = CurrencyFormatter.formatRupiah(totalAssetValue),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF1F5F9)
                    ) {
                        Text(
                            text = "$totalCount Varian",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF334155),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // 2. Search Bar (Left) & Category Filter Dropdown (Right) side-by-side
            val selectedCategoryName = if (selectedCategoryId == 0L) "Semua Kategori" else (categories.find { it.id == selectedCategoryId }?.name ?: "Semua Kategori")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Kolom pencarian di sebelah kiri
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari produk...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Cari", modifier = Modifier.size(16.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Hapus", modifier = Modifier.size(14.dp))
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1.1f)
                        .testTag("inventory_search_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Kolom kategori di sebelah kanan (Dropdown)
                ExposedDropdownMenuBox(
                    expanded = filterCategoryDropdownExpanded,
                    onExpandedChange = { filterCategoryDropdownExpanded = !filterCategoryDropdownExpanded },
                    modifier = Modifier.weight(0.9f)
                ) {
                    OutlinedTextField(
                        value = selectedCategoryName,
                        onValueChange = {},
                        readOnly = true,
                        leadingIcon = { Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = filterCategoryDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = filterCategoryDropdownExpanded,
                        onDismissRequest = { filterCategoryDropdownExpanded = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Semua Kategori", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)) },
                            onClick = {
                                selectedCategoryId = 0L
                                filterCategoryDropdownExpanded = false
                            },
                            modifier = Modifier.background(Color.White)
                        )
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name, fontSize = 12.sp, color = Color(0xFF0F172A)) },
                                onClick = {
                                    selectedCategoryId = cat.id
                                    filterCategoryDropdownExpanded = false
                                },
                                modifier = Modifier.background(Color.White)
                            )
                        }
                    }
                }
            }

            // 3. Stock Status Filter Chips (Semua, Stok Kritis/Minim, Stok Habis)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = stockFilterState == "SEMUA",
                    onClick = { stockFilterState = "SEMUA" },
                    label = { Text("Semua ($totalCount)", fontSize = 11.sp, fontWeight = if (stockFilterState == "SEMUA") FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White
                    )
                )

                FilterChip(
                    selected = stockFilterState == "KRITIS",
                    onClick = { stockFilterState = "KRITIS" },
                    label = { Text("Stok Kritis ($criticalCount)", fontSize = 11.sp, fontWeight = if (stockFilterState == "KRITIS") FontWeight.Bold else FontWeight.Normal) },
                    leadingIcon = {
                        if (criticalCount > 0) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = if (stockFilterState == "KRITIS") Color.White else Color(0xFFD97706), modifier = Modifier.size(13.dp))
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFD97706),
                        selectedLabelColor = Color.White
                    )
                )

                FilterChip(
                    selected = stockFilterState == "HABIS",
                    onClick = { stockFilterState = "HABIS" },
                    label = { Text("Habis ($outOfStockCount)", fontSize = 11.sp, fontWeight = if (stockFilterState == "HABIS") FontWeight.Bold else FontWeight.Normal) },
                    leadingIcon = {
                        if (outOfStockCount > 0) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = if (stockFilterState == "HABIS") Color.White else Color(0xFFDC2626), modifier = Modifier.size(13.dp))
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFDC2626),
                        selectedLabelColor = Color.White
                    )
                )
            }

            // Product List
            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Inventory,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tidak Ada Produk",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (searchQuery.isNotBlank()) "Tidak ditemukan produk dengan kata kunci \"$searchQuery\"" else "Tekan tombol + untuk menambahkan produk baru",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            } else {
                val totalPages = kotlin.math.max(1, kotlin.math.ceil(filteredProducts.size.toDouble() / inventoryPageSize).toInt())
                val clampedPage = inventoryPage.coerceIn(1, totalPages)
                val paginatedProducts = filteredProducts.paginate(clampedPage, inventoryPageSize)

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 84.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(paginatedProducts, key = { it.id }) { product ->
                        val cat = categories.find { it.id == product.categoryId }
                        ProductMobileCard(
                            product = product,
                            categoryName = cat?.name ?: "Tanpa Kategori",
                            categoryColorHex = cat?.colorHex ?: "#006C4C",
                            onClick = { productToEdit = product },
                            onDeleteClick = { productToDelete = product }
                        )
                    }

                    item {
                        PaginationControlBar(
                            currentPage = clampedPage,
                            totalPages = totalPages,
                            totalItems = filteredProducts.size,
                            pageSize = inventoryPageSize,
                            onPageChange = { inventoryPage = it },
                            onPageSizeChange = { 
                                inventoryPageSize = it
                                inventoryPage = 1
                            },
                            itemName = "produk",
                            modifier = Modifier.padding(top = 6.dp, bottom = 12.dp)
                        )
                    }
                }
            }
        }
    }
    1 -> {
        StockMovementScreen(
            viewModel = viewModel,
            showHeader = false
        )
    }
    2 -> {
        NsbHistoryTab(
            nsbMovements = allNsbMovements,
            summary = summary,
            onDeleteMovement = { viewModel.deleteStockMovement(it) },
            onSeedSampleData = { viewModel.seedStarterNsbData() },
            onAddNsbClick = { showRecordNsbDialog = true }
        )
    }
}
}
}

    // Add Product Dialog
    if (showAddProductDialog) {
        AddEditProductDialog(
            product = null,
            categories = categories,
            units = units,
            onDismiss = { showAddProductDialog = false },
            onSave = { newProduct ->
                viewModel.addProduct(newProduct)
                showAddProductDialog = false
                Toast.makeText(context, "Produk '${newProduct.name}' berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
            },
            onAddNewUnit = { unitName -> viewModel.addUnit(unitName) },
            onUpdateUnit = { unit -> viewModel.updateUnit(unit) },
            onDeleteUnit = { unit -> viewModel.deleteUnit(unit) },
            onAddCategory = { name, color -> viewModel.addCategory(name, color) },
            onUpdateCategory = { cat -> viewModel.updateCategory(cat) },
            onDeleteCategory = { cat -> viewModel.deleteCategory(cat) }
        )
    }

    // Edit Product Dialog (Direct Card Click)
    if (productToEdit != null) {
        AddEditProductDialog(
            product = productToEdit,
            categories = categories,
            units = units,
            onDismiss = { productToEdit = null },
            onSave = { updatedProduct ->
                viewModel.updateProduct(updatedProduct)
                productToEdit = null
                Toast.makeText(context, "Data produk '${updatedProduct.name}' berhasil diperbarui!", Toast.LENGTH_SHORT).show()
            },
            onAddNewUnit = { unitName -> viewModel.addUnit(unitName) },
            onUpdateUnit = { unit -> viewModel.updateUnit(unit) },
            onDeleteUnit = { unit -> viewModel.deleteUnit(unit) },
            onAddCategory = { name, color -> viewModel.addCategory(name, color) },
            onUpdateCategory = { cat -> viewModel.updateCategory(cat) },
            onDeleteCategory = { cat -> viewModel.deleteCategory(cat) }
        )
    }

    // Delete Confirmation Dialog
    if (productToDelete != null) {
        val prod = productToDelete!!
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            containerColor = Color.White,
            title = { Text("Hapus Produk?") },
            text = { Text("Apakah Anda yakin ingin menghapus '${prod.name}'? Produk ini akan dihapus dari aplikasi dan cloud database.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProduct(prod)
                        productToDelete = null
                        Toast.makeText(context, "Produk berhasil dihapus", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { productToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }

    if (showRecordNsbDialog) {
        RecordNsbDialog(
            products = products,
            onDismiss = { showRecordNsbDialog = false },
            onSave = { prod, qty, reason, notes, lossAmount, timestamp ->
                showRecordNsbDialog = false
                viewModel.recordStockOut(prod, qty, reason, notes, isNsb = true, lossAmount = lossAmount, timestamp = timestamp)
                Toast.makeText(context, "Catatan NSB '${prod.name}' berhasil disimpan!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordNsbDialog(
    products: List<ProductEntity>,
    onDismiss: () -> Unit,
    onSave: (product: ProductEntity, qty: Int, reason: String, notes: String, lossAmount: Double, timestamp: Long) -> Unit
) {
    val context = LocalContext.current
    val availableProducts = remember(products) { products.filter { it.stock > 0 } }
    var selectedProduct by remember { mutableStateOf<ProductEntity?>(availableProducts.firstOrNull()) }
    var productDropdownExpanded by remember { mutableStateOf(false) }
    val reasonOptions = listOf("Rusak/Kadaluarsa", "Hilang", "Penyesuaian")
    var selectedReason by remember { mutableStateOf(reasonOptions[0]) }
    var reasonDropdownExpanded by remember { mutableStateOf(false) }
    var qtyText by remember { mutableStateOf("1") }
    var notesText by remember { mutableStateOf("") }
    var recordTimestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var isSubmitting by remember { mutableStateOf(false) }

    val qty = qtyText.toIntOrNull() ?: 1
    val unitPrice = if ((selectedProduct?.buyPrice ?: 0.0) > 0.0) selectedProduct!!.buyPrice else (selectedProduct?.sellPrice ?: 0.0)
    val totalLoss = qty * unitPrice

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Text("Catat Nota Selisih Barang (NSB)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Pilih Tanggal & Waktu
                Text("Tanggal & Waktu", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            val cal = Calendar.getInstance().apply { timeInMillis = recordTimestamp }
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    TimePickerDialog(
                                        context,
                                        { _, hourOfDay, minute ->
                                            val updatedCal = Calendar.getInstance().apply {
                                                set(year, month, dayOfMonth, hourOfDay, minute, 0)
                                            }
                                            recordTimestamp = updatedCal.timeInMillis
                                        },
                                        cal.get(Calendar.HOUR_OF_DAY),
                                        cal.get(Calendar.MINUTE),
                                        true
                                    ).show()
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF1F5F9),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color(0xFF006C4C), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = CurrencyFormatter.formatDate(recordTimestamp, "EEEE, dd MMM yyyy • HH:mm"),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Ketuk untuk ganti tanggal / jam",
                                fontSize = 10.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }

                Text("Pilih Produk", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
                ExposedDropdownMenuBox(
                    expanded = productDropdownExpanded,
                    onExpandedChange = { productDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedProduct?.name ?: "Pilih Produk",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = productDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        singleLine = true
                    )
                    ExposedDropdownMenu(
                        expanded = productDropdownExpanded,
                        onDismissRequest = { productDropdownExpanded = false }
                    ) {
                        availableProducts.forEach { prod ->
                            DropdownMenuItem(
                                text = { Text("${prod.name} (Stok: ${prod.stock} ${prod.unit})") },
                                onClick = {
                                    selectedProduct = prod
                                    productDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Text("Alasan Pengeluaran / Kategori Kerugian", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
                ExposedDropdownMenuBox(
                    expanded = reasonDropdownExpanded,
                    onExpandedChange = { reasonDropdownExpanded = !reasonDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedReason,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Pilih Alasan", fontSize = 12.sp) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = reasonDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedBorderColor = Color(0xFFDC2626)
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = reasonDropdownExpanded,
                        onDismissRequest = { reasonDropdownExpanded = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        reasonOptions.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt, fontWeight = if (selectedReason == opt) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp) },
                                onClick = {
                                    selectedReason = opt
                                    reasonDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                val isQtyExceed = selectedProduct != null && qty > (selectedProduct?.stock ?: 0)
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { input ->
                        val digits = input.filter { c -> c.isDigit() }
                        qtyText = digits
                    },
                    isError = isQtyExceed,
                    supportingText = if (isQtyExceed) {
                        {
                            Text(
                                "Jumlah melebihi stok yang tersedia (${selectedProduct!!.stock} ${selectedProduct!!.unit})!",
                                color = Color(0xFFDC2626),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else null,
                    label = { Text("Jumlah (Qty ${selectedProduct?.unit ?: "pcs"})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFEE2E2),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total Kerugian (HPP):", fontSize = 11.sp, color = Color(0xFF991B1B), fontWeight = FontWeight.Medium)
                        Text(
                            CurrencyFormatter.formatRupiah(totalLoss),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFDC2626)
                        )
                    }
                }

                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Keterangan / Alasan") },
                    placeholder = { Text("Contoh: Kemasan sobek saat bongkar muat...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isSubmitting) return@Button
                    val prod = selectedProduct
                    if (prod == null) {
                        Toast.makeText(context, "Silakan pilih produk terlebih dahulu!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (prod.stock <= 0) {
                        Toast.makeText(context, "Stok '${prod.name}' sudah habis (0)! Tidak dapat memproses NSB.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (qty > prod.stock) {
                        Toast.makeText(context, "Jumlah QTY ($qty) melebihi stok yang tersedia (${prod.stock})!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (qty > 0) {
                        isSubmitting = true
                        onSave(prod, qty, selectedReason, notesText, totalLoss, recordTimestamp)
                    }
                },
                enabled = !isSubmitting && selectedProduct != null && qty > 0 && qty <= (selectedProduct?.stock ?: 0),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
            ) {
                Text(if (isSubmitting) "Menyimpan..." else "Simpan NSB", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
fun ProductMobileCard(
    product: ProductEntity,
    categoryName: String,
    categoryColorHex: String,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val isOutOfStock = product.stock <= 0
    val isLowStock = product.stock <= product.minStock
    val margin = product.sellPrice - product.buyPrice
    val marginPercent = if (product.buyPrice > 0) (margin / product.buyPrice) * 100 else 0.0

    val catColor = try {
        Color(android.graphics.Color.parseColor(categoryColorHex))
    } catch (_: Exception) {
        MaterialTheme.colorScheme.primary
    }

    val cardShape = RoundedCornerShape(12.dp)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = cardShape,
                clip = false,
                ambientColor = Color(0x1F000000),
                spotColor = Color(0x1F000000)
            )
            .clip(cardShape)
            .clickable(onClick = onClick),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            // Header Row: Category Badge + Stock Badge + Quick Delete Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = catColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = categoryName,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = catColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    val stockBadgeBg = when {
                        isOutOfStock -> Color(0xFFFEE2E2)
                        isLowStock -> Color(0xFFFEF3C7)
                        else -> Color(0xFFDCFCE7)
                    }
                    val stockBadgeText = when {
                        isOutOfStock -> Color(0xFFDC2626)
                        isLowStock -> Color(0xFFD97706)
                        else -> Color(0xFF16A34A)
                    }
                    val stockLabel = when {
                        isOutOfStock -> "Habis (${product.stock} ${product.unit})"
                        isLowStock -> "Kritis: ${product.stock} ${product.unit}"
                        else -> "Stok: ${product.stock} ${product.unit}"
                    }

                    Surface(
                        color = stockBadgeBg,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isLowStock || isOutOfStock) {
                                Icon(
                                    imageVector = if (isOutOfStock) Icons.Default.ErrorOutline else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = stockBadgeText,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                            }
                            Text(
                                text = stockLabel,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = stockBadgeText
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Hapus Produk",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Body: Product Name & Code on left, Sell Price & Margin on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Text(
                        text = product.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Modal: ${CurrencyFormatter.formatRupiah(product.buyPrice)}",
                        fontSize = 10.sp,
                        color = Color(0xFF64748B)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = CurrencyFormatter.formatRupiah(product.sellPrice),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Laba: +${CurrencyFormatter.formatRupiah(margin)} (${String.format("%.0f", marginPercent)}%)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (margin >= 0) Color(0xFF16A34A) else Color(0xFFDC2626)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProductDialog(
    product: ProductEntity?,
    categories: List<CategoryEntity>,
    units: List<UnitEntity>,
    onDismiss: () -> Unit,
    onSave: (ProductEntity) -> Unit,
    onAddNewUnit: (String) -> Unit,
    onUpdateUnit: (UnitEntity) -> Unit = {},
    onDeleteUnit: (UnitEntity) -> Unit = {},
    onAddCategory: (String, String) -> Unit = { _, _ -> },
    onUpdateCategory: (CategoryEntity) -> Unit = {},
    onDeleteCategory: (CategoryEntity) -> Unit = {}
) {
    var sku by remember { mutableStateOf(product?.sku ?: "") }
    var barcode by remember { mutableStateOf(product?.barcode ?: "") }
    var barcodeError by remember { mutableStateOf<String?>(null) }
    var name by remember { mutableStateOf(product?.name ?: "") }
    var selectedCatId by remember { mutableStateOf(product?.categoryId ?: (categories.firstOrNull()?.id ?: 0L)) }
    var buyPriceText by remember { mutableStateOf(if (product != null) product.buyPrice.toLong().toString() else "") }
    var sellPriceText by remember { mutableStateOf(if (product != null) product.sellPrice.toLong().toString() else "") }
    var minStockText by remember { mutableStateOf(product?.minStock?.toString() ?: "5") }
    var selectedUnit by remember { mutableStateOf(product?.unit?.ifBlank { "pcs" } ?: "pcs") }

    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var unitDropdownExpanded by remember { mutableStateOf(false) }

    // Dialogs for Unit CRUD
    var showAddCustomUnitDialog by remember { mutableStateOf(false) }
    var unitToEdit by remember { mutableStateOf<UnitEntity?>(null) }
    var unitToDelete by remember { mutableStateOf<UnitEntity?>(null) }

    // Dialogs for Category CRUD
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<CategoryEntity?>(null) }
    var categoryToDelete by remember { mutableStateOf<CategoryEntity?>(null) }

    val currentStock = product?.stock ?: 0

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFECFDF5),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = null,
                            tint = Color(0xFF006C4C),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (product == null) "Tambah Produk Baru" else "Detail & Edit Produk",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color(0xFF0F172A)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Section 1: Identitas Produk
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("IDENTITAS PRODUK", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Nama Produk *") },
                            leadingIcon = { Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            placeholder = { Text("Contoh: Gula Pasir 1kg") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )

                        OutlinedTextField(
                            value = sku,
                            onValueChange = { sku = it },
                            label = { Text("SKU") },
                            leadingIcon = { Icon(Icons.Default.Tag, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            placeholder = { Text("Contoh: SKU-7483012") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )

                        OutlinedTextField(
                            value = barcode,
                            onValueChange = { input ->
                                val filtered = input.filter { it.isDigit() }.take(13)
                                barcode = filtered
                                barcodeError = if (filtered.isNotBlank() && filtered.length != 13) {
                                    "EAN-13 harus 13 digit angka (${filtered.length}/13)"
                                } else if (filtered.length == 13 && !Ean13Utils.isValidEan13(filtered)) {
                                    val expected = Ean13Utils.calculateCheckDigit(filtered.substring(0, 12))
                                    "Digit cek EAN-13 salah! Harus berakhiran $expected"
                                } else {
                                    null
                                }
                            },
                            label = { Text("Barcode") },
                            leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            isError = barcodeError != null,
                            supportingText = {
                                if (barcodeError != null) {
                                    Text(barcodeError ?: "", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                                } else {
                                    Text("Standar EAN-13", fontSize = 11.sp, color = Color.Gray)
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            placeholder = { Text("Contoh: 8991234567890") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                // Section 2: Kategori & Satuan (Full Inline CRUD)
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("KATEGORI & SATUAN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))

                        // Category Dropdown with Inline Edit & Delete
                        ExposedDropdownMenuBox(
                            expanded = categoryDropdownExpanded,
                            onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
                        ) {
                            val currentCatName = categories.find { it.id == selectedCatId }?.name ?: "Pilih Kategori"
                            OutlinedTextField(
                                value = currentCatName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Kategori Produk") },
                                leadingIcon = { Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = categoryDropdownExpanded,
                                onDismissRequest = { categoryDropdownExpanded = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                categories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(cat.name, color = Color(0xFF0F172A), modifier = Modifier.weight(1f))
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                    modifier = Modifier.clickable(
                                                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                                        indication = null,
                                                        onClick = { /* Consumed to prevent dropdown selection */ }
                                                    )
                                                ) {
                                                    IconButton(
                                                        onClick = {
                                                            categoryToEdit = cat
                                                            categoryDropdownExpanded = false
                                                        },
                                                        modifier = Modifier.size(26.dp)
                                                    ) {
                                                        Icon(Icons.Default.Edit, contentDescription = "Edit Kategori", tint = Color(0xFF0284C7), modifier = Modifier.size(14.dp))
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            categoryToDelete = cat
                                                            categoryDropdownExpanded = false
                                                        },
                                                        modifier = Modifier.size(26.dp)
                                                    ) {
                                                        Icon(Icons.Default.Delete, contentDescription = "Hapus Kategori", tint = Color(0xFFDC2626), modifier = Modifier.size(14.dp))
                                                    }
                                                }
                                            }
                                        },
                                        onClick = {
                                            selectedCatId = cat.id
                                            categoryDropdownExpanded = false
                                        },
                                        modifier = Modifier.background(Color.White)
                                    )
                                }
                                HorizontalDivider(color = Color(0xFFE2E8F0))
                                DropdownMenuItem(
                                    text = {
                                        Text("+ Tambah Kategori Baru...", fontWeight = FontWeight.Bold, color = Color(0xFF006C4C))
                                    },
                                    onClick = {
                                        categoryDropdownExpanded = false
                                        showAddCategoryDialog = true
                                    },
                                    modifier = Modifier.background(Color.White)
                                )
                            }
                        }

                        // Satuan (Unit) Dropdown with Inline Edit & Delete
                        ExposedDropdownMenuBox(
                            expanded = unitDropdownExpanded,
                            onExpandedChange = { unitDropdownExpanded = !unitDropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedUnit,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Satuan Produk (Unit)") },
                                leadingIcon = { Icon(Icons.Default.Straighten, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitDropdownExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = unitDropdownExpanded,
                                onDismissRequest = { unitDropdownExpanded = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                units.forEach { u ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(u.name, color = Color(0xFF0F172A), modifier = Modifier.weight(1f))
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                    modifier = Modifier.clickable(
                                                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                                        indication = null,
                                                        onClick = { /* Consumed to prevent dropdown selection */ }
                                                    )
                                                ) {
                                                    IconButton(
                                                        onClick = {
                                                            unitToEdit = u
                                                            unitDropdownExpanded = false
                                                        },
                                                        modifier = Modifier.size(26.dp)
                                                    ) {
                                                        Icon(Icons.Default.Edit, contentDescription = "Edit Satuan", tint = Color(0xFF0284C7), modifier = Modifier.size(14.dp))
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            unitToDelete = u
                                                            unitDropdownExpanded = false
                                                        },
                                                        modifier = Modifier.size(26.dp)
                                                    ) {
                                                        Icon(Icons.Default.Delete, contentDescription = "Hapus Satuan", tint = Color(0xFFDC2626), modifier = Modifier.size(14.dp))
                                                    }
                                                }
                                            }
                                        },
                                        onClick = {
                                            selectedUnit = u.name
                                            unitDropdownExpanded = false
                                        },
                                        modifier = Modifier.background(Color.White)
                                    )
                                }
                                HorizontalDivider(color = Color(0xFFE2E8F0))
                                DropdownMenuItem(
                                    text = {
                                        Text("+ Tambah Satuan Baru...", fontWeight = FontWeight.Bold, color = Color(0xFF006C4C))
                                    },
                                    onClick = {
                                        unitDropdownExpanded = false
                                        showAddCustomUnitDialog = true
                                    },
                                    modifier = Modifier.background(Color.White)
                                )
                            }
                        }
                    }
                }

                // Section 3: Informasi Harga & Batas Stok
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("INFORMASI HARGA & STOK", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Harga Modal",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = buyPriceText,
                                    onValueChange = { buyPriceText = it },
                                    placeholder = { Text("Nominal", fontSize = 12.sp, color = Color(0xFF94A3B8)) },
                                    leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF006C4C)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Harga Jual *",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = sellPriceText,
                                    onValueChange = { sellPriceText = it },
                                    placeholder = { Text("Nominal", fontSize = 12.sp, color = Color(0xFF94A3B8)) },
                                    leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF006C4C)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = minStockText,
                            onValueChange = { minStockText = it },
                            label = { Text("Batas Peringatan Min. Stok") },
                            leadingIcon = { Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val randomDigits = (1000000..9999999).random()
                    val finalSku = if (sku.trim().matches(Regex("^SKU-\\d{7}$"))) {
                        sku.trim()
                    } else {
                        "SKU-$randomDigits"
                    }
                    val finalBarcode = Ean13Utils.toValidEan13(barcode)

                    val finalProduct = ProductEntity(
                        id = product?.id ?: 0L,
                        barcode = finalBarcode,
                        sku = finalSku,
                        name = name.ifBlank { "Produk Baru" },
                        categoryId = selectedCatId,
                        buyPrice = buyPriceText.toDoubleOrNull() ?: 0.0,
                        sellPrice = sellPriceText.toDoubleOrNull() ?: 0.0,
                        stock = currentStock,
                        minStock = minStockText.toIntOrNull() ?: 5,
                        unit = selectedUnit.ifBlank { "pcs" }
                    )
                    onSave(finalProduct)
                },
                enabled = name.isNotBlank() && barcodeError == null
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )

    if (showAddCustomUnitDialog) {
        var newUnitName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddCustomUnitDialog = false },
            containerColor = Color.White,
            title = { Text("Tambah Satuan Baru") },
            text = {
                OutlinedTextField(
                    value = newUnitName,
                    onValueChange = { newUnitName = it },
                    label = { Text("Nama Satuan") },
                    placeholder = { Text("Contoh: sachet, slop, pack, kg...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newUnitName.isNotBlank()) {
                            onAddNewUnit(newUnitName.trim())
                            selectedUnit = newUnitName.trim()
                            showAddCustomUnitDialog = false
                        }
                    },
                    enabled = newUnitName.isNotBlank()
                ) {
                    Text("Tambah")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomUnitDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    if (unitToEdit != null) {
        val u = unitToEdit!!
        var editedName by remember { mutableStateOf(u.name) }
        AlertDialog(
            onDismissRequest = { unitToEdit = null },
            containerColor = Color.White,
            title = { Text("Edit Nama Satuan") },
            text = {
                OutlinedTextField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    label = { Text("Nama Satuan") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editedName.isNotBlank()) {
                            onUpdateUnit(u.copy(name = editedName.trim()))
                            if (selectedUnit == u.name) selectedUnit = editedName.trim()
                            unitToEdit = null
                        }
                    },
                    enabled = editedName.isNotBlank()
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { unitToEdit = null }) {
                    Text("Batal")
                }
            }
        )
    }

    if (unitToDelete != null) {
        val u = unitToDelete!!
        AlertDialog(
            onDismissRequest = { unitToDelete = null },
            containerColor = Color.White,
            title = { Text("Hapus Satuan '${u.name}'?") },
            text = { Text("Apakah Anda yakin ingin menghapus satuan '${u.name}' dari master data?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteUnit(u)
                        unitToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { unitToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }

    if (showAddCategoryDialog) {
        var newCatName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            containerColor = Color.White,
            title = { Text("Tambah Kategori Baru") },
            text = {
                OutlinedTextField(
                    value = newCatName,
                    onValueChange = { newCatName = it },
                    label = { Text("Nama Kategori") },
                    placeholder = { Text("Contoh: Makanan, Minuman, Alat Tulis...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCatName.isNotBlank()) {
                            onAddCategory(newCatName.trim(), "#006C4C")
                            showAddCategoryDialog = false
                        }
                    },
                    enabled = newCatName.isNotBlank()
                ) {
                    Text("Tambah")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    if (categoryToEdit != null) {
        val cat = categoryToEdit!!
        var editedCatName by remember { mutableStateOf(cat.name) }
        AlertDialog(
            onDismissRequest = { categoryToEdit = null },
            containerColor = Color.White,
            title = { Text("Edit Kategori") },
            text = {
                OutlinedTextField(
                    value = editedCatName,
                    onValueChange = { editedCatName = it },
                    label = { Text("Nama Kategori") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editedCatName.isNotBlank()) {
                            onUpdateCategory(cat.copy(name = editedCatName.trim()))
                            categoryToEdit = null
                        }
                    },
                    enabled = editedCatName.isNotBlank()
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToEdit = null }) {
                    Text("Batal")
                }
            }
        )
    }

    if (categoryToDelete != null) {
        val cat = categoryToDelete!!
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            containerColor = Color.White,
            title = { Text("Hapus Kategori '${cat.name}'?") },
            text = { Text("Apakah Anda yakin ingin menghapus kategori '${cat.name}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCategory(cat)
                        categoryToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }
}
