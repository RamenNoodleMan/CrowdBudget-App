package com.crowdbudget.app.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

data class Product(
    @get:Exclude var id: String = "",
    val name: String = "",
    val category: String = "General",
    val estimatedPrice: Double = 0.0,
    val previousPrice: Double = 0.0,
    val priceCount: Int = 0,
    val lastUpdated: Timestamp = Timestamp.now()
)