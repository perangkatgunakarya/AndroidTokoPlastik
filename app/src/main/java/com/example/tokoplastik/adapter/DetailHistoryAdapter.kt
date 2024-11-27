package com.example.tokoplastik.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.tokoplastik.data.responses.TransactionDetail
import com.example.tokoplastik.data.responses.TransactionDetailProduct
import com.example.tokoplastik.databinding.DetailHistoryListLayoutBinding

class DetailHistoryAdapter : RecyclerView.Adapter<DetailHistoryAdapter.ViewHolder>()  {

    private var items = mutableListOf<TransactionDetailProduct>()
    private var total = ""

    inner class ViewHolder(private val binding: DetailHistoryListLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
            fun bind(item: TransactionDetailProduct, total: String) {
                binding.apply {
                    productText.text = item.productPrice.product.name
                    quantityText.text = item.quantity.toString()
                    unitText.text = item.productPrice.unit
                    adjustedPriceText.text = item.priceAdjustment.toString()
                    totalText.text = total
                }
            }
    }

    fun updateList(detailTransactions: List<TransactionDetailProduct>, total: String) {
        items.clear()
        items.addAll(detailTransactions)
        this.total = total
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): DetailHistoryAdapter.ViewHolder {
        val binding = DetailHistoryListLayoutBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], total)
    }

    override fun getItemCount() = items.size
}