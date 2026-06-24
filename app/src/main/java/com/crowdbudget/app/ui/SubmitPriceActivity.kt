package com.crowdbudget.app.ui

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.crowdbudget.app.databinding.ActivitySubmitPriceBinding
import com.crowdbudget.app.models.PriceSubmission
import com.crowdbudget.app.utils.PriceCalculator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.crowdbudget.app.R
import com.crowdbudget.app.models.Product
import android.util.Log

class SubmitPriceActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySubmitPriceBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var productId: String = ""
    private var productName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubmitPriceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        productId = intent.getStringExtra("product_id") ?: ""
        productName = intent.getStringExtra("product_name") ?: ""

        setupStoreSpinner()
        setupClickListeners()

        binding.tvProductName.text = productName
        if (productId.isEmpty()) {
            Toast.makeText(this, "Please select a product first", Toast.LENGTH_LONG).show()
        }

        setupPriceInputValidation()
    }

    private fun setupPriceInputValidation() {
        binding.editPrice.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val priceText = binding.editPrice.text.toString()
                if (priceText.isNotEmpty()) {
                    if (!PriceCalculator.isValidPrice(priceText)) {
                        binding.editPrice.error = getString(R.string.error_price_invalid)
                    } else {
                        val price = priceText.toDoubleOrNull()
                        if (price != null && price <= 0) {
                            binding.editPrice.error = getString(R.string.error_price_negative)
                        } else if (price != null && price > 10000) {
                            binding.editPrice.error = getString(R.string.error_price_high)
                        } else {
                            binding.editPrice.error = null
                        }
                    }
                }
            }
        }
    }

    private fun setupStoreSpinner() {
        val stores = listOf("Shoprite", "Pick n Pay", "Game", "Woolworths", "Local Market", "Other")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, stores)
        binding.spinnerStore.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.btnCancel.setOnClickListener {
            finish()
        }

        binding.btnSubmit.setOnClickListener {
            if (productId.isEmpty()) {
                Toast.makeText(this, "Please select a product first", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val priceStr = binding.editPrice.text.toString().trim()

            if (priceStr.isEmpty()) {
                binding.editPrice.error = getString(R.string.error_price_empty)
                binding.editPrice.requestFocus()
                return@setOnClickListener
            }

            if (!PriceCalculator.isValidPrice(priceStr)) {
                binding.editPrice.error = getString(R.string.error_price_invalid)
                binding.editPrice.requestFocus()
                return@setOnClickListener
            }

            val price = priceStr.toDoubleOrNull()

            if (price == null) {
                binding.editPrice.error = getString(R.string.error_price_invalid)
                binding.editPrice.requestFocus()
                return@setOnClickListener
            }

            if (price <= 0) {
                binding.editPrice.error = getString(R.string.error_price_negative)
                binding.editPrice.requestFocus()
                return@setOnClickListener
            }

            if (price > 10000) {
                binding.editPrice.error = getString(R.string.error_price_high)
                binding.editPrice.requestFocus()
                return@setOnClickListener
            }

            val store = binding.spinnerStore.selectedItem.toString()
            val userId = auth.currentUser?.uid ?: "anonymous"

            submitPrice(productId, productName, price, store, userId)
        }
    }

    private fun submitPrice(productId: String, productName: String, price: Double, store: String, userId: String) {
        binding.progressBar.visibility = android.view.View.VISIBLE
        binding.btnSubmit.isEnabled = false

        val submission = PriceSubmission(
            productId = productId,
            productName = productName,
            price = price,
            userId = userId,
            storeName = store,
            timestamp = com.google.firebase.Timestamp.now()
        )

        db.collection("priceSubmissions").add(submission)
            .addOnSuccessListener {
                updateProductAverage(productId)
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = android.view.View.GONE
                binding.btnSubmit.isEnabled = true
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun updateProductAverage(productId: String) {
        // FIRST, get the current product to know the OLD price
        db.collection("products").document(productId).get()
            .addOnSuccessListener { productDoc ->
                val currentProduct = productDoc.toObject(Product::class.java)
                val oldPrice = currentProduct?.estimatedPrice ?: 0.0

                android.util.Log.d("PriceTrend", "Old price (will become previousPrice): $oldPrice")

                // THEN, get all submissions and calculate new average
                db.collection("priceSubmissions")
                    .whereEqualTo("productId", productId)
                    .get()
                    .addOnSuccessListener { submissions ->
                        val prices = submissions.documents.mapNotNull { it.toObject(PriceSubmission::class.java) }
                        val averagePrice = if (prices.isNotEmpty()) {
                            prices.sumOf { it.price } / prices.size
                        } else {
                            0.0
                        }

                        android.util.Log.d("PriceTrend", "New average (estimatedPrice): $averagePrice")

                        val updates = mapOf(
                            "estimatedPrice" to averagePrice,
                            "previousPrice" to oldPrice,  // ← This is the OLD price
                            "priceCount" to prices.size,
                            "lastUpdated" to com.google.firebase.Timestamp.now()
                        )

                        db.collection("products").document(productId)
                            .update(updates)
                            .addOnSuccessListener {
                                android.util.Log.d("PriceTrend", "Update successful!")
                                binding.progressBar.visibility = android.view.View.GONE
                                binding.btnSubmit.isEnabled = true
                                binding.editPrice.text?.clear()
                                Toast.makeText(this, "Price submitted! New average: ${PriceCalculator.formatZMW(averagePrice)}", Toast.LENGTH_LONG).show()
                                finish()
                            }
                            .addOnFailureListener { e ->
                                android.util.Log.e("PriceTrend", "Update failed: ${e.message}")
                            }
                    }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("PriceTrend", "Failed to get product: ${e.message}")
                binding.progressBar.visibility = android.view.View.GONE
                binding.btnSubmit.isEnabled = true
            }
    }
}