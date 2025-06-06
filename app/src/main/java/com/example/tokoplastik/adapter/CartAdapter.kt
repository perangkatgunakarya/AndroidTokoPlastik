package com.example.tokoplastik.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

    private val priceFormatter by lazy {
        DecimalFormat("#,###", DecimalFormatSymbols(Locale.getDefault()).apply {
            groupingSeparator = '.'
        })
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

        private var quantityTextWatcher: TextWatcher? = null
        private var priceTextWatcher: TextWatcher? = null

        init {
            binding.priceEdit.setNumberFormatter()

            binding.btnPlus.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = getItem(position)
                    onQuantityChanged(item, item.quantity + 1)
                }
            }

            binding.btnMinus.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = getItem(position)
                    if (item.quantity > 1) {
                        onQuantityChanged(item, item.quantity - 1)
                    }
                }
            }

            binding.priceEdit.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val currentQuantity = binding.quantityText.text.toString().toIntOrNull() ?: 0
                        // Jika kuantitas 0 atau kurang, set kembali ke 1
                        if (currentQuantity <= 0) {
                            onQuantityChanged(getItem(position), 1)
                        }
                    }
                }
            }

            quantityTextWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION && binding.quantityText.hasFocus()) {
                        val newQuantity = s.toString().toIntOrNull() ?: 1
                        onQuantityChanged(getItem(position), newQuantity)
                    }
                }
                override fun afterTextChanged(s: Editable?) {}
            }
            binding.quantityText.addTextChangedListener(quantityTextWatcher)

            priceTextWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION && binding.priceEdit.hasFocus()) {
                        val newPrice = binding.priceEdit.getRawValue()
                        onPriceChanged(getItem(position), newPrice)
                    }
                }
                override fun afterTextChanged(s: Editable?) {}
            }
            binding.priceEdit.addTextChangedListener(priceTextWatcher)

            binding.unitsSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val adapterPosition = bindingAdapterPosition
                    if (adapterPosition == RecyclerView.NO_POSITION) return

                    val item = getItem(adapterPosition)
                    val selectedPrice = item.productPrice[position]

                    if (selectedPrice.unit != item.selectedPrice.unit) {
                        onUnitChanged(item, selectedPrice)
                        binding.priceEdit.setText(priceFormatter.format(selectedPrice.price))
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }

        fun bind(item: CartItem) {
            binding.apply {
                cartNumber.text = "${bindingAdapterPosition + 1}."
                productImage.text = getInitials(item.product?.data?.product?.name)
                productText.text = item.product?.data?.product?.name
                supplierText.text = item.product?.data?.product?.supplier

                val formattedCapitalPrice = priceFormatter.format(item.product?.data?.product?.newestCapitalPrice ?: 0)
                modalPrice.text = "Modal : Rp$formattedCapitalPrice"

                val units = item.productPrice.map { it.unit }
                val unitsAdapter = ArrayAdapter(itemView.context, android.R.layout.simple_spinner_item, units)
                unitsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                unitsSpinner.adapter = unitsAdapter

                val currentUnitPosition = item.productPrice.indexOfFirst { it.unit == item.selectedPrice.unit }
                if (currentUnitPosition != -1) {
                    unitsSpinner.setSelection(currentUnitPosition, false)
                }

                quantityText.removeTextChangedListener(quantityTextWatcher)
                quantityText.setText(item.quantity.toString())
                quantityText.setSelection(quantityText.text.length)
                quantityText.addTextChangedListener(quantityTextWatcher)

                priceEdit.removeTextChangedListener(priceTextWatcher)
                priceEdit.setText(priceFormatter.format(item.customPrice))
                priceEdit.addTextChangedListener(priceTextWatcher)

                updateLowestUnitDisplay(item)
                updateTotalPriceDisplay(item)
            }
        }

        private fun updateLowestUnitDisplay(item: CartItem) {
            val selectedUnitInfo = item.selectedPrice
            val totalLowestUnit = item.quantity * (selectedUnitInfo.quantityPerUnit.toIntOrNull() ?: 1)
            binding.lowestUnitQuantity.text = "(${totalLowestUnit} ${item.product?.data?.product?.lowestUnit})"
        }

        private fun updateTotalPriceDisplay(item: CartItem) {
            val total = item.quantity * item.customPrice
            binding.totalPrice.text = priceFormatter.format(total)
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
                    val position = viewHolder.bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        adapter.onDeleteItem(adapter.getItem(position))
                    }
                }
            }
        }
    }
}