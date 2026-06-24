package com.crowdbudget.app.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.crowdbudget.app.adapters.ShoppingListAdapter
import com.crowdbudget.app.databinding.ActivityShoppingListBinding
import com.crowdbudget.app.models.ShoppingListItem
import com.crowdbudget.app.utils.PriceCalculator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import com.crowdbudget.app.R

class ShoppingListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityShoppingListBinding
    private lateinit var adapter: ShoppingListAdapter
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private val shoppingItems = mutableListOf<ShoppingListItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShoppingListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        setupRecyclerView()
        setupClickListeners()
        loadShoppingList()
    }

    private fun setupRecyclerView() {
        adapter = ShoppingListAdapter(shoppingItems) { item ->
            removeItem(item)
        }
        binding.recyclerShoppingItems.layoutManager = LinearLayoutManager(this)
        binding.recyclerShoppingItems.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.btnSaveList.setOnClickListener {
            saveCurrentList()
        }

        binding.btnClearList.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Clear Shopping List")
                .setMessage("Are you sure you want to clear all items?")
                .setPositiveButton("Clear") { _, _ ->
                    val userId = auth.currentUser?.uid ?: return@setPositiveButton
                    db.collection("shoppingLists").document("current_list_$userId").delete()
                        .addOnSuccessListener {
                            Toast.makeText(this, R.string.success_list_cleared, Toast.LENGTH_SHORT).show()
                        }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun loadShoppingList() {
        binding.progressBar.visibility = android.view.View.VISIBLE

        val userId = auth.currentUser?.uid ?: return
        val listId = "current_list_$userId"

        db.collection("shoppingLists").document(listId)
            .addSnapshotListener { document, error ->
                binding.progressBar.visibility = android.view.View.GONE

                if (error != null) {
                    Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }

                shoppingItems.clear()

                if (document != null && document.exists()) {
                    val items = document.get("items") as? Map<String, Map<String, Any>> ?: emptyMap()

                    items.values.forEach { itemData ->
                        val listItem = ShoppingListItem(
                            productId = itemData["productId"] as? String ?: "",
                            productName = itemData["productName"] as? String ?: "",
                            quantity = (itemData["quantity"] as? Long)?.toInt() ?: 1,
                            estimatedUnitPrice = itemData["estimatedUnitPrice"] as? Double ?: 0.0
                        )
                        shoppingItems.add(listItem)
                    }

                    val total = document.getDouble("totalEstimate") ?: 0.0
                    binding.tvTotalEstimate.text = String.format("Total Estimate: ZMW %.2f", total)
                }

                adapter.notifyDataSetChanged()
                binding.tvEmptyState.visibility = if (shoppingItems.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            }
    }

    private fun removeItem(item: ShoppingListItem) {
        val userId = auth.currentUser?.uid ?: return
        val listId = "current_list_$userId"

        db.collection("shoppingLists").document(listId).get()
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

                    db.collection("shoppingLists").document(listId)
                        .update(mapOf("items" to items, "totalEstimate" to total))
                        .addOnSuccessListener {
                            Toast.makeText(this, "${item.productName} removed", Toast.LENGTH_SHORT).show()
                        }
                }
            }
    }

    private fun saveCurrentList() {
        val userId = auth.currentUser?.uid ?: return
        val currentListId = "current_list_$userId"

        db.collection("shoppingLists").document(currentListId).get()
            .addOnSuccessListener { document ->
                if (document.exists() && shoppingItems.isNotEmpty()) {
                    val data = document.data ?: return@addOnSuccessListener
                    val savedListId = db.collection("savedShoppingLists").document().id
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

                    val savedList = mapOf(
                        "id" to savedListId,
                        "userId" to userId,
                        "name" to "Shopping List ${dateFormat.format(Date())}",
                        "items" to data["items"],
                        "totalEstimate" to data["totalEstimate"],
                        "createdAt" to com.google.firebase.Timestamp.now()
                    )

                    db.collection("savedShoppingLists").document(savedListId).set(savedList)
                        .addOnSuccessListener {
                            Toast.makeText(this, R.string.success_list_saved, Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "No items to save", Toast.LENGTH_SHORT).show()
                }
            }
    }
}