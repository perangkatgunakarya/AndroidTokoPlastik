package com.example.tokoplastik.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.tokoplastik.data.responses.Customer
import com.example.tokoplastik.databinding.CustomerListLayoutBinding

class CustomerAdapter () : RecyclerView.Adapter<CustomerAdapter.ViewHolder>() {

    private var originalList = mutableListOf<Customer>()
    private var filteredList = mutableListOf<Customer>()
    private var onItemClickListener: ((Customer) -> Unit)? = null

    private fun getInitials(customerName: String): String {
        return customerName.split(" ")
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercase() }
            .joinToString("")
    }

    fun setOnItemClickListener(listener: (Customer) -> Unit) {
        onItemClickListener = listener
    }

    fun updateList(newCustomers: List<Customer>) {
        originalList.clear()
        originalList.addAll(newCustomers)
        filteredList.clear()
        filteredList.addAll(newCustomers)
        notifyDataSetChanged()
    }

    fun removeItem(customer: Customer) {
        val originalIndex = originalList.indexOfFirst { it == customer }
        val filteredIndex = filteredList.indexOfFirst { it == customer }

        if (originalIndex != -1) {
            originalList.removeAt(originalIndex)
        }
        if (filteredIndex != -1) {
            filteredList.removeAt(filteredIndex)
            notifyItemRemoved(filteredIndex)
        }
    }

    fun filterCustomers(query: String) {
        filteredList = if (query.isEmpty()) {
            originalList.toMutableList()
        } else {
            originalList.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.address.contains(query, ignoreCase = true)
            }.toMutableList()
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
       val binding = CustomerListLayoutBinding.inflate(
           LayoutInflater.from(parent.context), parent, false
       )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentCustomer = filteredList[position]
        holder.bind(currentCustomer)
    }

    override fun getItemCount() = filteredList.size

    fun sortByName(ascending: Boolean = true) {
        filteredList = if (ascending) {
            filteredList.sortedBy { it.name }.toMutableList()
        } else {
            filteredList.sortedByDescending { it.name }
        }.toMutableList()
    }

    inner class ViewHolder(private val binding: CustomerListLayoutBinding) :
            RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClickListener?.invoke(filteredList[position])
                }
            }
        }

        fun bind(customer: Customer) {
            binding.apply {
                val initial = getInitials(customer.name)
                customerInitial.text = initial
                customerNameText.text = customer.name
                addressText.text = customer.address
                phoneText.text = customer.phone
            }
        }
    }
}