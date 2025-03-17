package com.example.tokoplastik.ui.transaction

import android.content.Context
import android.content.Context.INPUT_METHOD_SERVICE
import android.content.Intent
import android.content.RestrictionEntry.TYPE_NULL
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.example.tokoplastik.R
import com.example.tokoplastik.data.network.CustomerApi
import com.example.tokoplastik.data.repository.CustomerRepository
import com.example.tokoplastik.data.responses.Customer
import com.example.tokoplastik.databinding.FragmentTransactionBinding
import com.example.tokoplastik.ui.base.BaseFragment
import com.example.tokoplastik.ui.product.AddProductActivity
import com.example.tokoplastik.ui.product.ProductSortBottomSheet
import com.example.tokoplastik.util.enable
import com.example.tokoplastik.util.visible
import com.example.tokoplastik.viewmodel.TransactionViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class TransactionFragment :
    BaseFragment<TransactionViewModel, FragmentTransactionBinding, CustomerRepository>() {

    private var customer_id: Int? = null

    override fun onResume() {
        super.onResume()

        binding.customerNameText.setText("")
        binding.customerAddressText.setText("")
        binding.customerHpText.setText("")
        binding.buttonToCheckout.enable(false)
        customer_id = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        val nameEditText = binding.customerNameText
        val addressEditText = binding.customerAddressText
        val hpEditText = binding.customerHpText

        binding.buttonAddCustomer.setOnClickListener {
            val directions =
                TransactionFragmentDirections.actionTransactionFragmentToAddCustomerFragment()
            findNavController().navigate(directions)
        }

        nameEditText.isFocusable = false
        addressEditText.isFocusable = false
        hpEditText.isFocusable = false

        nameEditText.inputType = TYPE_NULL
        addressEditText.inputType = TYPE_NULL
        hpEditText.inputType = TYPE_NULL

        setupCustomerAutocomplete()

        binding.menuIcon.setOnClickListener {
            showPopupMenu(it)
        }

        if (nameEditText.text.isNullOrEmpty()) {
            binding.buttonToCheckout.enable(false)
        }

        // Atur listener untuk WindowInsets
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())

            // Sesuaikan margin bottom button_add_product
            val params = binding.buttonToCheckout.layoutParams as ViewGroup.MarginLayoutParams
            params.bottomMargin =
                insets.bottom + resources.getDimensionPixelSize(R.dimen.add_button_margin) // Tambahkan margin tambahan
            binding.buttonToCheckout.layoutParams = params

            // Kembalikan insets yang telah dikonsumsi
            WindowInsetsCompat.CONSUMED
        }

        binding.buttonToCheckout.setOnClickListener {
            val action = TransactionFragmentDirections.actionTransactionFragmentToCheckoutFragment(
                customer_id.toString()
            )
            findNavController().navigate(action)
        }

        binding.buttonEditData.setOnClickListener {
            if (customer_id != null) {
                val action =
                    TransactionFragmentDirections.actionTransactionFragmentToUpdateCustomerFragment(
                        customer_id!!
                    )
                findNavController().navigate(action)
            }
        }
    }

    private fun showPopupMenu(view: View) {
        val popupMenu = androidx.appcompat.widget.PopupMenu(requireContext(), view)
        popupMenu.inflate(R.menu.transaction_fragment_menu)
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_customer_list -> {
                    val action =
                        TransactionFragmentDirections.actionTransactionFragmentToCustomerFragment()
                    findNavController().navigate(action)
                    true
                }

                else -> false
            }
        }
        popupMenu.show()
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
                binding.customerDropdown.text.clear()
                hideKeyboard()
            }
        })
    }

    private fun populateCustomerData(customer: Customer?) {
        customer?.let {
            binding.customerNameText.setText(it.name)
            binding.customerAddressText.setText(it.address)
            binding.customerHpText.setText(it.phone)
            customer_id = it.id
        }
        binding.buttonToCheckout.enable(true)
    }

    private fun hideKeyboard() {
        val imm =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(requireView().windowToken, 0)
    }

    override fun getViewModel() = TransactionViewModel::class.java

    override fun getFragmentBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentTransactionBinding.inflate(inflater, container, false)

    override fun getFragmentRepository(): CustomerRepository {
        val token = runBlocking { userPreferences.authToken.first() }
        val api = remoteDataSource.buildApi(CustomerApi::class.java, token)
        return CustomerRepository(api)
    }
}