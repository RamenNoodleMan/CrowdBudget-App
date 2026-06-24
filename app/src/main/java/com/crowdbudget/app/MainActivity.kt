package com.crowdbudget.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.crowdbudget.app.R
import com.crowdbudget.app.adapters.ProductAdapter
import com.crowdbudget.app.auth.LoginActivity
import com.crowdbudget.app.databinding.ActivityMainBinding
import com.crowdbudget.app.models.Product
import com.crowdbudget.app.ui.PriceContributionActivity
import com.crowdbudget.app.ui.ShoppingBasketActivity
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.widget.TextView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var productAdapter: ProductAdapter
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var toggle: ActionBarDrawerToggle

    private val products = mutableListOf<Product>()
    private var filteredProducts = mutableListOf<Product>()

    private lateinit var fabBasket: FloatingActionButton

    private var selectedCategory: String = "All"
    //private lateinit var basketBadge: TextView
    //private var basketItemCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // In onCreate, after setContentView:
        binding.toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.primary_green))
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Product Search"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white)
        binding.toolbar.setTitleTextColor(Color.WHITE)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        setupDrawerNavigation()
        setupFloatingBasketButton()
        //loadBasketCount()
        setupRecyclerView()
        setupCategoryFilters()
        setupSearchView()
        loadProducts()

        supportActionBar?.title = "Product Search"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Product Search"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setTitleTextColor(android.graphics.Color.WHITE)  // ← ADD THIS

    }



    private fun setupFloatingBasketButton() {
        fabBasket = binding.fabBasket

        fabBasket.setOnClickListener {
            // Show count in toast
            val userId = auth.currentUser?.uid
            if (userId != null) {
                val basketId = "current_basket_$userId"
                db.collection("shoppingBaskets").document(basketId).get()
                    .addOnSuccessListener { document ->
                        val items = document.get("items") as? Map<String, Map<String, Any>> ?: emptyMap()
                        val count = items.size
                        Toast.makeText(this, "🛒 $count item(s) in your basket", Toast.LENGTH_SHORT).show()
                    }
            }
            startActivity(Intent(this, ShoppingBasketActivity::class.java))
        }
    }

    private fun setupDrawerNavigation() {

        drawerLayout = binding.drawerLayout
        navView = binding.navView


        toggle = ActionBarDrawerToggle(this, drawerLayout, binding.toolbar, R.string.open, R.string.close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_product_search -> {
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_shopping_basket -> {
                    startActivity(Intent(this, ShoppingBasketActivity::class.java))
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_price_contribution -> {
                    startActivity(Intent(this, PriceContributionActivity::class.java))
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_logout -> {
                    auth.signOut()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                    drawerLayout.closeDrawers()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupRecyclerView() {
        productAdapter = ProductAdapter(filteredProducts) { product ->
            addToShoppingList(product)
        }
        binding.recyclerProducts.layoutManager = LinearLayoutManager(this)
        binding.recyclerProducts.adapter = productAdapter
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterProducts(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterProducts(newText)
                return true
            }
        })
    }

    private fun filterProducts(query: String?) {
        if (query.isNullOrEmpty()) {
            // No search query, just filter by category
            filteredProducts.clear()
            if (selectedCategory == "All") {
                filteredProducts.addAll(products)
            } else {
                filteredProducts.addAll(products.filter {
                    it.category.equals(selectedCategory, ignoreCase = true)
                })
            }
        } else {
            // Filter by both category AND search query
            filteredProducts.clear()
            val categoryFiltered = if (selectedCategory == "All") {
                products
            } else {
                products.filter { it.category.equals(selectedCategory, ignoreCase = true) }
            }
            filteredProducts.addAll(categoryFiltered.filter {
                it.name.contains(query, ignoreCase = true)
            })
        }
        productAdapter.notifyDataSetChanged()
        binding.tvEmptyState.visibility = if (filteredProducts.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun setupCategoryFilters() {
        val chipAll = binding.chipAll
        val chipBakery = binding.chipBakery
        val chipDairy = binding.chipDairy
        val chipMeat = binding.chipMeat
        val chipVegetables = binding.chipVegetables
        val chipFruits = binding.chipFruits
        val chipPantry = binding.chipPantry
        val chipGrains = binding.chipGrains

        chipAll.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedCategory = "All"
                filterByCategory()
            }
        }

        chipBakery.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedCategory = "Bakery"
                filterByCategory()
            }
        }

        chipDairy.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedCategory = "Dairy"
                filterByCategory()
            }
        }

        chipMeat.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedCategory = "Meat"
                filterByCategory()
            }
        }

        chipVegetables.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedCategory = "Vegetables"
                filterByCategory()
            }
        }

        chipFruits.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedCategory = "Fruits"
                filterByCategory()
            }
        }

        chipPantry.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedCategory = "Pantry"
                filterByCategory()
            }
        }

        chipGrains.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedCategory = "Grains"
                filterByCategory()
            }
        }
    }

    private fun filterByCategory() {
        if (selectedCategory == "All") {
            filteredProducts.clear()
            filteredProducts.addAll(products)
        } else {
            filteredProducts.clear()
            filteredProducts.addAll(products.filter {
                it.category.equals(selectedCategory, ignoreCase = true)
            })
        }

        // Also respect search query if there is one
        val searchQuery = binding.searchView.query.toString()
        if (searchQuery.isNotEmpty()) {
            filterProducts(searchQuery)
        } else {
            productAdapter.notifyDataSetChanged()
            binding.tvEmptyState.visibility = if (filteredProducts.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        }
    }

    private fun loadProducts() {
        binding.progressBar.visibility = android.view.View.VISIBLE

        db.collection("products")
            .orderBy("name")
            .addSnapshotListener { snapshot, error ->
                binding.progressBar.visibility = android.view.View.GONE

                if (error != null) {
                    Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }

                products.clear()
                snapshot?.documents?.forEach { doc ->
                    val product = doc.toObject(Product::class.java)
                    product?.let {
                        it.id = doc.id
                        products.add(it)
                    }
                }
                filteredProducts.clear()
                filteredProducts.addAll(products)
                productAdapter.notifyDataSetChanged()
                binding.tvEmptyState.visibility = if (filteredProducts.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            }
    }

    private fun addToShoppingList(product: Product) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_quantity, null)
        val quantityInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editQuantity)

        AlertDialog.Builder(this)
            .setTitle("Add to Shopping Basket")
            .setMessage("${product.name}")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val quantityText = quantityInput.text.toString().trim()
                val quantity = quantityText.toIntOrNull()

                // QUANTITY VALIDATION - Add this entire block
                if (quantityText.isEmpty()) {
                    Toast.makeText(this, "Please enter a quantity", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (quantity == null) {
                    Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (quantity < 1) {
                    Toast.makeText(this, "Quantity must be at least 1", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (quantity > 99) {
                    Toast.makeText(this, "Quantity cannot exceed 99", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val userId = auth.currentUser?.uid
                if (userId == null) {
                    Toast.makeText(this, "You need to be logged in", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val basketId = "current_basket_$userId"
                val basketRef = db.collection("shoppingBaskets").document(basketId)

                basketRef.get()
                    .addOnSuccessListener { document ->
                        val currentItems = if (document.exists()) {
                            (document.get("items") as? Map<String, Map<String, Any>>)?.toMutableMap() ?: mutableMapOf()
                        } else {
                            mutableMapOf()
                        }

                        val existingItem = currentItems[product.id]
                        val newQuantity = if (existingItem != null) {
                            val existingQty = (existingItem["quantity"] as? Long)?.toInt() ?: 0
                            existingQty + quantity
                        } else {
                            quantity
                        }

                        val itemData = mapOf(
                            "productId" to product.id,
                            "productName" to product.name,
                            "quantity" to newQuantity,
                            "estimatedUnitPrice" to product.estimatedPrice
                        )

                        currentItems[product.id] = itemData

                        var total = 0.0
                        currentItems.values.forEach { item ->
                            val qty = (item["quantity"] as? Long)?.toInt() ?: 1
                            val price = item["estimatedUnitPrice"] as? Double ?: 0.0
                            total += qty * price
                        }

                        val basketData = mapOf(
                            "userId" to userId,
                            "name" to "My Shopping Basket",
                            "items" to currentItems,
                            "totalEstimate" to total,
                            "updatedAt" to com.google.firebase.Timestamp.now()
                        )

                        basketRef.set(basketData)
                            .addOnSuccessListener {
                                Toast.makeText(this, "${product.name} added (Qty: $newQuantity)", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}