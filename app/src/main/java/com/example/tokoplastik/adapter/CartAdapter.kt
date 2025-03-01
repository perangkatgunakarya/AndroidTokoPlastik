package com.example.tokoplastik.adapter

import android.R
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.tokoplastik.data.responses.CartItem
import com.example.tokoplastik.data.responses.ProductPrice
import com.example.tokoplastik.databinding.CartItemLayoutBinding
import com.example.tokoplastik.util.getRawValue
import com.example.tokoplastik.util.setNumberFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class CartAdapter(
    private val onQuantityChanged: (CartItem, Int) -> Unit,
    private val onPriceChanged: (CartItem, Int) -> Unit,
    private val onUnitChanged: (CartItem, ProductPrice) -> Unit,
    private val onDeleteItem: (CartItem) -> Unit,
    private val onItemFocused: (Int) -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {
    private var items = mutableListOf<CartItem>()
    private var tempPrices = mutableMapOf<Int, Int>()
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private var lastClickTime = 0L
    private val CLICK_TIME_INTERVAL = 50L

    private fun isClickValid(): Boolean {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime < CLICK_TIME_INTERVAL) {
            return false
        }
        lastClickTime = currentTime
        return true
    }

    private fun getInitials(productName: String?): String? {
        return productName?.split(" ")
            ?.take(2)
            ?.mapNotNull { it.firstOrNull()?.uppercase() }
            ?.joinToString("")
    }

    fun updateItems(newItems: List<CartItem>) {
        scope.launch {
            try {
                val diffCallback = CartDiffCallback()
                val diffResult = withContext(Dispatchers.Default) {
                    DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                        override fun getOldListSize(): Int = items.size
                        override fun getNewListSize(): Int = newItems.size

                        override fun areItemsTheSame(
                            oldItemPosition: Int,
                            newItemPosition: Int
                        ): Boolean {
                            return items[oldItemPosition].product?.data?.product?.id ==
                                    newItems[newItemPosition].product?.data?.product?.id
                        }

                        override fun areContentsTheSame(
                            oldItemPosition: Int,
                            newItemPosition: Int
                        ): Boolean {
                            return items[oldItemPosition] == newItems[newItemPosition]
                        }
                    })
                }

                items.clear()
                items.addAll(newItems)
                diffResult.dispatchUpdatesTo(this@CartAdapter)
            } catch (e: Exception) {
                Log.e("CartAdapter", "Error updating items", e)
            }
        }
    }

    inner class CartViewHolder(private val binding: CartItemLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CartItem) {
            try {
                binding.apply {
                    priceEdit.setNumberFormatter()

                    val initial = getInitials(item.product?.data?.product?.name)
                    productImage.text = initial

                    productText.text = item.product?.data?.product?.name

                    val symbols = DecimalFormatSymbols(Locale.getDefault()).apply {
                        groupingSeparator = '.'
                    }
                    val formatter = DecimalFormat("#,###", symbols)
                    val formattedTotal =
                        formatter.format(item.product?.data?.product?.newestCapitalPrice)
                    modalPrice.text = "Modal : Rp$formattedTotal"

                    supplierText.text = item.product?.data?.product?.supplier

                    val unitsAdapter = ArrayAdapter(
                        itemView.context,
                        android.R.layout.simple_spinner_item,
                        item.productPrice.map { it.unit }
                    )
                    unitsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    unitsSpinner.adapter = unitsAdapter
                    val selectedPrice = item.productPrice[position]
                    binding.lowestUnitQuantity.text =
                        "(${(selectedPrice.quantityPerUnit)} ${item.product?.data?.product?.lowestUnit})"


                    binding.quantityText.text = item.quantity.toString()
                    val initialUnit = item.selectedPrice.unit
                    val position = item.productPrice.indexOfFirst { it.unit == initialUnit }
                    if (position != -1) {
                        unitsSpinner.setSelection(position)
                        if (item.customPrice != item.selectedPrice.price) {
                            tempPrices[adapterPosition] = item.customPrice
                        } else {
                            tempPrices[adapterPosition] = item.selectedPrice.price
                        }
                    }

                    if (!priceEdit.hasFocus()) {
                        val price = tempPrices[adapterPosition] ?: item.customPrice
                        priceEdit.setText(price.toString())
                    } else {
                        onItemFocused(absoluteAdapterPosition)
                    }

                    priceEdit.imeOptions = EditorInfo.IME_ACTION_DONE
                    priceEdit.inputType = InputType.TYPE_CLASS_NUMBER

                    priceEdit.setOnFocusChangeListener { _, hasFocus ->
                        if (!hasFocus) {
                            val newPrice = priceEdit.getRawValue()
                            tempPrices[adapterPosition] = newPrice
                            onPriceChanged(item, newPrice)
                        } else {
                            onItemFocused(absoluteAdapterPosition)
                        }
                    }

                    priceEdit.setOnEditorActionListener { _, actionId, _ ->
                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            val newPrice = priceEdit.getRawValue()
                            tempPrices[adapterPosition] = newPrice
                            onPriceChanged(item, newPrice)
                            priceEdit.clearFocus()
                            true
                        } else {
                            false
                        }
                    }

                    btnPlus.setOnClickListener {
                        try {
                            if (!isClickValid()) return@setOnClickListener

                            val newQuantity = item.quantity + 1
                            onQuantityChanged(item, newQuantity)
                            binding.quantityText.text = newQuantity.toString()

                            // Dapatkan posisi unit yang sedang dipilih di spinner
                            val position = unitsSpinner.selectedItemPosition

                            // Pastikan posisi valid
                            if (position != -1 && position < item.productPrice.size) {
                                val selectedPrice = item.productPrice[position]

                                // Update tampilan lowest unit quantity
                                binding.lowestUnitQuantity.text =
                                    "(${(selectedPrice.quantityPerUnit.toInt() * newQuantity)} ${item.product?.data?.product?.lowestUnit})"
                            }
                        } catch (e: Exception) {
                            Log.e("CartAdapter", "Error increasing quantity", e)
                        }
                    }

                    btnMinus.setOnClickListener {
                        try {
                            if (!isClickValid()) return@setOnClickListener

                            if (item.quantity > 1) {
                                val newQuantity = item.quantity - 1
                                onQuantityChanged(item, newQuantity)
                                binding.quantityText.text = newQuantity.toString()

                                // Dapatkan posisi unit yang sedang dipilih di spinner
                                val position = unitsSpinner.selectedItemPosition

                                // Pastikan posisi valid
                                if (position != -1 && position < item.productPrice.size) {
                                    val selectedPrice = item.productPrice[position]

                                    // Update tampilan lowest unit quantity
                                    binding.lowestUnitQuantity.text =
                                        "(${(selectedPrice.quantityPerUnit.toInt() * newQuantity)} ${item.product?.data?.product?.lowestUnit})"
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("CartAdapter", "Error decreasing quantity", e)
                        }
                    }

                    unitsSpinner.onItemSelectedListener =
                        object : AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(
                                parent: AdapterView<*>?,
                                view: View?,
                                position: Int,
                                id: Long
                            ) {
                                try {
                                    val selectedPrice = item.productPrice[position]
                                    if (selectedPrice != item.selectedPrice) {
                                        onUnitChanged(item, selectedPrice)

                                        // Update tempPrices dan EditText dengan harga unit baru
                                        tempPrices[adapterPosition] = selectedPrice.price
                                        binding.lowestUnitQuantity.text =
                                            "(${(selectedPrice.quantityPerUnit.toInt() * item.quantity)} ${item.product?.data?.product?.lowestUnit})" //product lowest unit

                                        // Format dan set harga baru ke EditText
                                        val symbols =
                                            DecimalFormatSymbols(Locale.getDefault()).apply {
                                                groupingSeparator = '.'
                                            }
                                        val formatter = DecimalFormat("#,###", symbols)
                                        val formattedPrice = formatter.format(selectedPrice.price)
                                        priceEdit.setText(formattedPrice)
                                    }
                                } catch (e: Exception) {
                                    Log.e("CartAdapter", "Error changing unit", e)
                                }
                            }

                            override fun onNothingSelected(parent: AdapterView<*>?) {}
                        }
                }
            } catch (e: Exception) {
                Log.e("CartAdapter", "Error binding view holder", e)
            }
        }
    }

    class CartDiffCallback : DiffUtil.ItemCallback<CartItem>() {
        override fun areItemsTheSame(oldItem: CartItem, newItem: CartItem): Boolean {
            return oldItem.product?.data?.product?.id == newItem.product?.data?.product?.id
        }

        override fun areContentsTheSame(oldItem: CartItem, newItem: CartItem): Boolean {
            return oldItem == newItem
        }
    }

    companion object {
        fun createSwipeToDelete(adapter: CartAdapter): ItemTouchHelper.SimpleCallback {
            return object : ItemTouchHelper.SimpleCallback(
                0,
                ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
            ) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean = false

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    try {
                        val position = viewHolder.adapterPosition
                        val item = adapter.items[position]
                        adapter.onDeleteItem(item)
                    } catch (e: Exception) {
                        Log.e("CartAdapter", "Error deleting item", e)
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val binding = CartItemLayoutBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CartViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size
}