package com.crowdbudget.app.models

import com.google.firebase.Timestamp

data class ShoppingListItem(
    val productId: String,
    val productName: String,
    val quantity: Int = 1,
    val estimatedUnitPrice: Double = 0.0
) {
    val totalPrice: Double
        get() = estimatedUnitPrice * quantity
}

data class ShoppingList(
    val id: String = "",
    val userId: String = "",
    val name: String = "My Shopping List",
    val items: MutableMap<String, ShoppingListItem> = mutableMapOf(),
    val totalEstimate: Double = 0.0,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)