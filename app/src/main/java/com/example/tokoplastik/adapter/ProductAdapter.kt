package com.example.tokoplastik.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import cn.pedant.SweetAlert.SweetAlertDialog
import com.example.tokoplastik.data.responses.GetProduct
import com.example.tokoplastik.databinding.ProductListLayoutBinding
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class ProductAdapter(
    private val onDeleteItem: (GetProduct) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ViewHolder> () {

    private var originalList = mutableListOf<GetProduct>()
    private var filteredList = mutableListOf<GetProduct>()
    private var onItemClickListener: ((GetProduct) -> Unit)? = null

    fun setOnItemClickListener(listener: (GetProduct) -> Unit) {
        onItemClickListener = listener
    }

    fun updateList(newProducts: List<GetProduct>) {
        originalList.clear()
        originalList.addAll(newProducts)
        filteredList.clear()
        filteredList.addAll(newProducts)
        notifyDataSetChanged()
    }

    fun removeItem(product: GetProduct) {
        val originalIndex = originalList.indexOfFirst { it == product }
        val filteredIndex = filteredList.indexOfFirst { it == product }

        if (originalIndex != -1) {
            originalList.removeAt(originalIndex)
        }
        if (filteredIndex != -1) {
            filteredList.removeAt(filteredIndex)
            notifyItemRemoved(filteredIndex)
        }
    }

    fun filterProducts(query: String) {
        filteredList = if (query.isEmpty()) {
            originalList.toMutableList()
        } else {
            originalList.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.supplier.contains(query, ignoreCase = true)
            }.toMutableList()
        }
        notifyDataSetChanged()
    }


    companion object {
        fun createSwipeToDelete(
            adapter: ProductAdapter,
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
                        .setTitleText("Hapus Produk")
                        .setContentText("Apakah Anda yakin ingin menghapus produk ini?")
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
                        contentText = "Produk Berhasil Dihapus"
                        setConfirmButton("OK") {
                            dismissWithAnimation()
                        }
                        show()
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ProductListLayoutBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentProduct = filteredList[position]
        holder.bind(currentProduct)
    }

    override fun getItemCount() = filteredList.size

    fun sortByName(ascending: Boolean = true) {
        filteredList = if (ascending) {
            filteredList.sortedBy { it.name }.toMutableList()
        } else {
            filteredList.sortedByDescending { it.name }
        }.toMutableList()
        notifyDataSetChanged()
    }

    fun sortByPrice(ascending: Boolean = true) {
        filteredList = if (ascending) {
            filteredList.sortedBy { it.capitalPrice }.toMutableList()
        } else {
            filteredList.sortedByDescending { it.capitalPrice }
        }.toMutableList()
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

                val symbols = DecimalFormatSymbols(Locale.getDefault()).apply {
                    groupingSeparator = '.'
                }
                val formatter = DecimalFormat("#,###", symbols)
                val formattedTotal = formatter.format(product.capitalPrice)
                nominalText.text = "Rp$formattedTotal"
            }
        }
    }
}