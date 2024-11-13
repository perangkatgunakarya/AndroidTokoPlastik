package com.example.tokoplastik.adapter

import android.R
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.tokoplastik.data.responses.CartItem
import com.example.tokoplastik.data.responses.ProductPrice
import com.example.tokoplastik.databinding.CartItemLayoutBinding

class CartAdapter(
    private val onQuantityChanged: (CartItem, Int) -> Unit,
    private val onPriceChanged: (CartItem, Int) -> Unit,
    private val onUnitChanged: (CartItem, ProductPrice) -> Unit,
    private val onDeleteItem: (CartItem) -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {
    private var items = mutableListOf<CartItem>()

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

                // Setup unit spinner
                val unitsAdapter = ArrayAdapter(
                    itemView.context,
                    android.R.layout.simple_spinner_item,
                    item.productPrice.map { it.unit }
                )
                unitsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                unitsSpinner.adapter = unitsAdapter

                // Set initial unit selection
                binding.quantityText.text = item.quantity.toString()
                val initialUnit = item.selectedPrice.unit
                val position = item.productPrice.indexOfFirst { it.unit == initialUnit }
                if (position != -1) {
                    unitsSpinner.setSelection(position)
                }

                // Setup price EditText
                priceEdit.setText(item.customPrice.toString())

                // Quantity controls
                btnPlus.setOnClickListener {
                    onQuantityChanged(item, item.quantity + 1)
                    binding.quantityText.text = item.quantity.toString()
                }

                btnMinus.setOnClickListener {
                    if (item.quantity > 1) {
                        onQuantityChanged(item, item.quantity - 1)
                        binding.quantityText.text = item.quantity.toString()
                    }
                }

                // Delete item
//                btnDelete.setOnClickListener {
//                    onDeleteItem(item)
//                }

                // Unit selection listener
                unitsSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        val selectedPrice = item.productPrice[position]
                        if (selectedPrice != item.selectedPrice) {
                            onUnitChanged(item, selectedPrice)
                        }
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }

                // Price edit listener
                priceEdit.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable?) {
                        s?.toString()?.toIntOrNull()?.let { price ->
                            if (price != item.customPrice) {
                                onPriceChanged(item, price)
                            }
                        }
                    }
                })
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