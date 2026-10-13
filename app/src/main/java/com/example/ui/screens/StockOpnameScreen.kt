package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ProductEntity
import com.example.ui.KasirViewModel
import com.example.ui.components.PaginationControlBar
import com.example.ui.components.paginate

@Composable
fun StockOpnameScreen(viewModel: KasirViewModel) {
    val context = LocalContext.current
    val products by viewModel.products.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var productForOpname by remember { mutableStateOf<ProductEntity?>(null) }
    var opnamePage by remember { mutableIntStateOf(1) }
    var opnamePageSize by remember { mutableIntStateOf(10) }

    val filteredProducts = products.filter {
        searchQuery.isBlank() ||
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.barcode.contains(searchQuery, ignoreCase = true) ||
                it.sku.contains(searchQuery, ignoreCase = true)
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Screen Header Title & Subtitle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Stok Opname Fisik",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "Audit dan sesuaikan stok fisik rak/gudang dengan sistem",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Cari barang untuk opname...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 12.dp)
                .testTag("opname_search_input"),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        // Products List
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
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tidak Ada Produk Ditemukan",
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                }
            }
        } else {
            val totalPages = kotlin.math.max(1, kotlin.math.ceil(filteredProducts.size.toDouble() / opnamePageSize).toInt())
            val clampedPage = opnamePage.coerceIn(1, totalPages)
            val paginatedProducts = filteredProducts.paginate(clampedPage, opnamePageSize)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(paginatedProducts, key = { it.id }) { product ->
                    OpnameProductCard(
                        product = product,
                        onClick = { productForOpname = product }
                    )
                }

                item {
                    PaginationControlBar(
                        currentPage = clampedPage,
                        totalPages = totalPages,
                        totalItems = filteredProducts.size,
                        pageSize = opnamePageSize,
                        onPageChange = { opnamePage = it },
                        onPageSizeChange = {
                            opnamePageSize = it
                            opnamePage = 1
                        },
                        itemName = "produk",
                        modifier = Modifier.padding(top = 6.dp, bottom = 12.dp)
                    )
                }
            }
        }
    }
}

    if (productForOpname != null) {
        val prod = productForOpname!!
        StockOpnameModalDialog(
            product = prod,
            onDismiss = { productForOpname = null },
            onConfirm = { actualQty, notes ->
                viewModel.recordStockOpname(prod, actualQty, notes)
                productForOpname = null
                Toast.makeText(context, "Stok fisik ${prod.name} diperbarui menjadi $actualQty ${prod.unit}", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun OpnameProductCard(
    product: ProductEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "SKU: ${product.sku.ifBlank { "-" }}  •  EAN-13: ${product.barcode.ifBlank { "-" }}",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
                Text(
                    text = "Stok Sistem Saat Ini: ${product.stock} ${product.unit}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Button(
                onClick = onClick,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Hitung Fisik", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun StockOpnameModalDialog(
    product: ProductEntity,
    onDismiss: () -> Unit,
    onConfirm: (actualQty: Int, notes: String) -> Unit
) {
    var actualQtyText by remember { mutableStateOf(product.stock.toString()) }
    var notes by remember { mutableStateOf("") }

    val actualQty = actualQtyText.toIntOrNull() ?: product.stock
    val delta = actualQty - product.stock

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Column {
                Text("Stok Opname Fisik", fontWeight = FontWeight.Bold)
                Text(product.name, fontSize = 12.sp, color = Color.Gray)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Stok Sistem Saat Ini: ${product.stock} ${product.unit}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (delta != 0) {
                            Text(
                                text = "Selisih: ${if (delta > 0) "+$delta" else "$delta"} ${product.unit} (${if (delta > 0) "Kelebihan Fisik" else "Kekurangan Fisik"})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (delta > 0) Color(0xFF16A34A) else Color(0xFFDC2626)
                            )
                        } else {
                            Text(
                                text = "Stok fisik cocok dengan sistem",
                                fontSize = 11.sp,
                                color = Color(0xFF16A34A)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = actualQtyText,
                    onValueChange = { actualQtyText = it },
                    label = { Text("Jumlah Riil / Fisik di Rak (${product.unit})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan Audit Opname") },
                    placeholder = { Text("Alasan selisih jika ada...") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalQty = actualQtyText.toIntOrNull()
                    if (finalQty != null && finalQty >= 0) {
                        onConfirm(finalQty, notes)
                    }
                },
                enabled = actualQtyText.toIntOrNull() != null && (actualQtyText.toIntOrNull() ?: -1) >= 0
            ) {
                Text("Simpan Hasil Opname")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
