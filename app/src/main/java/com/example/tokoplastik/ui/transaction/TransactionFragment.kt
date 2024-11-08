package com.example.tokoplastik.ui.transaction

import android.content.RestrictionEntry.TYPE_NULL
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.example.tokoplastik.R
import com.example.tokoplastik.data.network.CustomerApi
import com.example.tokoplastik.data.repository.CustomerRepository
import com.example.tokoplastik.data.responses.Customer
import com.example.tokoplastik.databinding.FragmentTransactionBinding
import com.example.tokoplastik.ui.base.BaseFragment
import com.example.tokoplastik.util.enable
import com.example.tokoplastik.viewmodel.TransactionViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class TransactionFragment : BaseFragment<TransactionViewModel, FragmentTransactionBinding, CustomerRepository>() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val nameEditText = binding.customerNameText
        val addressEditText = binding.customerAddressText
        val hpEditText = binding.customerHpText

        nameEditText.enable(false)
        addressEditText.enable(false)
        hpEditText.enable(false)

        nameEditText.isFocusable = false
        addressEditText.isFocusable = false
        hpEditText.isFocusable = false

        nameEditText.inputType = TYPE_NULL
        addressEditText.inputType = TYPE_NULL
        hpEditText.inputType = TYPE_NULL

        setupCustomerAutocomplete()
    }

    private fun setupCustomerAutocomplete() {
        viewModel.getTransaction()
        viewModel.transaction.observe(viewLifecycleOwner, Observer { result ->
            val customerNames = result.data?.data?.map { it.name } ?: emptyList()
            val customerAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                customerNames
            )

            binding.customerDropdown.setAdapter(customerAdapter)
            binding.customerDropdown.threshold = 1

            binding.customerDropdown.setOnItemClickListener { _, _, position, _ ->
                val selectedCustomerName = customerAdapter.getItem(position)
                val selectedCustomer = result.data?.data?.find { it.name == selectedCustomerName }
                populateCustomerData(selectedCustomer)
            }
        })
    }

    private fun populateCustomerData(customer: Customer?) {
        customer?.let {
            binding.customerNameText.setText(it.name)
            binding.customerAddressText.setText(it.address)
            binding.customerHpText.setText(it.phone)
        }
    }

    override fun getViewModel() = TransactionViewModel::class.java

    override fun getFragmentBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentTransactionBinding.inflate(inflater, container, false)

    override fun getFragmentRepository() : CustomerRepository {
        val token = runBlocking { userPreferences.authToken.first() }
        val api = remoteDataSource.buildApi(CustomerApi::class.java, token)
        return CustomerRepository(api)
    }
}