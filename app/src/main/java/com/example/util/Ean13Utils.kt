package com.example.util

import kotlin.random.Random

/**
 * Utility helper to handle EAN-13 barcode validation, calculation of checksum digits,
 * normalization, and automatic generation for products.
 */
object Ean13Utils {

    /**
     * Checks whether the given string is a strictly valid 13-digit EAN-13 barcode
     * with a matching check digit.
     */
    fun isValidEan13(code: String): Boolean {
        val trimmed = code.trim()
        if (trimmed.length != 13 || !trimmed.all { it.isDigit() }) {
            return false
        }
        val calculatedCheckDigit = calculateCheckDigit(trimmed.substring(0, 12))
        return (trimmed.last() - '0') == calculatedCheckDigit
    }

    /**
     * Calculates the standard EAN-13 modulo 10 check digit for a 12-digit numeric prefix.
     * Weights: alternate 1 and 3 (index 0 is weight 1, index 1 is weight 3, ...).
     */
    fun calculateCheckDigit(twelveDigits: String): Int {
        require(twelveDigits.length == 12 && twelveDigits.all { it.isDigit() }) {
            "Input must be exactly 12 digits, got: '$twelveDigits'"
        }
        var sum = 0
        for (i in 0 until 12) {
            val digit = twelveDigits[i] - '0'
            val weight = if (i % 2 == 0) 1 else 3
            sum += digit * weight
        }
        val remainder = sum % 10
        return if (remainder == 0) 0 else (10 - remainder)
    }

    /**
     * Normalizes any input code into a valid 13-digit EAN-13 barcode:
     * - If already a valid 13-digit EAN-13: returns it.
     * - If it's a 13-digit number but has an invalid check digit: recalculates the 13th digit.
     * - If it's between 1 and 12 numeric digits: prepends/pads with country/company prefix (default 899 for Indonesia)
     *   or zero-pads up to 12 digits, then computes the 13th check digit.
     * - If blank or contains non-digits: generates a new valid EAN-13 barcode.
     */
    fun toValidEan13(rawCode: String?, prefix: String = "899"): String {
        val clean = rawCode?.filter { it.isDigit() } ?: ""
        
        return when {
            clean.length == 13 && isValidEan13(clean) -> {
                clean
            }
            clean.length == 13 -> {
                // Recompute valid check digit for the 12-digit prefix
                val prefix12 = clean.substring(0, 12)
                prefix12 + calculateCheckDigit(prefix12)
            }
            clean.length == 12 -> {
                clean + calculateCheckDigit(clean)
            }
            clean.isNotEmpty() && clean.length < 12 -> {
                // Format with Indonesian retail GS1 prefix "899" if not present
                val body = if (clean.startsWith(prefix)) {
                    clean.padEnd(12, '0').take(12)
                } else {
                    (prefix + clean).padEnd(12, '0').take(12)
                }
                body + calculateCheckDigit(body)
            }
            clean.length > 13 -> {
                // Take first 12 digits and append check digit
                val first12 = clean.take(12)
                first12 + calculateCheckDigit(first12)
            }
            else -> {
                generateEan13(prefix)
            }
        }
    }

    /**
     * Automatically generates a new, unique, syntactically valid EAN-13 barcode.
     * By default uses GS1 Indonesia prefix "899" followed by 9 random digits and the 13th check digit.
     */
    fun generateEan13(prefix: String = "899"): String {
        val safePrefix = prefix.filter { it.isDigit() }.take(3).ifEmpty { "899" }
        val remainingDigitsCount = 12 - safePrefix.length
        val randomPart = buildString {
            repeat(remainingDigitsCount) {
                append(Random.nextInt(0, 10))
            }
        }
        val first12 = safePrefix + randomPart
        val checkDigit = calculateCheckDigit(first12)
        return "$first12$checkDigit"
    }

    /**
     * Validates manual input for immediate UI feedback.
     * Returns null if valid EAN-13, or an error message if invalid.
     */
    fun getValidationError(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null // allowed if auto-generated upon blank
        if (!trimmed.all { it.isDigit() }) {
            return "Barcode EAN-13 hanya boleh terdiri dari angka"
        }
        if (trimmed.length != 13) {
            return "Barcode EAN-13 harus tepat 13 digit (saat ini ${trimmed.length} digit)"
        }
        val expectedCheckDigit = calculateCheckDigit(trimmed.substring(0, 12))
        val actualCheckDigit = trimmed.last() - '0'
        if (expectedCheckDigit != actualCheckDigit) {
            return "Digit cek EAN-13 tidak valid (harus diakhiri digit $expectedCheckDigit)"
        }
        return null
    }
}
