package com.crowdbudget.app.models

import com.google.firebase.Timestamp

data class PriceSubmission(
    val id: String = "",
    val productId: String = "",
    val productName: String = "",
    val price: Double = 0.0,
    val userId: String = "",
    val storeName: String = "",
    val timestamp: Timestamp = Timestamp.now()
)