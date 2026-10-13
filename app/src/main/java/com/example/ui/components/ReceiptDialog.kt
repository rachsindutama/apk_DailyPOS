package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.dao.TransactionWithItems
import com.example.data.local.entity.StoreSettingEntity
import com.example.util.CurrencyFormatter
import com.example.util.ReceiptPrinterHelper

@Composable
fun ReceiptDialog(
    transactionWithItems: TransactionWithItems,
    storeSettings: StoreSettingEntity,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val t = transactionWithItems.transaction
    val items = transactionWithItems.items

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Success Indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = "Transaksi Berhasil",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF16A34A)
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_receipt_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Thermal Paper Container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFCFDFD))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Store Info
                        Text(
                            text = storeSettings.storeName.uppercase(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            color = Color.Black
                        )
                        if (storeSettings.slogan.isNotBlank()) {
                            Text(
                                text = storeSettings.slogan,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center,
                                color = Color.Black
                            )
                        }
                        Text(
                            text = storeSettings.address,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            color = Color.Black
                        )
                        Text(
                            text = "Telp/WA: ${storeSettings.phoneNumber}",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            color = Color.Black
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "--------------------------------",
                            fontFamily = FontFamily.Monospace,
                            color = Color.Black
                        )

                        // Meta Info
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "No: ${t.invoiceNumber}", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                            Text(text = CurrencyFormatter.formatDate(t.timestamp, "dd/MM/yy HH:mm"), fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Kasir: ${t.cashierName}", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                            Text(text = "Plg: ${t.customerName}", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                        }

                        Text(
                            text = "--------------------------------",
                            fontFamily = FontFamily.Monospace,
                            color = Color.Black
                        )

                        // Items
                        for (item in items) {
                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                Text(
                                    text = item.productName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.Black
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${item.quantity} x ${CurrencyFormatter.formatRupiah(item.unitPrice)}",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.Black
                                    )
                                    Text(
                                        text = CurrencyFormatter.formatRupiah(item.subtotal),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.Black
                                    )
                                }
                                if (item.discountAmount > 0) {
                                    val promoDesc = if (item.promoNote.isNotBlank()) {
                                        val formattedNote = item.promoNote.trim()
                                        " ($formattedNote)"
                                    } else ""
                                    Text(
                                        text = "  Diskon -${CurrencyFormatter.formatRupiah(item.discountAmount)}$promoDesc",
                                        fontSize = 9.sp,
                                        color = Color.Black,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        Text(
                            text = "--------------------------------",
                            fontFamily = FontFamily.Monospace,
                            color = Color.Black
                        )

                        // Totals
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Subtotal (Asli)", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                            Text(text = CurrencyFormatter.formatRupiah(t.subtotal), fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                        }
                        if (t.discountAmount > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "Total Diskon", fontSize = 11.sp, color = Color.Black, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
                                Text(text = "-${CurrencyFormatter.formatRupiah(t.discountAmount)}", fontSize = 11.sp, color = Color.Black, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
                            }
                            if (t.discountNote.isNotBlank()) {
                                val cleanNote = if (t.discountNote.startsWith("(") && t.discountNote.endsWith(")")) {
                                    t.discountNote.substring(1, t.discountNote.length - 1)
                                } else {
                                    t.discountNote
                                }.removePrefix("Promo: ").trim()
                                val promoList = cleanNote
                                    .split(Regex("[,|]"))
                                    .map { it.trim() }
                                    .filter { it.isNotBlank() }

                                if (promoList.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Diskon Promo",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Start
                                    )
                                    promoList.forEach { promoItem ->
                                        val rawClean = promoItem.trim()
                                        val cleanPromoName = rawClean.trim()
                                        Text(
                                            text = "* $cleanPromoName",
                                            fontSize = 9.5.sp,
                                            color = Color.Black,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 4.dp),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Start
                                        )
                                    }
                                }
                            }
                        }
                        if (t.isTaxApplied && t.taxAmount > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "PPN (${t.taxPercent}%)", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                                Text(text = CurrencyFormatter.formatRupiah(t.taxAmount), fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "TOTAL", fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                            Text(text = CurrencyFormatter.formatRupiah(t.grandTotal), fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Metode", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                            Text(text = t.paymentMethod, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                        }

                        if (t.paymentMethod == "TUNAI") {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "Bayar Tunai", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                                Text(text = CurrencyFormatter.formatRupiah(t.cashReceived), fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "Kembalian", fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                                Text(text = CurrencyFormatter.formatRupiah(t.changeAmount), fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                            }
                        }

                        Text(
                            text = "================================",
                            fontFamily = FontFamily.Monospace,
                            color = Color.Black
                        )

                        // Footer
                        Text(
                            text = storeSettings.receiptFooter,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            color = Color.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val html = ReceiptPrinterHelper.generateReceiptHtml(transactionWithItems, storeSettings)
                            ReceiptPrinterHelper.printReceipt(context, html, "Struk_${t.invoiceNumber}")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("print_receipt_action"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = "Cetak")
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Cetak Struk")
                    }

                    OutlinedButton(
                        onClick = {
                            val textReceipt = ReceiptPrinterHelper.generateReceiptText(transactionWithItems, storeSettings)
                            ReceiptPrinterHelper.shareReceipt(context, textReceipt)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_receipt_action")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Bagikan")
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Bagikan")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("done_receipt_action"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Transaksi Selesai")
                }
            }
        }
    }
}
