package com.example.tokoplastik.ui.transaction

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import cn.pedant.SweetAlert.SweetAlertDialog
import com.example.tokoplastik.R
import com.example.tokoplastik.adapter.CartAdapter
import com.example.tokoplastik.data.network.CheckoutApi
import com.example.tokoplastik.data.repository.CheckoutRepository
import com.example.tokoplastik.data.responses.CartItem
import com.example.tokoplastik.databinding.FragmentCheckoutBinding
import com.example.tokoplastik.ui.base.BaseFragment
import com.example.tokoplastik.util.ReceiptGenerator
import com.example.tokoplastik.util.ReceiptHandler
import com.example.tokoplastik.util.Resource
import com.example.tokoplastik.util.handleApiError
import com.example.tokoplastik.viewmodel.CheckoutViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class CheckoutFragment :
    BaseFragment<CheckoutViewModel, FragmentCheckoutBinding, CheckoutRepository>() {

    private lateinit var cartAdapter: CartAdapter
    private lateinit var receiptHandler: ReceiptHandler
    private var generatedHtml: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val data: CheckoutFragmentArgs by navArgs()
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

            val swipeToDeleteCallback = CartAdapter.createSwipeToDelete(cartAdapter)
            val itemTouchHelper = ItemTouchHelper(swipeToDeleteCallback)
            itemTouchHelper.attachToRecyclerView(this)
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

            if (viewModel.currentCartItems.isEmpty()) {
                binding.buttonCheckout.setOnClickListener {
                    showPaymentStatusDialog()
                }
            }
        })
    }

    private fun setupObservers() {
        viewModel.searchResults.observe(viewLifecycleOwner) { products ->
            val adapter = binding.productDropdown.adapter as ArrayAdapter<String>
            adapter.clear()
            adapter.addAll(products.map { it.name })
        }

        viewModel.cartItems.observe(viewLifecycleOwner) { items ->
            cartAdapter.updateItems(items)
            updateTotalAmount(items)
        }
    }

    private fun updateTotalAmount(items: List<CartItem>) {
        val total = items.sumOf { it.customPrice * it.quantity }
        binding.countTotal.text = getString(R.string.price_format, total.toDouble())
    }

    private fun hideKeyboard() {
        val imm =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(requireView().windowToken, 0)
    }

    private fun showPaymentStatusDialog() {
        SweetAlertDialog(requireContext(), SweetAlertDialog.NORMAL_TYPE).apply {
            titleText = "Pembayaran"
            contentText = "Status Pembayaran"

            setConfirmButton("Lunas") { dialog ->
                dialog.dismissWithAnimation()
                processCheckout("lunas")
            }

            setCancelButton("Belum Lunas") { dialog ->
                dialog.dismissWithAnimation()
                processCheckout("belum lunas")
            }

            confirmButtonBackgroundColor = R.color.g_blue
            cancelButtonBackgroundColor = R.color.g_orange_yellow

            setCancelable(true)
            setCanceledOnTouchOutside(true)

            show()
        }
    }

    private fun processCheckout(paymentMethod: String) {
        val loadingDialog =
            SweetAlertDialog(requireContext(), SweetAlertDialog.PROGRESS_TYPE).apply {
                titleText = "Processing"
                contentText = "Processing order..."
                setCancelable(false)
                show()
            }

        viewModel.checkout(paymentMethod)
        viewModel.addTransaction.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Resource.Success -> {
                    loadingDialog.dismiss()
                    result.data?.let { response ->
                        Log.i("hasil Checkout response", "${response}")
                        val cartItems = viewModel.cartItems.value ?: emptyList()
                        if (cartItems.isNotEmpty()) {
                            Log.i("hasil Checkout response", "akan muncul jika cartitem not empty")
                            try {
                                val html = ReceiptGenerator.generateHtmlReceipt(
                                    orderData = response.data,
                                    cartItems = cartItems,
                                    orderId = response.data.id.toString()
                                )
                                showSuccessDialog(html)
                            } catch (e: Exception) {
                                handleReceiptError(e)
                            }
                        } else {
                            Log.i("hasil Checkout response", "akan muncul jika cartitem empty")
                            showSuccessDialog(null)
                        }
                    }
                }

                is Resource.Failure -> {
                    loadingDialog.dismiss()
                    Log.i("hasil Checkout response", "akan muncul jika error api")
                    handleApiError(result)
                }
                is Resource.Loading -> {}
            }
        }
    }

    private fun showSuccessDialog(receiptHtml: String?) {
        Log.i("hasil Checkout response", "akan muncul jika success dialog muncul")
        SweetAlertDialog(requireContext(), SweetAlertDialog.SUCCESS_TYPE).apply {
            titleText = "Success!"
            contentText = "Order completed successfully"
            setConfirmButton("OK") {
                dismissWithAnimation()
                if (receiptHtml != null) {
                    showReceiptOptionsDialog(receiptHtml)
                } else {
                    findNavController().navigate(R.id.action_checkoutFragment_to_transactionFragment)
                }
            }
            show()
        }
    }

    private fun showReceiptOptionsDialog(receiptHtml: String) {
        Log.i("hasil Checkout response", "akan muncul jika receipt dialog muncul")
        SweetAlertDialog(requireContext(), SweetAlertDialog.NORMAL_TYPE).apply {
            titleText = "Receipt Options"
            contentText = "What would you like to do with the receipt?"

            setConfirmButton("Print") {
                dismissWithAnimation()
                receiptHandler.printReceipt(receiptHtml)
            }

            setCancelButton("Share") {
                dismissWithAnimation()
                receiptHandler.shareReceipt(receiptHtml)
            }

            setNeutralButton("Skip") {
                dismissWithAnimation()
                findNavController().navigate(R.id.action_checkoutFragment_to_transactionFragment)
            }
            show()
        }
    }

    private fun handleReceiptError(error: Exception) {
        Log.i("hasil Checkout response", "akan muncul jika receipt error")
        SweetAlertDialog(requireContext(), SweetAlertDialog.ERROR_TYPE).apply {
            titleText = "Receipt Generation Error"
            contentText = "Failed to generate receipt: ${error.message}"
            setConfirmButton("OK") {
                dismissWithAnimation()
                findNavController().navigate(R.id.action_checkoutFragment_to_transactionFragment)
            }
            show()
        }
    }

    override fun getViewModel() = CheckoutViewModel::class.java

    override fun getFragmentBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentCheckoutBinding.inflate(inflater, container, false)

    override fun getFragmentRepository(): CheckoutRepository {
        val token = runBlocking { userPreferences.authToken.first() }
        val api = remoteDataSource.buildApi(CheckoutApi::class.java, token)
        return CheckoutRepository(api)
    }
}