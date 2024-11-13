package com.example.tokoplastik.ui.transaction

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tokoplastik.R
import com.example.tokoplastik.adapter.CartAdapter
import com.example.tokoplastik.data.network.CheckoutApi
import com.example.tokoplastik.data.repository.CheckoutRepository
import com.example.tokoplastik.data.responses.CartItem
import com.example.tokoplastik.databinding.FragmentCheckoutBinding
import com.example.tokoplastik.ui.base.BaseFragment
import com.example.tokoplastik.viewmodel.CheckoutViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class CheckoutFragment : BaseFragment<CheckoutViewModel, FragmentCheckoutBinding, CheckoutRepository>() {

    private lateinit var cartAdapter: CartAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val data : CheckoutFragmentArgs by navArgs()
        viewModel.customerId = data.customerId.toInt()

        setupViews()
        setupObservers()
    }

    private fun setupViews() {
        cartAdapter = CartAdapter(
            onQuantityChanged = { item, newQuantity ->
                viewModel.updateItemQuantity(item, newQuantity)
            },
            onPriceChanged = { item, newPrice ->
                viewModel.updateItemPrice(item, newPrice)
            },
            onUnitChanged = { item, newUnit ->
                viewModel.updateItemUnit(item, newUnit)
            },
            onDeleteItem = { item ->
                viewModel.removeCartItem(item)
            }
        )
        binding.cartRecyclerView.apply {
            adapter = cartAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        viewModel.getProducts()
        viewModel.product.observe(viewLifecycleOwner, Observer { result ->
            val productNames = result.data?.data?.map { it.name } ?: emptyList()
            val productAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                productNames
            )

            binding.productDropdown.setAdapter(productAdapter)
            binding.productDropdown.threshold = 1

            binding.productDropdown.setOnItemClickListener { _, _, position, _ ->
                val selectedProductName = productAdapter.getItem(position)
                val selectedProduct = result.data?.data?.find { it.name == selectedProductName }
                if (selectedProduct != null) {
                    viewModel.selectProduct(selectedProduct.id)
                    hideKeyboard()
                }
            }

            binding.buttonAddProduct.setOnClickListener {
                viewModel.addSelectedProductToCart()
                binding.productDropdown.text.clear()
            }

            binding.buttonCheckout.setOnClickListener {
                viewModel.checkout()
            }
        })
    }

    private fun setupObservers() {
        // Observe search results
        viewModel.searchResults.observe(viewLifecycleOwner) { products ->
            val adapter = binding.productDropdown.adapter as ArrayAdapter<String>
            adapter.clear()
            adapter.addAll(products.map { it.name })
        }

        // Observe cart items
        viewModel.cartItems.observe(viewLifecycleOwner) { items ->
            cartAdapter.updateItems(items)
            updateTotalAmount(items)
        }

        // Observe checkout status
        viewModel.checkoutStatus.observe(viewLifecycleOwner) { success ->
            if (success) {
                // Navigate back or to next screen
                findNavController().navigateUp()
            }
        }
    }

    private fun updateTotalAmount(items: List<CartItem>) {
        val total = items.sumOf { it.customPrice * it.quantity }
        binding.countTotal.text = getString(R.string.price_format, total.toDouble())
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(requireView().windowToken, 0)
    }

    override fun getViewModel() = CheckoutViewModel::class.java

    override fun getFragmentBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentCheckoutBinding.inflate(inflater, container, false)

    override fun getFragmentRepository() : CheckoutRepository {
        val token = runBlocking { userPreferences.authToken.first() }
        val api = remoteDataSource.buildApi(CheckoutApi::class.java, token)
        return CheckoutRepository(api)
    }
}