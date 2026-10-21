package com.teasupplier.app.utils

/**
 * Formats a Double as Indian currency (₹).
 * Uses Indian number system: 1,00,000 instead of 100,000.
 * Always shows 2 decimal places.
 * 
 * Examples:
 *   12345.0    → "₹ 12,345.00"
 *   100000.5   → "₹ 1,00,000.50"
 *   0.0        → "₹ 0.00"
 *   -5000.0    → "-₹ 5,000.00"
 */
fun Double.formatCurrency(): String {
    val isNegative = this < 0
    val absValue = kotlin.math.abs(this)
    
    // Split into integer and decimal parts
    val integerPart = absValue.toLong()
    val decimalPart = String.format("%02d", ((absValue - integerPart) * 100).toLong())
    
    // Format integer part with Indian comma grouping
    val intStr = integerPart.toString()
    val formatted = if (intStr.length <= 3) {
        intStr
    } else {
        val lastThree = intStr.takeLast(3)
        val remaining = intStr.dropLast(3)
        val grouped = remaining.reversed().chunked(2).joinToString(",").reversed()
        "$grouped,$lastThree"
    }
    
    val prefix = if (isNegative) "-₹ " else "₹ "
    return "$prefix$formatted.$decimalPart"
}

/**
 * Formats a Double as Indian currency without decimal places.
 * Useful for integer-only displays.
 */
fun Double.formatCurrencyShort(): String {
    val isNegative = this < 0
    val absValue = kotlin.math.abs(this).toLong()
    
    val intStr = absValue.toString()
    val formatted = if (intStr.length <= 3) {
        intStr
    } else {
        val lastThree = intStr.takeLast(3)
        val remaining = intStr.dropLast(3)
        val grouped = remaining.reversed().chunked(2).joinToString(",").reversed()
        "$grouped,$lastThree"
    }
    
    val prefix = if (isNegative) "-₹ " else "₹ "
    return "$prefix$formatted"
}
