package com.example.tokoplastik.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import cn.pedant.SweetAlert.SweetAlertDialog
import com.example.tokoplastik.data.responses.AllTransaction
import com.example.tokoplastik.data.responses.GetProduct
import com.example.tokoplastik.data.responses.Transaction
import com.example.tokoplastik.databinding.HistoryListLayoutBinding
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class HistoryAdapter (
    private val onDeleteItem: (AllTransaction) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    private var originalList = mutableListOf<AllTransaction>()
    private var filteredList = mutableListOf<AllTransaction>()
    private var onItemClickListener: ((AllTransaction) -> Unit)? = null

    private fun getInitials(customerName: String): String {
        return customerName.split(" ")
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercase() }
            .joinToString("")
    }

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

    fun removeItem(history: AllTransaction) {
        val originalIndex = originalList.indexOfFirst { it == history }
        val filteredIndex = filteredList.indexOfFirst { it == history }

        if (originalIndex != -1) {
            originalList.removeAt(originalIndex)
        }
        if (filteredIndex != -1) {
            filteredList.removeAt(filteredIndex)
            notifyItemRemoved(filteredIndex)
        }
    }

    fun sortByDate(ascending: Boolean = true) {
        filteredList = if (ascending) {
            filteredList.sortedBy { it.createdAt }.toMutableList()
        } else {
            filteredList.sortedByDescending { it.createdAt }
        }.toMutableList()
        notifyDataSetChanged()
    }

    companion object {
        fun createSwipeToDelete(
            adapter: HistoryAdapter,
            context: Context
        ): ItemTouchHelper.SimpleCallback {
            return object : ItemTouchHelper.SimpleCallback (
                0,
                ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
            ) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean = false

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val position = viewHolder.adapterPosition
                    val item = adapter.originalList[position]

                    SweetAlertDialog(context, SweetAlertDialog.WARNING_TYPE)
                        .setTitleText("Hapus Transaksi")
                        .setContentText("Apakah Anda yakin ingin menghapus transaksi ini?")
                        .setConfirmText("Hapus")
                        .setConfirmClickListener { sDialog ->
                            adapter.removeItem(item)
                            adapter.onDeleteItem(item)
                            sDialog.dismissWithAnimation()
                            confirm()
                        }
                        .setCancelText("Cancel")
                        .setCancelClickListener { sDialog ->
                            adapter.notifyItemChanged(position)
                            sDialog.dismissWithAnimation()
                        }
                        .show()
                }

                fun confirm() {
                    SweetAlertDialog(context, SweetAlertDialog.SUCCESS_TYPE).apply {
                        contentText = "Transaksi Berhasil Dihapus"
                        setConfirmButton("OK") {
                            dismissWithAnimation()
                        }
                        show()
                    }
                }
            }
        }
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
                customerText.text = "${transaction.customer.name}"

                val initial = getInitials(transaction.customer.name)
                historyInitial.text = initial

                val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'")
                parser.timeZone = TimeZone.getTimeZone("UTC")
                val date = parser.parse(transaction.createdAt)
                dateText.text = SimpleDateFormat("dd MMM Y").format(date)
                clockText.text = SimpleDateFormat("HH:mm").format(date)

                val symbols = DecimalFormatSymbols(Locale.getDefault()).apply {
                    groupingSeparator = '.'
                }
                val formatter = DecimalFormat("#,###", symbols)

                val formattedBalance = formatter.format(transaction.total - transaction.paid)
                balanceValue.text = "Rp$formattedBalance"

                val formattedTotal = formatter.format(transaction.total)
                totalValue.text = "Rp$formattedTotal"

                if (transaction.paymentStatus == "belum lunas" || transaction.paymentStatus == "dalam cicilan") {
                    capLunas.visibility = View.GONE
                } else {
                    capLunas.visibility = View.VISIBLE
                }
            }
        }
    }

}