package com.example.tokoplastik.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.tokoplastik.data.responses.GetProduct
import com.example.tokoplastik.data.responses.ProductPrice
import com.example.tokoplastik.databinding.ProductPriceListLayoutBinding

class ProductPricesAdapter : RecyclerView.Adapter<ProductPricesAdapter.ViewHolder>() {

    private var productList = mutableListOf<ProductPrice>()

    class ViewHolder(private val binding: ProductPriceListLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(price: ProductPrice) {
            binding.unitText.text = price.unit
            binding.quantityPerUnitText.text = "${price.quantityPerUnit} per unit"
            binding.priceText.text = "Rp ${price.price}"
        }
    }

    fun updateList(newProductsPrices: List<ProductPrice>) {
        productList.clear()
        productList.addAll(newProductsPrices)
        notifyDataSetChanged()
    }

    override fun getItemCount() = productList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ProductPriceListLayoutBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(productList[position])
    }
}