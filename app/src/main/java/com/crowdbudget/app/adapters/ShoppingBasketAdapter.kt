package com.crowdbudget.app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.crowdbudget.app.databinding.ItemShoppingBasketBinding
import com.crowdbudget.app.models.ShoppingListItem
import com.crowdbudget.app.utils.PriceCalculator

class ShoppingBasketAdapter(
    private val items: List<ShoppingListItem>,
    private val onRemoveClick: (ShoppingListItem) -> Unit
) : RecyclerView.Adapter<ShoppingBasketAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemShoppingBasketBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(
        private val binding: ItemShoppingBasketBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ShoppingListItem) {
            binding.tvProductName.text = item.productName
            binding.tvQuantity.text = "Quantity: ${item.quantity}"
            binding.tvPrice.text = PriceCalculator.formatZMW(item.estimatedUnitPrice) + " each"
            binding.tvTotal.text = PriceCalculator.formatZMW(item.totalPrice)

            binding.btnRemove.setOnClickListener {
                onRemoveClick(item)
            }
        }
    }
}