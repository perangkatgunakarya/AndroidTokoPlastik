package com.example.tokoplastik.adapter

import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.tokoplastik.data.responses.CartItem
import com.example.tokoplastik.data.responses.ProductPrice
import com.example.tokoplastik.databinding.CartItemLayoutBinding
import com.example.tokoplastik.util.getRawValue
import com.example.tokoplastik.util.setNumberFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class CartAdapter(
    private val onQuantityChanged: (CartItem, Int) -> Unit,
    private val onPriceChanged: (CartItem, Int) -> Unit,
    private val onUnitChanged: (CartItem, ProductPrice) -> Unit,
    private val onDeleteItem: (CartItem) -> Unit,
    private val onItemFocused: (Int) -> Unit
) : ListAdapter<CartItem, CartAdapter.CartViewHolder>(CartDiffCallback()) {

    // Temporary storage for prices to prevent UI flickers
    private val tempPrices = mutableMapOf<Int, Int>()

    // Better coroutine management with a cancelable job
    private val adapterJob = Job()
    private val adapterScope = CoroutineScope(Dispatchers.Main + adapterJob)

    // Click debounce control
    private var lastClickTime = 0L
    private val CLICK_TIME_INTERVAL = 200L  // Increased to prevent rapid clicks

    // Reusable formatter to minimize object creation
    private val priceFormatter by lazy {
        DecimalFormat("#,###", DecimalFormatSymbols(Locale.getDefault()).apply {
            groupingSeparator = '.'
        })
    }

    // Clean up resources when adapter is detached
    fun onDetached() {
        adapterScope.cancel()  // Cancel all coroutines when the adapter is no longer needed
    }

    private fun isClickValid(): Boolean {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime < CLICK_TIME_INTERVAL) {
            return false
        }
        lastClickTime = currentTime
        return true
    }

    private fun getInitials(productName: String?): String {
        return productName?.split(" ")
            ?.take(2)
            ?.mapNotNull { it.firstOrNull()?.uppercase() }
            ?.joinToString("") ?: ""
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val binding = CartItemLayoutBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CartViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CartViewHolder(private val binding: CartItemLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        // Debounce jobs for text changes
        private var quantityTextJob: Job? = null
        private var priceTextJob: Job? = null

        init {
            // Set up text formatters once
            binding.priceEdit.setNumberFormatter()

            // Set up price edit text listener once
            binding.priceEdit.apply {
                imeOptions = EditorInfo.IME_ACTION_DONE
                inputType = InputType.TYPE_CLASS_NUMBER

                setOnFocusChangeListener { _, hasFocus ->
                    if (!hasFocus) {
                        val position = bindingAdapterPosition
                        if (position != RecyclerView.NO_POSITION) {
                            val item = getItem(position)
                            val newPrice = getRawValue()
                            tempPrices[position] = newPrice
                            onPriceChanged(item, newPrice)
                        }
                    } else {
                        onItemFocused(bindingAdapterPosition)
                    }
                }

                setOnEditorActionListener { _, actionId, _ ->
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        val position = bindingAdapterPosition
                        if (position != RecyclerView.NO_POSITION) {
                            val item = getItem(position)
                            val newPrice = getRawValue()
                            tempPrices[position] = newPrice
                            onPriceChanged(item, newPrice)
                            clearFocus()
                        }
                        true
                    } else {
                        false
                    }
                }
            }

            // Set up quantity buttons once
            binding.btnPlus.setOnClickListener {
                if (!isClickValid()) return@setOnClickListener

                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    try {
                        val item = getItem(position)
                        val currentQuantity = binding.quantityText.text.toString().toIntOrNull() ?: 1
                        val newQuantity = currentQuantity + 1

                        // Update the text first
                        binding.quantityText.setText(newQuantity.toString())
                        binding.quantityText.setSelection(binding.quantityText.text.length)

                        // Then update the data model
                        updateQuantity(item, newQuantity)
                    } catch (e: Exception) {
                        Log.e("CartAdapter", "Error increasing quantity", e)
                    }
                }
            }

            binding.btnMinus.setOnClickListener {
                if (!isClickValid()) return@setOnClickListener

                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    try {
                        val item = getItem(position)
                        val currentQuantity = binding.quantityText.text.toString().toIntOrNull() ?: 1

                        if (currentQuantity > 1) {
                            val newQuantity = currentQuantity - 1

                            // Update the text first
                            binding.quantityText.setText(newQuantity.toString())
                            binding.quantityText.setSelection(binding.quantityText.text.length)

                            // Then update the data model
                            updateQuantity(item, newQuantity)
                        }
                    } catch (e: Exception) {
                        Log.e("CartAdapter", "Error decreasing quantity", e)
                    }
                }
            }
        }

        fun bind(item: CartItem) {
            try {
                binding.apply {
                    // Set basic product info
                    productImage.text = getInitials(item.product?.data?.product?.name)
                    productText.text = item.product?.data?.product?.name
                    supplierText.text = item.product?.data?.product?.supplier

                    // Set modal price
                    val formattedCapitalPrice = priceFormatter.format(item.product?.data?.product?.newestCapitalPrice ?: 0)
                    modalPrice.text = "Modal : Rp$formattedCapitalPrice"

                    // Set up unit spinner
                    setupUnitSpinner(item)

                    // Set quantity text without adding a new TextWatcher
                    removeQuantityTextWatcher()
                    quantityText.setText(item.quantity.toString())
                    setupQuantityTextWatcher(item)

                    // Set price text
                    if (!priceEdit.hasFocus()) {
                        val price = tempPrices[bindingAdapterPosition] ?: item.customPrice
                        priceEdit.setText(priceFormatter.format(price))
                    }

                    removePriceTextWatcher()
                    setupPriceTextWatcher(item)

                    // Update total price display
                    updateTotalPriceDisplay(item)

                    // Update lowest unit quantity display
                    updateLowestUnitDisplay(item)
                }
            } catch (e: Exception) {
                Log.e("CartAdapter", "Error binding view holder", e)
            }
        }

        // Text watchers references to be able to remove them
        private var quantityTextWatcher: TextWatcher? = null
        private var priceTextWatcher: TextWatcher? = null

        private fun removeQuantityTextWatcher() {
            quantityTextWatcher?.let { binding.quantityText.removeTextChangedListener(it) }
        }

        private fun removePriceTextWatcher() {
            priceTextWatcher?.let { binding.priceEdit.removeTextChangedListener(it) }
        }

        private fun setupQuantityTextWatcher(item: CartItem) {
            quantityTextWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    // Cancel previous job if it exists
                    quantityTextJob?.cancel()

                    val newQuantityString = s.toString()

                    // Skip processing if empty
                    if (newQuantityString.isEmpty()) {
                        updateQuantity(item, 0)
                        binding.lowestUnitQuantity.text = ""
                        binding.totalPrice.text = "Rp0"
                        return
                    }

                    // Trim leading zeros
                    val trimmedQuantityString = newQuantityString.trimStart('0')
                    if (newQuantityString != trimmedQuantityString) {
                        binding.quantityText.setText(trimmedQuantityString)
                        binding.quantityText.setSelection(trimmedQuantityString.length)
                        return
                    }

                    // Process with debounce
                    quantityTextJob = adapterScope.launch {
                        delay(150) // Small debounce to prevent rapid changes
                        try {
                            if (trimmedQuantityString.isNotEmpty()) {
                                val newQuantity = trimmedQuantityString.toInt()
                                if (newQuantity >= 1) {
                                    updateQuantity(item, newQuantity)
                                } else {
                                    binding.quantityText.text.clear()
                                    updateQuantity(item, 0)
                                }
                            }
                        } catch (e: NumberFormatException) {
                            binding.quantityText.text.clear()
                            updateQuantity(item, 0)
                        }
                    }
                }

                override fun afterTextChanged(s: Editable?) {}
            }

            binding.quantityText.addTextChangedListener(quantityTextWatcher)
        }

        private fun setupPriceTextWatcher(item: CartItem) {
            priceTextWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (binding.priceEdit.hasFocus()) {
                        // Cancel previous job if it exists
                        priceTextJob?.cancel()

                        priceTextJob = adapterScope.launch {
                            delay(200) // Debounce price changes
                            try {
                                val newPrice = binding.priceEdit.getRawValue()
                                tempPrices[bindingAdapterPosition] = newPrice
                                onPriceChanged(item, newPrice)
                                updateTotalPriceDisplay(item)
                            } catch (e: NumberFormatException) {
                                Log.e("CartAdapter", "Error parsing price", e)
                            }
                        }
                    }
                }

                override fun afterTextChanged(s: Editable?) {}
            }

            binding.priceEdit.addTextChangedListener(priceTextWatcher)
        }

        private fun setupUnitSpinner(item: CartItem) {
            val units = item.productPrice.map { it.unit }
            val unitsAdapter = ArrayAdapter(
                itemView.context,
                android.R.layout.simple_spinner_item,
                units
            )
            unitsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

            // Remember current selection to prevent unnecessary updates
            val currentSelection = binding.unitsSpinner.selectedItemPosition
            val targetSelection = item.productPrice.indexOfFirst { it.unit == item.selectedPrice.unit }

            binding.unitsSpinner.adapter = unitsAdapter

            // Only set selection if different to prevent callbacks
            if (targetSelection != currentSelection && targetSelection >= 0) {
                binding.unitsSpinner.setSelection(targetSelection)
            }

            // Remove existing listener to prevent duplicates
            if (binding.unitsSpinner.onItemSelectedListener != null) {
                binding.unitsSpinner.onItemSelectedListener = null
            }

            binding.unitsSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                    try {
                        if (position >= 0 && position < item.productPrice.size) {
                            val selectedPrice = item.productPrice[position]
                            if (selectedPrice != item.selectedPrice) {
                                onUnitChanged(item, selectedPrice)

                                // Update price in map and UI
                                tempPrices[bindingAdapterPosition] = selectedPrice.price

                                // Update related UI elements
                                updateLowestUnitDisplay(item)

                                // Format and set price
                                binding.priceEdit.setText(priceFormatter.format(selectedPrice.price))

                                // Update total price
                                updateTotalPriceDisplay(item)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("CartAdapter", "Error changing unit", e)
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }

        private fun updateQuantity(item: CartItem, newQuantity: Int) {
            onQuantityChanged(item, newQuantity)
            updateLowestUnitDisplay(item)
            updateTotalPriceDisplay(item)
        }

        private fun updateLowestUnitDisplay(item: CartItem) {
            try {
                val position = binding.unitsSpinner.selectedItemPosition
                if (position != -1 && position < item.productPrice.size) {
                    val selectedPrice = item.productPrice[position]
                    val quantity = binding.quantityText.text.toString().toIntOrNull() ?: 0
                    binding.lowestUnitQuantity.text =
                        "(${quantity} x ${selectedPrice.quantityPerUnit.toInt()} ${item.product?.data?.product?.lowestUnit})"
                }
            } catch (e: Exception) {
                Log.e("CartAdapter", "Error updating lowest unit display", e)
                binding.lowestUnitQuantity.text = ""
            }
        }

        private fun updateTotalPriceDisplay(item: CartItem) {
            try {
                val quantity = binding.quantityText.text.toString().toIntOrNull() ?: 0
                val price = tempPrices[bindingAdapterPosition] ?: item.customPrice
                binding.totalPrice.text = "Rp${priceFormatter.format(quantity * price)}"
            } catch (e: Exception) {
                Log.e("CartAdapter", "Error updating total price display", e)
                binding.totalPrice.text = "Rp0"
            }
        }
    }

    class CartDiffCallback : DiffUtil.ItemCallback<CartItem>() {
        override fun areItemsTheSame(oldItem: CartItem, newItem: CartItem): Boolean {
            return oldItem.product?.data?.product?.id == newItem.product?.data?.product?.id
        }

        override fun areContentsTheSame(oldItem: CartItem, newItem: CartItem): Boolean {
            return oldItem.quantity == newItem.quantity &&
                    oldItem.customPrice == newItem.customPrice &&
                    oldItem.selectedPrice.unit == newItem.selectedPrice.unit
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
                        val position = viewHolder.bindingAdapterPosition
                        if (position != RecyclerView.NO_POSITION) {
                            val item = adapter.getItem(position)
                            adapter.onDeleteItem(item)
                        }
                    } catch (e: Exception) {
                        Log.e("CartAdapter", "Error deleting item", e)
                    }
                }
            }
        }
    }
}