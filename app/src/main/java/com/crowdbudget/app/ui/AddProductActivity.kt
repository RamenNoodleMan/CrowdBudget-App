package com.crowdbudget.app.ui

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.crowdbudget.app.R
import com.crowdbudget.app.databinding.ActivityAddProductBinding
import com.crowdbudget.app.models.Product
import com.crowdbudget.app.utils.PriceCalculator
import com.google.firebase.firestore.FirebaseFirestore

class AddProductActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddProductBinding
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()

        // Setup toolbar
        binding.toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.primary_green))
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Add Product"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white)
        binding.toolbar.setTitleTextColor(Color.WHITE)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        setupInputValidation()

        binding.btnSave.setOnClickListener {
            val name = binding.etProductName.text.toString().trim()
            val category = binding.etCategory.text.toString().trim()

            binding.etProductName.error = null

            if (name.isEmpty()) {
                binding.etProductName.error = getString(R.string.error_product_name_empty)
                binding.etProductName.requestFocus()
                return@setOnClickListener
            }

            if (name.length < 2) {
                binding.etProductName.error = getString(R.string.error_product_name_short)
                binding.etProductName.requestFocus()
                return@setOnClickListener
            }

            if (name.length > 50) {
                binding.etProductName.error = getString(R.string.error_product_name_long)
                binding.etProductName.requestFocus()
                return@setOnClickListener
            }

            binding.btnSave.isEnabled = false
            binding.btnSave.text = getString(R.string.save)

            val product = Product(
                name = name,
                category = category.ifEmpty { "General" },
                estimatedPrice = 0.0,
                priceCount = 0
            )

            db.collection("products").add(product)
                .addOnSuccessListener {
                    binding.btnSave.isEnabled = true
                    binding.btnSave.text = getString(R.string.save)
                    Toast.makeText(this, R.string.success_product_added, Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    binding.btnSave.isEnabled = true
                    binding.btnSave.text = getString(R.string.save)
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun setupInputValidation() {
        binding.etProductName.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val name = binding.etProductName.text.toString().trim()
                if (name.isNotEmpty() && name.length < 2) {
                    binding.etProductName.error = getString(R.string.error_product_name_short)
                }
            }
        }
    }
}