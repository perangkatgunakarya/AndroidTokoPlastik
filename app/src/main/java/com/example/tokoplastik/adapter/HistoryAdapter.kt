package com.example.tokoplastik.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.tokoplastik.data.responses.AllTransaction
import com.example.tokoplastik.databinding.HistoryListLayoutBinding
import java.text.SimpleDateFormat
import java.util.TimeZone

class HistoryAdapter : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    private var originalList = mutableListOf<AllTransaction>()
    private var filteredList = mutableListOf<AllTransaction>()
    private var onItemClickListener: ((AllTransaction) -> Unit)? = null

    fun setOnItemClickListener(listener: (AllTransaction) -> Unit) {
        onItemClickListener = listener
    }

    fun updateList(newTransactions: List<AllTransaction>) {
        originalList.clear()
        originalList.addAll(newTransactions)
        filteredList.clear()
        filteredList.addAll(newTransactions)
        notifyDataSetChanged()
    }

    fun filterTransactions(query: String) {
        filteredList = if (query.isEmpty()) {
            originalList.toMutableList()
        } else {
            originalList.filter {
                it.customer.name.contains(query, ignoreCase = true) ||
                        it.createdAt.contains(query, ignoreCase = true)
            }.toMutableList()
        }
        notifyDataSetChanged()
    }

    fun sortByDate(ascending: Boolean = true) {
        filteredList = if (ascending) {
            filteredList.sortedBy { it.createdAt }.toMutableList()
        } else {
            filteredList.sortedByDescending { it.createdAt }
        }.toMutableList()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryAdapter.ViewHolder {
        val binding = HistoryListLayoutBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryAdapter.ViewHolder, position: Int) {
        val currentTransaction = filteredList[position]
        holder.bind(currentTransaction)
    }

    override fun getItemCount() = filteredList.size

    inner class ViewHolder(private val binding: HistoryListLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClickListener?.invoke(filteredList[position])
                }
            }
        }

        fun bind(transaction: AllTransaction) {
            binding.apply {
                customerText.text = "(${transaction.customer.name})"

                val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'")
                parser.timeZone = TimeZone.getTimeZone("UTC")
                val date = parser.parse(transaction.createdAt)
                dateText.text = SimpleDateFormat("dd MMM Y").format(date)
                clockText.text = SimpleDateFormat("HH:mm").format(date)

                totalText.text = "Rp. ${transaction.total}"

                if (transaction.paymentStatus == "belum lunas" || transaction.paymentStatus == "dalam cicilan") {
                    capLunas.visibility = View.GONE
                } else {
                    capLunas.visibility = View.VISIBLE
                }
            }
        }
    }

}