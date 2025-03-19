package com.example.tokoplastik.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import cn.pedant.SweetAlert.SweetAlertDialog
import com.example.tokoplastik.data.responses.GetProduct
import com.example.tokoplastik.data.responses.ProductPrice
import com.example.tokoplastik.databinding.ProductPriceListLayoutBinding
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class ProductPricesAdapter(
    private val onItemClick: (ProductPrice) -> Unit,
    private val onDeleteItem: (ProductPrice) -> Unit
) : RecyclerView.Adapter<ProductPricesAdapter.ViewHolder>() {

    private var productList = mutableListOf<ProductPrice>()

    class ViewHolder(private val binding: ProductPriceListLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(price: ProductPrice, onItemClick: (ProductPrice) -> Unit) {
            binding.root.setOnClickListener { onItemClick(price) }  // Add click listener

            binding.unitText.text = price.unit
            binding.quantityPerUnitText.text = "isi ${price.quantityPerUnit} ${price.product.lowestUnit}"

            val symbols = DecimalFormatSymbols(Locale.getDefault()).apply {
                groupingSeparator = '.'
            }
            val formatter = DecimalFormat("#,###", symbols)
            val formattedPrice = formatter.format(price.price)

            binding.priceText.text = "Rp${formattedPrice}"
        }
    }

    fun updateList(newProductsPrices: List<ProductPrice>) {
        productList.clear()
        productList.addAll(newProductsPrices)
        notifyDataSetChanged()
    }

    fun removeItem(productPrice: ProductPrice) {
        val originalIndex = productList.indexOfFirst { it == productPrice }

        if (originalIndex != -1) {
            productList.removeAt(originalIndex)
            notifyItemRemoved(originalIndex)
        }
    }

    companion object {
        fun createSwipeToDelete(
            adapter: ProductPricesAdapter,
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
                    val item = adapter.productList[position]

                    SweetAlertDialog(context, SweetAlertDialog.WARNING_TYPE)
                        .setTitleText("Hapus Produk")
                        .setContentText("Apakah Anda yakin ingin menghapus harga satuan ini?")
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
                        contentText = "Harga Satuan Berhasil Dihapus"
                        setConfirmButton("OK") {
                            dismissWithAnimation()
                        }
                        show()
                    }
                }
            }
        }
    }

    override fun getItemCount() = productList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ProductPriceListLayoutBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(productList[position], onItemClick)
    }
}