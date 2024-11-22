package com.example.tokoplastik.adapter

import android.R
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.tokoplastik.data.responses.CartItem
import com.example.tokoplastik.data.responses.ProductPrice
import com.example.tokoplastik.databinding.CartItemLayoutBinding

class CartAdapter(
    private val onQuantityChanged: (CartItem, Int) -> Unit,
    private val onPriceChanged: (CartItem, Int) -> Unit,
    private val onUnitChanged: (CartItem, ProductPrice) -> Unit,
    private val onDeleteItem: (CartItem) -> Unit,
    private val onItemFocused: (Int) -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {
    private var items = mutableListOf<CartItem>()
    private var tempPrices = mutableMapOf<Int, Int>()

    fun updateItems(newItems: List<CartItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    inner class CartViewHolder(private val binding: CartItemLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CartItem) {
            binding.apply {
                productText.text = item.product?.data?.name

                val unitsAdapter = ArrayAdapter(
                    itemView.context,
                    android.R.layout.simple_spinner_item,
                    item.productPrice.map { it.unit }
                )
                unitsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                unitsSpinner.adapter = unitsAdapter

                binding.quantityText.text = item.quantity.toString()
                val initialUnit = item.selectedPrice.unit
                val position = item.productPrice.indexOfFirst { it.unit == initialUnit }
                if (position != -1) {
                    unitsSpinner.setSelection(position)
                }

                if (!priceEdit.hasFocus()) {
                    val price = tempPrices[adapterPosition] ?: item.customPrice
                    priceEdit.setText(price.toString())
                } else {
                    onItemFocused(absoluteAdapterPosition)
                }

                priceEdit.imeOptions = EditorInfo.IME_ACTION_DONE
                priceEdit.inputType = InputType.TYPE_CLASS_NUMBER

                priceEdit.setOnEditorActionListener { _, actionId, _ ->
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        tempPrices[adapterPosition]?.let { price ->
                            onPriceChanged(item, price)
                            tempPrices.remove(adapterPosition)
                        }
                        priceEdit.clearFocus()
                        true
                    } else {
                        false
                    }
                }

                priceEdit.doOnTextChanged { text, _, _, _ ->
                    if (priceEdit.hasFocus()) {
                        text?.toString()?.toIntOrNull()?.let { price ->
                            tempPrices[adapterPosition] = price
                        }
                    }
                }

                btnPlus.setOnClickListener {
                    onQuantityChanged(item, item.quantity + 1)
                    binding.quantityText.text = (item.quantity + 1).toString()
                }

                btnMinus.setOnClickListener {
                    if (item.quantity > 1) {
                        onQuantityChanged(item, item.quantity - 1)
                        binding.quantityText.text = (item.quantity - 1).toString()
                    }
                }

                unitsSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        val selectedPrice = item.productPrice[position]
                        if (selectedPrice != item.selectedPrice) {
                            onUnitChanged(item, selectedPrice)
                        }
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            }
        }
    }

    companion object {
        fun createSwipeToDelete(adapter: CartAdapter): ItemTouchHelper.SimpleCallback {
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
                    val item = adapter.items[position]
                    adapter.onDeleteItem(item)
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