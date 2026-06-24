package com.crowdbudget.app.ui

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.crowdbudget.app.R
import com.crowdbudget.app.adapters.ShoppingBasketAdapter
import com.crowdbudget.app.databinding.ActivityShoppingBasketBinding
import com.crowdbudget.app.models.ShoppingListItem
import com.crowdbudget.app.utils.PriceCalculator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import java.util.Random


class ShoppingBasketActivity : AppCompatActivity() {

    private lateinit var binding: ActivityShoppingBasketBinding
    private lateinit var adapter: ShoppingBasketAdapter
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private val basketItems = mutableListOf<ShoppingListItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShoppingBasketBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()


        binding.toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.primary_green))
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "My Shopping Basket"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white)
        binding.toolbar.setTitleTextColor(Color.WHITE)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        setupRecyclerView()
        setupClickListeners()
        loadBasket()
    }

    private fun setupRecyclerView() {
        adapter = ShoppingBasketAdapter(basketItems) { item ->
            removeItem(item)
        }
        binding.recyclerBasketItems.layoutManager = LinearLayoutManager(this)
        binding.recyclerBasketItems.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.btnClearBasket.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Clear Basket")
                .setMessage("Are you sure you want to clear all items?")
                .setPositiveButton("Clear") { _, _ ->
                    val userId = auth.currentUser?.uid ?: return@setPositiveButton
                    val basketId = "current_basket_$userId"

                    // Set basket to empty instead of deleting
                    val emptyBasket = mapOf(
                        "userId" to userId,
                        "name" to "My Shopping Basket",
                        "items" to emptyMap<String, Any>(),
                        "totalEstimate" to 0.0,
                        "updatedAt" to com.google.firebase.Timestamp.now()
                    )

                    db.collection("shoppingBaskets").document(basketId)
                        .set(emptyBasket)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Basket cleared", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.btnSaveBasket.setOnClickListener {
            saveBasketToHistory()
        }

        binding.btnViewHistory.setOnClickListener {
            viewBasketHistory()
        }
    }

    private fun loadBasket() {
        binding.progressBar.visibility = android.view.View.VISIBLE

        val userId = auth.currentUser?.uid ?: return
        val basketId = "current_basket_$userId"

        db.collection("shoppingBaskets").document(basketId)
            .addSnapshotListener { document, error ->
                binding.progressBar.visibility = android.view.View.GONE

                if (error != null) {
                    Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }

                basketItems.clear()

                if (document != null && document.exists()) {
                    val items = document.get("items") as? Map<String, Map<String, Any>> ?: emptyMap()

                    items.values.forEach { itemData ->
                        val listItem = ShoppingListItem(
                            productId = itemData["productId"] as? String ?: "",
                            productName = itemData["productName"] as? String ?: "",
                            quantity = (itemData["quantity"] as? Long)?.toInt() ?: 1,
                            estimatedUnitPrice = itemData["estimatedUnitPrice"] as? Double ?: 0.0
                        )
                        basketItems.add(listItem)
                    }

                    val total = document.getDouble("totalEstimate") ?: 0.0
                    binding.tvTotalEstimate.text = String.format("Total Estimate: ZMW %.2f", total)
                }

                adapter.notifyDataSetChanged()
                binding.tvEmptyState.visibility = if (basketItems.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            }
    }

    private fun removeItem(item: ShoppingListItem) {
        val userId = auth.currentUser?.uid ?: return
        val basketId = "current_basket_$userId"

        db.collection("shoppingBaskets").document(basketId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val items = (document.get("items") as? Map<String, Map<String, Any>>)?.toMutableMap() ?: return@addOnSuccessListener
                    items.remove(item.productId)

                    var total = 0.0
                    items.values.forEach { itemData ->
                        val qty = (itemData["quantity"] as? Long)?.toInt() ?: 1
                        val price = itemData["estimatedUnitPrice"] as? Double ?: 0.0
                        total += qty * price
                    }

                    db.collection("shoppingBaskets").document(basketId)
                        .update(mapOf("items" to items, "totalEstimate" to total))
                        .addOnSuccessListener {
                            Toast.makeText(this, "${item.productName} removed", Toast.LENGTH_SHORT).show()
                        }
                }
            }
    }

    private fun saveBasketToHistory() {
        if (basketItems.isEmpty()) {
            Toast.makeText(this, "Basket is empty. Add items first!", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = android.view.View.VISIBLE

        val userId = auth.currentUser?.uid ?: return
        val currentBasketId = "current_basket_$userId"
        val savedBasketId = db.collection("savedBaskets").document().id
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val currentDate = dateFormat.format(Date())

        db.collection("shoppingBaskets").document(currentBasketId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val savedData = document.data?.toMutableMap() ?: return@addOnSuccessListener
                    savedData["savedAt"] = com.google.firebase.Timestamp.now()
                    savedData["savedDate"] = currentDate
                    savedData["originalId"] = currentBasketId
                    savedData["basketName"] = "Shopping Basket - $currentDate"

                    db.collection("savedBaskets").document(savedBasketId)
                        .set(savedData)
                        .addOnSuccessListener {
                            binding.progressBar.visibility = android.view.View.GONE
                            Toast.makeText(this, "Basket saved to history!", Toast.LENGTH_LONG).show()
                            binding.root.post {
                                showConfetti()
                            }
                        }

                        .addOnFailureListener { e ->
                            binding.progressBar.visibility = android.view.View.GONE
                            Toast.makeText(this, "Error saving: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    binding.progressBar.visibility = android.view.View.GONE
                }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = android.view.View.GONE
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun viewBasketHistory() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Please log in to view history", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = android.view.View.VISIBLE


        db.collection("savedBaskets")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                binding.progressBar.visibility = android.view.View.GONE

                if (documents.isEmpty) {
                    Toast.makeText(this, "No saved baskets yet.\nSave a basket first!", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }


                val message = StringBuilder()
                message.append("Your Saved Baskets:\n\n")

                documents.forEachIndexed { index, doc ->
                    val basketName = doc.getString("basketName") ?: "Basket ${index + 1}"
                    val total = doc.getDouble("totalEstimate") ?: 0.0
                    val savedDate = doc.getString("savedDate") ?: "Unknown date"
                    val itemCount = (doc.get("items") as? Map<*, *>)?.size ?: 0

                    message.append("${index + 1}. $basketName\n")
                    message.append("   Items: $itemCount\n")
                    message.append("   Total: ${PriceCalculator.formatZMW(total)}\n")
                    message.append("   Saved: $savedDate\n\n")
                }

                AlertDialog.Builder(this)
                    .setTitle("📜 Basket History")
                    .setMessage(message.toString())
                    .setPositiveButton("OK", null)
                    .setNeutralButton("Clear All") { _, _ ->
                        clearAllHistory()
                    }
                    .show()
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = android.view.View.GONE
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun clearAllHistory() {
        val userId = auth.currentUser?.uid ?: return

        AlertDialog.Builder(this)
            .setTitle("Clear History")
            .setMessage("Are you sure you want to delete ALL saved baskets?")
            .setPositiveButton("Delete All") { _, _ ->
                db.collection("savedBaskets")
                    .whereEqualTo("userId", userId)
                    .get()
                    .addOnSuccessListener { documents ->
                        for (doc in documents) {
                            doc.reference.delete()
                        }
                        Toast.makeText(this, "All history cleared", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun clearBasketHistory() {
        AlertDialog.Builder(this)
            .setTitle("Clear History")
            .setMessage("Are you sure you want to delete all saved baskets?")
            .setPositiveButton("Delete All") { _, _ ->
                val userId = auth.currentUser?.uid ?: return@setPositiveButton

                db.collection("savedBaskets")
                    .whereEqualTo("userId", userId)
                    .get()
                    .addOnSuccessListener { documents ->
                        for (doc in documents) {
                            doc.reference.delete()
                        }
                        Toast.makeText(this, "History cleared", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
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

            val random = java.util.Random()
            val rootView = binding.root

            for (i in 0..50) {
                val size = (10 + random.nextInt(15))
                val view = android.view.View(this).apply {
                    setBackgroundColor(colors[random.nextInt(colors.size)])
                    layoutParams = android.view.ViewGroup.LayoutParams(size, size)
                    translationX = random.nextInt(rootView.width).toFloat()
                    translationY = (-50f)
                    rotation = random.nextInt(360).toFloat()
                    alpha = 0.9f
                }

                rootView.addView(view)

                view.animate()
                    .translationYBy((rootView.height + 200).toFloat())
                    .translationXBy((random.nextInt(300) - 150).toFloat())
                    .rotationBy(720f)
                    .alpha(0f)
                    .setDuration(1500)
                    .withEndAction {
                        try {
                            rootView.removeView(view)
                        } catch (e: Exception) {
                            // Ignore
                        }
                    }
                    .start()
            }
        } catch (e: Exception) {
            android.util.Log.e("Confetti", "Error: ${e.message}")
        }
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }
}