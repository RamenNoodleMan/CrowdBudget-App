package com.crowdbudget.app.ui

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.crowdbudget.app.R
import com.crowdbudget.app.databinding.ActivityPreviousListsBinding
import com.crowdbudget.app.utils.PriceCalculator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PreviousListsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPreviousListsBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private val savedLists = mutableListOf<Map<String, Any>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPreviousListsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Setup toolbar
        binding.toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.primary_green))
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Previous Lists"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white)
        binding.toolbar.setTitleTextColor(Color.WHITE)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        loadSavedLists()
    }

    private fun loadSavedLists() {
        binding.progressBar.visibility = View.VISIBLE

        val userId = auth.currentUser?.uid ?: return

        db.collection("savedBaskets")
            .whereEqualTo("userId", userId)
            .orderBy("savedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                binding.progressBar.visibility = View.GONE
                savedLists.clear()
                savedLists.addAll(documents.documents.mapNotNull { it.data })

                if (savedLists.isEmpty()) {
                    binding.tvEmptyState.visibility = View.VISIBLE
                    binding.recyclerSavedLists.visibility = View.GONE
                } else {
                    binding.tvEmptyState.visibility = View.GONE
                    binding.recyclerSavedLists.visibility = View.VISIBLE
                    setupRecyclerView()
                }
            }
            .addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Error loading saved lists", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupRecyclerView() {
        binding.recyclerSavedLists.layoutManager = LinearLayoutManager(this)
        binding.recyclerSavedLists.adapter = SavedListsAdapter(savedLists)
    }

    inner class SavedListsAdapter(private val lists: List<Map<String, Any>>) : RecyclerView.Adapter<SavedListsAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = layoutInflater.inflate(android.R.layout.simple_list_item_2, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val list = lists[position]
            val name = list["basketName"] as? String ?: list["name"] as? String ?: "Shopping List"
            val total = list["totalEstimate"] as? Double ?: 0.0
            val savedDate = list["savedDate"] as? String ?: ""
            holder.bind(name, total, savedDate)
        }

        override fun getItemCount(): Int = lists.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val text1 = itemView.findViewById<TextView>(android.R.id.text1)
            private val text2 = itemView.findViewById<TextView>(android.R.id.text2)

            fun bind(name: String, total: Double, savedDate: String) {
                text1.text = name
                text2.text = if (savedDate.isNotEmpty()) {
                    "${PriceCalculator.formatZMW(total)} - Saved: $savedDate"
                } else {
                    PriceCalculator.formatZMW(total)
                }
            }
        }
    }
}