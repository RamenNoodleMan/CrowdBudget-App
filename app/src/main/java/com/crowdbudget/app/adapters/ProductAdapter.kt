package com.crowdbudget.app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.crowdbudget.app.databinding.ItemProductBinding
import com.crowdbudget.app.models.Product
import com.crowdbudget.app.utils.PriceCalculator

class ProductAdapter(
    private val products: List<Product>,
    private val onAddClick: (Product) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(products[position])
    }

    override fun getItemCount(): Int = products.size

    inner class ProductViewHolder(
        private val binding: ItemProductBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(product: Product) {
            binding.tvProductName.text = product.name
            binding.tvProductCategory.text = product.category.ifEmpty { "General" }
            binding.tvEstimatedPrice.text = PriceCalculator.formatZMW(product.estimatedPrice)
            binding.tvPriceCount.text = "${product.priceCount} submissions"

            // Add trend indicator
            val trend = PriceCalculator.getPriceTrend(product.estimatedPrice, product.previousPrice)
            val trendMessage = PriceCalculator.getTrendMessage(product.estimatedPrice, product.previousPrice)

            // You'll need to add a new TextView in your item_product.xml for the trend
            // For now, we can combine it with the price count
            binding.tvPriceCount.text = "$trend ${product.priceCount} submissions | $trendMessage"

            binding.btnAdd.setOnClickListener {
                onAddClick(product)
            }
        }


    }


}