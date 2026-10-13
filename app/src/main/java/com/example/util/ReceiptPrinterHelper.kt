package com.example.util

import android.content.Context
import android.content.Intent
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.data.local.dao.TransactionWithItems
import com.example.data.local.entity.StoreSettingEntity

object ReceiptPrinterHelper {

    fun generateReceiptHtml(
        transactionWithItems: TransactionWithItems,
        storeSettings: StoreSettingEntity
    ): String {
        val t = transactionWithItems.transaction
        val items = transactionWithItems.items
        val paperWidth = if (storeSettings.printerPaperSize == "80mm") "80mm" else "58mm"

        val itemsHtml = StringBuilder()
        for (item in items) {
            val discText = if (item.discountAmount > 0) {
                val formattedNote = item.promoNote.trim()
                val pNote = if (formattedNote.isNotBlank()) " ($formattedNote)" else ""
                "<div style='font-size:10px; color:#000;'>Diskon: -${CurrencyFormatter.formatRupiah(item.discountAmount)}$pNote</div>"
            } else ""

            itemsHtml.append("""
                <tr>
                    <td colspan="2" style="font-weight:bold; padding-top:4px;">${item.productName}</td>
                </tr>
                <tr style="border-bottom: 1px dashed #ccc;">
                    <td style="font-size:11px; padding-bottom:4px;">${item.quantity} x ${CurrencyFormatter.formatRupiah(item.unitPrice)} $discText</td>
                    <td style="text-align:right; font-size:11px; padding-bottom:4px;">${CurrencyFormatter.formatRupiah(item.subtotal)}</td>
                </tr>
            """.trimIndent())
        }

        val taxHtml = if (t.isTaxApplied && t.taxAmount > 0) {
            """
            <tr>
                <td style="font-size:11px;">PPN (${t.taxPercent}%):</td>
                <td style="text-align:right; font-size:11px;">${CurrencyFormatter.formatRupiah(t.taxAmount)}</td>
            </tr>
            """.trimIndent()
        } else ""

        val discountHtml = if (t.discountAmount > 0) {
            val cleanNote = if (t.discountNote.startsWith("(") && t.discountNote.endsWith(")")) {
                t.discountNote.substring(1, t.discountNote.length - 1)
            } else {
                t.discountNote
            }.removePrefix("Promo: ").trim()
            val promoList = cleanNote
                .split(Regex("[,|]"))
                .map { it.trim() }
                .filter { it.isNotBlank() }

            val promoListHtml = if (promoList.isNotEmpty()) {
                val listItems = promoList.joinToString("") { p ->
                    val rawDisp = p.trim()
                    val disp = rawDisp.trim()
                    "<div style='font-size:10px; color:#000; padding-left:6px;'>* $disp</div>"
                }
                """
                <tr>
                    <td colspan="2" style="font-size:10px; color:#000; font-weight:bold; padding-top:2px;">Diskon Promo</td>
                </tr>
                <tr>
                    <td colspan="2">$listItems</td>
                </tr>
                """.trimIndent()
            } else ""

            """
            <tr>
                <td style="font-size:11px; color:#000;">Total Diskon:</td>
                <td style="text-align:right; font-size:11px; color:#000;">-${CurrencyFormatter.formatRupiah(t.discountAmount)}</td>
            </tr>
            $promoListHtml
            """.trimIndent()
        } else ""

        val paymentDetailsHtml = if (t.paymentMethod == "TUNAI") {
            """
            <tr>
                <td style="font-size:11px;">Bayar Tunai:</td>
                <td style="text-align:right; font-size:11px;">${CurrencyFormatter.formatRupiah(t.cashReceived)}</td>
            </tr>
            <tr>
                <td style="font-size:11px; font-weight:bold;">Kembalian:</td>
                <td style="text-align:right; font-size:11px; font-weight:bold;">${CurrencyFormatter.formatRupiah(t.changeAmount)}</td>
            </tr>
            """.trimIndent()
        } else {
            """
            <tr>
                <td style="font-size:11px;">Metode:</td>
                <td style="text-align:right; font-size:11px; font-weight:bold;">${t.paymentMethod} (LUNAS)</td>
            </tr>
            """.trimIndent()
        }

        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <style>
                body {
                    font-family: 'Courier New', monospace;
                    width: $paperWidth;
                    margin: 0 auto;
                    padding: 8px;
                    color: #000;
                    font-size: 12px;
                }
                .text-center { text-align: center; }
                .store-title { font-size: 15px; font-weight: bold; margin-bottom: 2px; }
                .store-sub { font-size: 11px; margin-bottom: 2px; }
                .divider { border-top: 1px dashed #000; margin: 6px 0; }
                .double-divider { border-top: 2px dashed #000; margin: 6px 0; }
                table { width: 100%; border-collapse: collapse; }
                .grand-total { font-size: 14px; font-weight: bold; }
                .footer { font-size: 10px; margin-top: 8px; line-height: 1.3; }
            </style>
        </head>
        <body>
            <div class="text-center">
                <div class="store-title">${storeSettings.storeName}</div>
                <div class="store-sub">${storeSettings.slogan}</div>
                <div class="store-sub">${storeSettings.address}</div>
                <div class="store-sub">Telp/WA: ${storeSettings.phoneNumber}</div>
            </div>
            
            <div class="divider"></div>
            
            <table style="font-size: 11px;">
                <tr>
                    <td>No: ${t.invoiceNumber}</td>
                    <td style="text-align:right;">${CurrencyFormatter.formatDate(t.timestamp, "dd/MM/yy HH:mm")}</td>
                </tr>
                <tr>
                    <td>Kasir: ${t.cashierName}</td>
                    <td style="text-align:right;">Plg: ${t.customerName}</td>
                </tr>
            </table>
            
            <div class="divider"></div>
            
            <table>
                $itemsHtml
            </table>
            
            <div class="divider"></div>
            
            <table>
                <tr>
                    <td style="font-size:11px;">Subtotal:</td>
                    <td style="text-align:right; font-size:11px;">${CurrencyFormatter.formatRupiah(t.subtotal)}</td>
                </tr>
                $discountHtml
                $taxHtml
                <tr class="grand-total">
                    <td style="padding: 4px 0;">TOTAL:</td>
                    <td style="text-align:right; padding: 4px 0;">${CurrencyFormatter.formatRupiah(t.grandTotal)}</td>
                </tr>
                $paymentDetailsHtml
            </table>
            
            <div class="double-divider"></div>
            
            <div class="text-center footer">
                <div>${storeSettings.receiptFooter.replace("\n", "<br>")}</div>
                <div style="margin-top: 4px;">*** STRUK RESMI KASIR ***</div>
            </div>
        </body>
        </html>
        """.trimIndent()
    }

    fun printReceipt(context: Context, htmlContent: String, jobName: String = "Struk_Belanja") {
        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                val printAdapter = webView.createPrintDocumentAdapter(jobName)
                val printAttributes = PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A6)
                    .setResolution(PrintAttributes.Resolution("thermal", "Thermal", 203, 203))
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                    .build()
                printManager?.print(jobName, printAdapter, printAttributes)
            }
        }
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
    }

    fun generateReceiptText(
        transactionWithItems: TransactionWithItems,
        storeSettings: StoreSettingEntity
    ): String {
        val t = transactionWithItems.transaction
        val items = transactionWithItems.items
        val sb = StringBuilder()

        sb.appendLine("================================")
        sb.appendLine(storeSettings.storeName.uppercase())
        sb.appendLine(storeSettings.slogan)
        sb.appendLine(storeSettings.address)
        sb.appendLine("Telp/WA: ${storeSettings.phoneNumber}")
        sb.appendLine("--------------------------------")
        sb.appendLine("No. Struk : ${t.invoiceNumber}")
        sb.appendLine("Tanggal   : ${CurrencyFormatter.formatDate(t.timestamp)}")
        sb.appendLine("Kasir     : ${t.cashierName}")
        sb.appendLine("Pelanggan : ${t.customerName}")
        sb.appendLine("--------------------------------")

        for (item in items) {
            sb.appendLine(item.productName)
            val subFormatted = CurrencyFormatter.formatRupiah(item.subtotal)
            val qtyPrice = "${item.quantity} x ${CurrencyFormatter.formatRupiah(item.unitPrice)}"
            sb.appendLine("$qtyPrice".padEnd(20) + subFormatted.padStart(12))
            if (item.discountAmount > 0) {
                sb.appendLine(" (Hemat: -${CurrencyFormatter.formatRupiah(item.discountAmount)})")
            }
        }

        sb.appendLine("--------------------------------")
        sb.appendLine("Subtotal      : ${CurrencyFormatter.formatRupiah(t.subtotal)}")
        if (t.discountAmount > 0) {
            sb.appendLine("Diskon        : -${CurrencyFormatter.formatRupiah(t.discountAmount)}")
        }
        if (t.isTaxApplied && t.taxAmount > 0) {
            sb.appendLine("PPN (${t.taxPercent}%)    : ${CurrencyFormatter.formatRupiah(t.taxAmount)}")
        }
        sb.appendLine("TOTAL         : ${CurrencyFormatter.formatRupiah(t.grandTotal)}")
        sb.appendLine("Metode Bayar  : ${t.paymentMethod}")
        if (t.paymentMethod == "TUNAI") {
            sb.appendLine("Diterima      : ${CurrencyFormatter.formatRupiah(t.cashReceived)}")
            sb.appendLine("Kembalian     : ${CurrencyFormatter.formatRupiah(t.changeAmount)}")
        }
        sb.appendLine("================================")
        sb.appendLine(storeSettings.receiptFooter)
        sb.appendLine("================================")

        return sb.toString()
    }

    fun shareReceipt(context: Context, textReceipt: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Struk Pembelian")
            putExtra(Intent.EXTRA_TEXT, textReceipt)
        }
        context.startActivity(Intent.createChooser(intent, "Bagikan Struk Belanja"))
    }
}
