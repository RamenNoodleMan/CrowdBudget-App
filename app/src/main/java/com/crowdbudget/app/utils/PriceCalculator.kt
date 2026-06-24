package com.crowdbudget.app.utils

import com.crowdbudget.app.models.PriceSubmission

object PriceCalculator {

    fun calculateAveragePrice(submissions: List<PriceSubmission>): Double {
        if (submissions.isEmpty()) return 0.0
        val sum = submissions.sumOf { it.price }
        return sum / submissions.size
    }

    fun calculateMedianPrice(submissions: List<PriceSubmission>): Double {
        if (submissions.isEmpty()) return 0.0
        val sortedPrices = submissions.map { it.price }.sorted()
        val middle = sortedPrices.size / 2
        return if (sortedPrices.size % 2 == 0) {
            (sortedPrices[middle - 1] + sortedPrices[middle]) / 2
        } else {
            sortedPrices[middle]
        }
    }

    fun getPriceRange(submissions: List<PriceSubmission>): Pair<Double, Double> {
        if (submissions.isEmpty()) return Pair(0.0, 0.0)
        val prices = submissions.map { it.price }
        return Pair(prices.minOrNull() ?: 0.0, prices.maxOrNull() ?: 0.0)
    }

    fun formatZMW(price: Double): String {
        return String.format("ZMW %.2f", price)
    }

    fun isValidPrice(priceStr: String): Boolean {
        val pricePattern = Regex("^\\d+(\\.\\d{1,2})?$")
        return pricePattern.matches(priceStr)
    }

    fun isValidEmail(email: String): Boolean {
        if (email.isEmpty()) return false
        val emailPattern = Regex("[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+")
        return emailPattern.matches(email)
    }

    fun isValidPassword(password: String): Boolean {
        return password.length >= 6 && password.any { it.isDigit() }
    }

    fun isValidName(name: String): Boolean {
        return name.isNotEmpty() && name.length >= 2
    }

    fun isValidProductName(name: String): Boolean {
        return name.isNotEmpty() && name.length >= 2 && name.length <= 50
    }

    fun getPriceTrend(currentPrice: Double, previousPrice: Double): String {
        return when {
            previousPrice == 0.0 -> "➡️"  // No previous data
            currentPrice > previousPrice -> "📈"  // Price increased
            currentPrice < previousPrice -> "📉"  // Price decreased
            else -> "➡️"  // Price stable
        }
    }

    fun getTrendMessage(currentPrice: Double, previousPrice: Double): String {
        return when {
            previousPrice == 0.0 -> "New product"
            currentPrice > previousPrice -> "↑ ${String.format("%.2f", currentPrice - previousPrice)} ZMW"
            currentPrice < previousPrice -> "↓ ${String.format("%.2f", previousPrice - currentPrice)} ZMW"
            else -> "No change"
        }
    }
}