package com.example.tokoplastik.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.tokoplastik.data.responses.GetProduct
import com.example.tokoplastik.databinding.ProductListLayoutBinding

class ProductAdapter (private var productList: List<GetProduct>) : RecyclerView.Adapter<ProductAdapter.ViewHolder> () {

    private var originalList = listOf<GetProduct>()  // Store original list
    private var filteredList = listOf<GetProduct>()  // Store filtered list
    private var onItemClickListener: ((GetProduct) -> Unit)? = null

    fun setOnItemClickListener(listener: (GetProduct) -> Unit) {
        onItemClickListener = listener
    }

    fun updateList(newProducts: List<GetProduct>) {
        originalList = newProducts
        filteredList = newProducts  // Initialize filtered list with all products
        notifyDataSetChanged()
    }

    fun filterProducts(query: String) {
        filteredList = if (query.isEmpty()) {
            originalList  // If query is empty, show all products
        } else {
            originalList.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.supplier.contains(query, ignoreCase = true)
            }
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ProductListLayoutBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Use filteredList instead of products
        val currentProduct = filteredList[position]
        holder.bind(currentProduct)
    }

    override fun getItemCount() = filteredList.size  // Return filtered list size

    fun sortByName(ascending: Boolean = true) {
        filteredList = if (ascending) {
            filteredList.sortedBy { it.name }
        } else {
            filteredList.sortedByDescending { it.name }
        }
        notifyDataSetChanged()
    }

    fun sortByPrice(ascending: Boolean = true) {
        filteredList = if (ascending) {
            filteredList.sortedBy { it.capitalPrice }
        } else {
            filteredList.sortedByDescending { it.capitalPrice }
        }
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val binding: ProductListLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClickListener?.invoke(filteredList[position])
                }
            }
        }

        fun bind(product: GetProduct) {
            binding.apply {
                supplierText.text = "(${product.supplier})"
                productNameText.text = product.name
                nominalText.text = "Rp ${product.capitalPrice}"
            }
        }
    }
}