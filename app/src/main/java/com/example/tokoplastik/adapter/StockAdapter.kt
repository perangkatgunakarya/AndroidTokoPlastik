package com.example.tokoplastik.adapter

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import cn.pedant.SweetAlert.SweetAlertDialog
import com.example.tokoplastik.data.responses.GetProduct
import com.example.tokoplastik.data.responses.Stock
import com.example.tokoplastik.databinding.StockListLayoutBinding
import java.text.SimpleDateFormat
import java.util.TimeZone

class StockAdapter (
    private val onDeleteItem: (Stock) -> Unit
) : RecyclerView.Adapter<StockAdapter.ViewHolder>() {

    private var originalList = mutableListOf<Stock>()
    private var filteredList = mutableListOf<Stock>()

    fun updateList(newStock: List<Stock>) {
        originalList.clear()
        originalList.addAll(newStock)
        filteredList.clear()
        filteredList.addAll(newStock)
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

    fun removeItem(stock: Stock) {
        val originalIndex = originalList.indexOfFirst { it == stock }
        val filteredIndex = filteredList.indexOfFirst { it == stock }

        if (originalIndex != -1) {
            originalList.removeAt(originalIndex)
        }
        if (filteredIndex != -1) {
            filteredList.removeAt(filteredIndex)
            notifyItemRemoved(filteredIndex)
        }
    }

    companion object {
        fun createSwipeToDelete(
            adapter: StockAdapter,
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
                        .setTitleText("Hapus Riwayat Stok")
                        .setContentText("Apakah Anda yakin ingin menghapus riwayat stok?")
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
                        contentText = "Stok Berhasil Dihapus"
                        setConfirmButton("OK") {
                            dismissWithAnimation()
                        }
                        show()
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StockAdapter.ViewHolder {
        val binding = StockListLayoutBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StockAdapter.ViewHolder, position: Int) {
        val currentStock = filteredList[position]
        holder.bind(currentStock)
    }

    override fun getItemCount() = filteredList.size

    inner class ViewHolder(private val binding: StockListLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(stock: Stock) {
            binding.apply {
                val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'")
                parser.timeZone = TimeZone.getTimeZone("UTC")
                val date = parser.parse(stock.createdAt)
                dateText.text = SimpleDateFormat("dd MMM Y").format(date)
                clockText.text = SimpleDateFormat("HH:mm").format(date)

                latestStockValue.text = stock.latestStock.toString()

                if (stock.type == "restock") {
                    typeText.text = stock.type
                    typeText.setTextColor(Color.GREEN)
                    quantityText.text = stock.quantity.toString()
                    quantityText.setTextColor(Color.GREEN)
                } else {
                    typeText.text = stock.type
                    typeText.setTextColor(Color.RED)
                    quantityText.text = stock.quantity.toString()
                    quantityText.setTextColor(Color.RED)
                }

                currentStockValue.text = stock.currentStock.toString()
            }
        }
    }
}