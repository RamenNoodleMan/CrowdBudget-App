package com.crowdbudget.app.ui

import android.graphics.Color
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.crowdbudget.app.R
import com.crowdbudget.app.databinding.ActivityPriceContributionBinding
import com.crowdbudget.app.models.PriceSubmission
import com.crowdbudget.app.models.Product
import com.crowdbudget.app.utils.PriceCalculator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Random

class PriceContributionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPriceContributionBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private val products = mutableListOf<Product>()
    private var selectedProductId: String = ""
    private var selectedProductName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPriceContributionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Setup toolbar
        binding.toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.primary_green))
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Price Contribution"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white)
        binding.toolbar.setTitleTextColor(Color.WHITE)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        setupStoreSpinner()
        loadProducts()
        setupPriceValidation()

        binding.btnSubmit.setOnClickListener {
            submitPrice()
        }
    }



    private fun loadProducts() {
        binding.progressBar.visibility = android.view.View.VISIBLE

        db.collection("products")
            .orderBy("name")
            .get()
            .addOnSuccessListener { documents ->
                binding.progressBar.visibility = android.view.View.GONE

                products.clear()
                val productNames = mutableListOf<String>()

                documents.forEach { doc ->
                    val product = doc.toObject(Product::class.java)
                    product.id = doc.id
                    products.add(product)
                    productNames.add(product.name)
                }

                if (productNames.isNotEmpty()) {
                    setupProductSpinner(productNames)
                    selectedProductId = products[0].id
                    selectedProductName = products[0].name
                } else {
                    Toast.makeText(this, "No products found. Add products first!", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = android.view.View.GONE
                Toast.makeText(this, "Error loading products: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun setupProductSpinner(productNames: List<String>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, productNames)
        binding.spinnerProduct.adapter = adapter

        binding.spinnerProduct.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                if (position < products.size) {
                    selectedProductId = products[position].id
                    selectedProductName = products[position].name
                }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    private fun setupStoreSpinner() {
        val stores = listOf("Shoprite", "Pick n Pay", "Game", "Woolworths", "Local Market", "Other")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, stores)
        binding.spinnerStore.adapter = adapter
    }

    private fun setupPriceValidation() {
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

    private fun showConfetti() {
        try {
            val colors = intArrayOf(
                0xFFFFFF00.toInt(), // Yellow
                0xFF00FF00.toInt(), // Green
                0xFFF9A825.toInt(), // Gold
                0xFF2E7D32.toInt(), // Dark Green
                0xFFFF0000.toInt(), // Red
                0xFF0000FF.toInt()  // Blue
            )

            val random = Random()


            val rootView = binding.root as android.view.ViewGroup


            for (i in 0..30) {
                val size = (10 + random.nextInt(15))
                val view = android.view.View(this).apply {
                    setBackgroundColor(colors[random.nextInt(colors.size)])
                    layoutParams = android.view.ViewGroup.LayoutParams(size, size)
                    translationX = random.nextInt(rootView.width).toFloat()
                    translationY = (-50 - random.nextInt(150)).toFloat()
                    rotation = random.nextInt(360).toFloat()
                }

                rootView.addView(view)

                view.animate()
                    .translationYBy((rootView.height + 200).toFloat())
                    .translationXBy((random.nextInt(300) - 150).toFloat())
                    .rotationBy(720f)
                    .setDuration(1500)
                    .withEndAction {
                        try {
                            rootView.removeView(view)
                        } catch (e: Exception) { }
                    }
                    .start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback - just show toast
            Toast.makeText(this, "🎉 Price Submitted! 🎉", Toast.LENGTH_SHORT).show()
        }
    }

    private fun submitPrice() {
        val priceStr = binding.editPrice.text.toString().trim()

        if (priceStr.isEmpty()) {
            binding.editPrice.error = getString(R.string.error_price_empty)
            return
        }

        if (!PriceCalculator.isValidPrice(priceStr)) {
            binding.editPrice.error = getString(R.string.error_price_invalid)
            return
        }

        val price = priceStr.toDoubleOrNull()
        if (price == null || price <= 0) {
            binding.editPrice.error = getString(R.string.error_price_negative)
            return
        }

        if (selectedProductId.isEmpty()) {
            Toast.makeText(this, "Please select a product", Toast.LENGTH_SHORT).show()
            return
        }

        val store = binding.spinnerStore.selectedItem.toString()
        val userId = auth.currentUser?.uid ?: "anonymous"

        binding.progressBar.visibility = android.view.View.VISIBLE
        binding.btnSubmit.isEnabled = false


        db.collection("products").document(selectedProductId).get()
            .addOnSuccessListener { productDoc ->
                val currentProduct = productDoc.toObject(Product::class.java)
                val oldPrice = currentProduct?.estimatedPrice ?: 0.0

                android.util.Log.d("PriceTrend", "Old price (will become previousPrice): $oldPrice")

                val submission = PriceSubmission(
                    productId = selectedProductId,
                    productName = selectedProductName,
                    price = price,
                    userId = userId,
                    storeName = store,
                    timestamp = com.google.firebase.Timestamp.now()
                )


                db.collection("priceSubmissions").add(submission)
                    .addOnSuccessListener {
                        // Get all submissions including the new one
                        db.collection("priceSubmissions")
                            .whereEqualTo("productId", selectedProductId)
                            .get()
                            .addOnSuccessListener { submissions ->
                                val prices = submissions.documents.mapNotNull { it.toObject(PriceSubmission::class.java) }
                                val averagePrice = if (prices.isNotEmpty()) {
                                    prices.sumOf { it.price } / prices.size
                                } else {
                                    price
                                }

                                android.util.Log.d("PriceTrend", "New average: $averagePrice")

                                val updates = mapOf(
                                    "estimatedPrice" to averagePrice,
                                    "previousPrice" to oldPrice,
                                    "priceCount" to prices.size,
                                    "lastUpdated" to com.google.firebase.Timestamp.now()
                                )

                                db.collection("products").document(selectedProductId)
                                    .update(updates)
                                    .addOnSuccessListener {
                                        binding.progressBar.visibility = android.view.View.GONE
                                        binding.btnSubmit.isEnabled = true
                                        binding.editPrice.text?.clear()
                                        Toast.makeText(this, "Price submitted! New average: ${PriceCalculator.formatZMW(averagePrice)}", Toast.LENGTH_LONG).show()
                                        showConfetti()
                                        finish()
                                    }
                                    .addOnFailureListener { e ->
                                        android.util.Log.e("PriceTrend", "Update failed: ${e.message}")
                                        binding.progressBar.visibility = android.view.View.GONE
                                        binding.btnSubmit.isEnabled = true
                                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                            }
                    }
                    .addOnFailureListener { e ->
                        binding.progressBar.visibility = android.view.View.GONE
                        binding.btnSubmit.isEnabled = true
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = android.view.View.GONE
                binding.btnSubmit.isEnabled = true
                Toast.makeText(this, "Error getting product: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}