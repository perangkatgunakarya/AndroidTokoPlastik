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
import androidx.compose.ui.semantics.text
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
                        "(${item.quantity} x ${(selectedPrice.quantityPerUnit)} ${item.product?.data?.product?.lowestUnit})"


                    quantityText.setText(item.quantity.toString())
                    quantityText.addTextChangedListener(object : TextWatcher {
                        override fun beforeTextChanged(
                            s: CharSequence?,
                            start: Int,
                            count: Int,
                            after: Int
                        ) {
                        }

                        override fun onTextChanged(
                            s: CharSequence?,
                            start: Int,
                            before: Int,
                            count: Int
                        ) {
                            val newQuantityString = s.toString()

                            // Menghilangkan angka 0 di depan
                            val trimmedQuantityString = newQuantityString.trimStart('0')

                            if (newQuantityString != trimmedQuantityString) {
                                // Jika ada angka 0 di depan, update EditText
                                quantityText.setText(trimmedQuantityString)
                            }

                            if (trimmedQuantityString.isNotEmpty()) {
                                try {
                                    val newQuantity = trimmedQuantityString.toInt()
                                    // Memastikan kuantitas tidak kurang dari 1
                                    if (newQuantity >= 1) {
                                        // Memanggil onQuantityChanged untuk memperbarui data
                                        onQuantityChanged(item, newQuantity)

                                        // Dapatkan posisi unit yang sedang dipilih di spinner
                                        val position = unitsSpinner.selectedItemPosition

                                        // Pastikan posisi valid
                                        if (position != -1 && position < item.productPrice.size) {
                                            val selectedPrice = item.productPrice[position]

                                            // Update tampilan lowest unit quantity
                                            binding.lowestUnitQuantity.text =
                                                "(${newQuantity} x ${selectedPrice.quantityPerUnit.toInt()} ${item.product?.data?.product?.lowestUnit})"
                                        }

                                        // Tambahkan update untuk totalPrice
                                        val currentPrice = tempPrices[adapterPosition] ?: item.customPrice
                                        val symbols = DecimalFormatSymbols(Locale.getDefault()).apply {
                                            groupingSeparator = '.'
                                        }
                                        val formatter = DecimalFormat("#,###", symbols)
                                        totalPrice.text = "Rp${formatter.format(newQuantity * currentPrice)}"
                                    } else {
                                        // Jika kuantitas kurang dari 1, bersihkan teks
                                        quantityText.text.clear()
                                        onQuantityChanged(item, 0)

                                        // Reset tampilan lowest unit quantity
                                        binding.lowestUnitQuantity.text = ""

                                        // Reset total price
                                        totalPrice.text = "Rp0"
                                    }
                                } catch (e: NumberFormatException) {
                                    // Jika input bukan angka, bersihkan teks
                                    quantityText.text.clear()
                                    onQuantityChanged(item, 0)

                                    // Reset tampilan lowest unit quantity
                                    binding.lowestUnitQuantity.text = ""

                                    // Reset total price
                                    totalPrice.text = "Rp0"
                                }
                            } else {
                                // Teks kosong, reset ke kondisi awal
                                onQuantityChanged(item, 0)
                                binding.lowestUnitQuantity.text = ""

                                // Reset total price
                                totalPrice.text = "Rp0"
                            }
                        }

                        override fun afterTextChanged(s: Editable?) {}
                    })

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

                    binding.priceEdit.addTextChangedListener(object : TextWatcher {
                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                            // Tidak ada perubahan
                        }

                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                            if (binding.priceEdit.hasFocus()) {
                                try {
                                    // Gunakan getRawValue() untuk mendapatkan nilai tanpa pemisah ribuan
                                    val newPrice = binding.priceEdit.getRawValue()
                                    tempPrices[adapterPosition] = newPrice
                                    onPriceChanged(item, newPrice)

                                    // Update totalPrice setiap kali harga berubah
                                    val currentQuantity = quantityText.text.toString().toIntOrNull() ?: 0
                                    val symbols = DecimalFormatSymbols(Locale.getDefault()).apply {
                                        groupingSeparator = '.'
                                    }
                                    val formatter = DecimalFormat("#,###", symbols)
                                    totalPrice.text = "Rp${formatter.format(currentQuantity * newPrice)}"
                                } catch (e: NumberFormatException) {
                                    // Log error tanpa mengubah nilai
                                    Log.e("CartAdapter", "Error parsing price", e)
                                }
                            }
                        }

                        override fun afterTextChanged(s: Editable?) {
                            // Tidak ada perubahan
                        }
                    })

                    totalPrice.text = "Rp${formatter.format(quantityText.text.toString().toInt() * item.customPrice)}"

                    btnPlus.setOnClickListener {
                        try {
                            if (!isClickValid()) return@setOnClickListener

                            val currentQuantity = quantityText.text.toString().toIntOrNull() ?: 1
                            val newQuantity = currentQuantity + 1
                            onQuantityChanged(item, newQuantity)
                            // Perbarui quantityText di sini
                            quantityText.setText(newQuantity.toString())
                            quantityText.setSelection(quantityText.text.length) // Pindahkan kursor ke akhir teks

                            // Dapatkan posisi unit yang sedang dipilih di spinner
                            val position = unitsSpinner.selectedItemPosition

                            // Pastikan posisi valid
                            if (position != -1 && position < item.productPrice.size) {
                                val selectedPrice = item.productPrice[position]

                                // Update tampilan lowest unit quantity
                                binding.lowestUnitQuantity.text =
                                    "(${newQuantity} x ${selectedPrice.quantityPerUnit.toInt()} ${item.product?.data?.product?.lowestUnit})"
                            }

                            totalPrice.text = "Rp${formatter.format(newQuantity * item.customPrice)}"
                        } catch (e: Exception) {
                            Log.e("CartAdapter", "Error increasing quantity", e)
                        }
                    }

                    btnMinus.setOnClickListener {
                        try {
                            if (!isClickValid()) return@setOnClickListener

                            val currentQuantity = quantityText.text.toString().toIntOrNull() ?: 1
                            if (currentQuantity > 1) {
                                val newQuantity = currentQuantity - 1
                                onQuantityChanged(item, newQuantity)
                                // Perbarui quantityText di sini
                                quantityText.setText(newQuantity.toString())
                                quantityText.setSelection(quantityText.text.length) // Pindahkan kursor ke akhir teks

                                // Dapatkan posisi unit yang sedang dipilih di spinner
                                val position = unitsSpinner.selectedItemPosition

                                // Pastikan posisi valid
                                if (position != -1 && position < item.productPrice.size) {
                                    val selectedPrice = item.productPrice[position]

                                    // Update tampilan lowest unit quantity
                                    binding.lowestUnitQuantity.text =
                                        "(${newQuantity} x ${selectedPrice.quantityPerUnit.toInt()} ${item.product?.data?.product?.lowestUnit})"
                                }

                                totalPrice.text = "Rp${formatter.format(newQuantity * item.customPrice)}"
                            }
                        } catch (e: Exception) {
                            Log.e("CartAdapter", "Error decreasing quantity", e)
                        }
                    }

                    unitsSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
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
                                        "(${item.quantity} x ${selectedPrice.quantityPerUnit.toInt()} ${item.product?.data?.product?.lowestUnit})" //product lowest unit

                                    // Format dan set harga baru ke EditText
                                    val symbols = DecimalFormatSymbols(Locale.getDefault()).apply {
                                        groupingSeparator = '.'
                                    }
                                    val formatter = DecimalFormat("#,###", symbols)
                                    val formattedPrice = formatter.format(selectedPrice.price)
                                    priceEdit.setText(formattedPrice)

                                    // Update totalPrice dengan harga unit baru
                                    val currentQuantity = quantityText.text.toString().toIntOrNull() ?: 0
                                    totalPrice.text = "Rp${formatter.format(currentQuantity * selectedPrice.price)}"
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