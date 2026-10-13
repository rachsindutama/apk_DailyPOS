package com.example.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CurrencyFormatter {
    private val rupiahFormat: DecimalFormat by lazy {
        val symbols = DecimalFormatSymbols(Locale("id", "ID")).apply {
            currencySymbol = "Rp "
            groupingSeparator = '.'
            monetaryDecimalSeparator = ','
        }
        DecimalFormat("Rp #,##0", symbols)
    }

    private val numberFormat: DecimalFormat by lazy {
        val symbols = DecimalFormatSymbols(Locale("id", "ID")).apply {
            groupingSeparator = '.'
            monetaryDecimalSeparator = ','
        }
        DecimalFormat("#,##0", symbols)
    }

    fun formatRupiah(amount: Double): String {
        return rupiahFormat.format(amount)
    }

    fun formatNumber(number: Number): String {
        return numberFormat.format(number)
    }

    fun formatDate(timestamp: Long, pattern: String = "dd MMM yyyy, HH:mm"): String {
        val sdf = SimpleDateFormat(pattern, Locale("id", "ID"))
        return sdf.format(Date(timestamp))
    }

    fun formatDateShort(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID"))
        return sdf.format(Date(timestamp))
    }

    fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale("id", "ID"))
        return sdf.format(Date(timestamp))
    }
}
