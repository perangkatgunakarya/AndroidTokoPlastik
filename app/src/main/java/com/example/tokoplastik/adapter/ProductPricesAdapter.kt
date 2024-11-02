package com.example.tokoplastik.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.tokoplastik.data.responses.GetProduct
import com.example.tokoplastik.data.responses.ProductPrice
import com.example.tokoplastik.databinding.ProductPriceListLayoutBinding

class ProductPricesAdapter : ListAdapter<ProductPrice, ProductPricesAdapter.ViewHolder>(ProductPriceDiffCallback()) {

    private var productList = listOf<ProductPrice>()

    class ViewHolder(private val binding: ProductPriceListLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(price: ProductPrice) {
            binding.unitText.text = price.unit
            binding.quantityPerUnitText.text = "${price.quantityPerUnit} per unit"
            binding.priceText.text = "Rp ${price.price}"
        }
    }

    fun updateList(newProductsPrices: List<ProductPrice>) {
        productList = newProductsPrices
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ProductPriceListLayoutBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class ProductPriceDiffCallback : DiffUtil.ItemCallback<ProductPrice>() {
    override fun areItemsTheSame(oldItem: ProductPrice, newItem: ProductPrice) =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: ProductPrice, newItem: ProductPrice) =
        oldItem == newItem
}