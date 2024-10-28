package com.example.tokoplastik.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.tokoplastik.data.responses.GetProduct
import com.example.tokoplastik.databinding.ProductListLayoutBinding

class ProductAdapter (private val productList: List<GetProduct>) : RecyclerView.Adapter<ProductAdapter.ViewHolder> () {

    inner class ViewHolder (val binding : ProductListLayoutBinding) : RecyclerView.ViewHolder(binding.root) {

    }

    fun updateData() {
        notifyDataSetChanged()  // Notify the adapter of data changes
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ProductListLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun getItemCount(): Int {
        return productList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentItem = productList[position]
        holder.binding.apply {
            supplierText.text = "( ${currentItem.supplier} )"
            productNameText.text = "( ${currentItem.name} )"
            capitalPriceText.text = "( ${currentItem.capitalPrice} )"
        }
    }
}