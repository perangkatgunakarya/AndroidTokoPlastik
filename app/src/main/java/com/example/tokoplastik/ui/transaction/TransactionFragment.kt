package com.example.tokoplastik.ui.transaction

import android.content.RestrictionEntry.TYPE_NULL
import android.os.Bundle
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

        setupCustomerSpinner()
    }

    private fun setupCustomerSpinner() {
        viewModel.getTransaction()
        viewModel.transaction.observe(viewLifecycleOwner, Observer { result ->
            val customerAdapter = object : ArrayAdapter<Customer>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                result.data?.data ?: emptyList()
            ) {
                override fun getFilter(): Filter {
                    return object : Filter() {
                        override fun performFiltering(constraint: CharSequence?): FilterResults {
                            val filteredResults = if (constraint.isNullOrEmpty()) {
                                result.data?.data ?: emptyList()
                            } else {
                                result.data?.data?.filter {
                                    it.name.contains(constraint.toString(), ignoreCase = true)
                                }
                            }

                            val results = FilterResults()
                            results.values = filteredResults
                            return results
                        }

                        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                            (results?.values as? List<Customer>)?.let {
                                notifyDataSetChanged()
                            }
                        }
                    }
                }

                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = super.getView(position, convertView, parent)
                    (view as TextView).text = getItem(position)?.name ?: ""
                    return view
                }

                override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = super.getDropDownView(position, convertView, parent)
                    (view as TextView).text = getItem(position)?.name ?: ""
                    return view
                }
            }

            binding.customerSpinner.adapter = customerAdapter
            binding.customerSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val selectedCustomer = customerAdapter.getItem(position)
                    populateCustomerData(selectedCustomer)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
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