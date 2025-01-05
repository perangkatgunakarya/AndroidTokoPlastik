package com.example.tokoplastik.ui.transaction

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
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
import com.example.tokoplastik.data.responses.Transaction
import com.example.tokoplastik.databinding.FragmentCheckoutBinding
import com.example.tokoplastik.ui.base.BaseFragment
import com.example.tokoplastik.ui.history.SortFilterBottomSheet
import com.example.tokoplastik.util.ReceiptGenerator
import com.example.tokoplastik.util.Resource
import com.example.tokoplastik.util.handleApiError
import com.example.tokoplastik.viewmodel.CheckoutViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File

class CheckoutFragment :
    BaseFragment<CheckoutViewModel, FragmentCheckoutBinding, CheckoutRepository>() {

    private lateinit var cartAdapter: CartAdapter
    private lateinit var invoiceGenerator: ReceiptGenerator

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        invoiceGenerator = ReceiptGenerator(requireContext())

        val data: CheckoutFragmentArgs by navArgs()
        viewModel.customerId = data.customerId.toInt()

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            binding.cartRecyclerView.updatePadding(bottom = imeHeight)
            insets
        }

        setupViews()
        setupObservers()
    }

    override fun onResume() {
        super.onResume()
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    override fun onPause() {
        super.onPause()
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
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
            },
            onItemFocused = { position ->
                binding.cartRecyclerView.postDelayed({
                    try {
                        binding.cartRecyclerView.smoothScrollToPosition(position)
                    } catch (e: Exception) {
                        Log.e("CheckoutFragment", "Scroll error: ${e.message}")
                    }
                }, 100)
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
            val productList = result.data?.data ?: emptyList()
            val displayList = productList.map { "${it.supplier} - ${it.name}" to it.id }

            val productAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                displayList.map { it.first }
            )

            binding.productDropdown.setAdapter(productAdapter)
            binding.productDropdown.threshold = 1

            binding.productDropdown.setOnItemClickListener { _, _, position, _ ->
                val item = productAdapter.getItem(position)
                val selectedItem = displayList.find { it.first == item }
                val selectedProductId = selectedItem?.second

                val selectedProduct = productList.find { it.id == selectedProductId }

                if (selectedProduct != null) {
                    viewModel.selectProduct(selectedProduct.id)
                    if (viewModel.unsignedproductPrice != true) {
                        hideKeyboard()
                    } else {
                        SweetAlertDialog(requireContext(), SweetAlertDialog.ERROR_TYPE).apply {
                            titleText = "No Price Found"
                            contentText = "Produk ini belum memiliki data harga. Silakan tambahkan harga terlebih dahulu."
                            setConfirmButton("OK") {
                                dismissWithAnimation()
                                binding.productDropdown.text = null
                            }
                            setCancelable(false)
                            setCanceledOnTouchOutside(false)
                            show()
                        }
                    }
                }
            }

            binding.buttonAddProduct.setOnClickListener {
                viewModel.addSelectedProductToCart()
                binding.productDropdown.text.clear()
            }

            if (viewModel.currentCartItems.isEmpty()) {
                binding.buttonCheckout.setOnClickListener {
                    val bottomSheet = PaidBottomSheet()
                    bottomSheet.show(childFragmentManager, "SORT_FILTER_BOTTOM_SHEET")
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

        viewModel.paidAmount.observe(viewLifecycleOwner) { amount ->
            if (amount == 0) {
                processCheckout("belum lunas")
            }
            if (amount > 0 && amount < viewModel.cartItems.value?.sumOf { it.customPrice * it.quantity } ?: 0) {
                processCheckout("dalam cicilan")
            }
            else {
                processCheckout("lunas")
            }
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
        if (!isAdded) return

        context?.let { ctx ->
            val dialog = SweetAlertDialog(ctx, SweetAlertDialog.NORMAL_TYPE)
            dialog.apply {
                titleText = "Pembayaran"
                contentText = "Status Pembayaran"

                confirmButtonBackgroundColor = ContextCompat.getColor(ctx, R.color.g_blue)
                cancelButtonBackgroundColor = ContextCompat.getColor(ctx, R.color.g_orange_yellow)

                confirmButtonTextColor = ContextCompat.getColor(ctx, android.R.color.white)
                cancelButtonTextColor = ContextCompat.getColor(ctx, android.R.color.white)

                setConfirmClickListener { sDialog ->
                    sDialog.dismissWithAnimation()
                    processCheckout("lunas")
                }

                setCancelClickListener { sDialog ->
                    sDialog.dismissWithAnimation()
                    processCheckout("belum lunas")
                }

                confirmText = "Lunas"
                cancelText = "Belum Lunas"

                setCancelable(true)
                setCanceledOnTouchOutside(true)

                show()
            }
        }
    }

    private fun processCheckout(paymentMethod: String) {
        if (!isAdded) return

        context?.let { ctx ->
            val loadingDialog = SweetAlertDialog(ctx, SweetAlertDialog.PROGRESS_TYPE)
            loadingDialog.apply {
                titleText = "Processing"
                contentText = "Processing order..."
                setCancelable(false)
                show()
            }

            viewModel.addTransaction.observe(viewLifecycleOwner, { result ->
                if (!isAdded) {
                    loadingDialog.dismissWithAnimation()
                    return@observe
                }

                when (result) {
                    is Resource.Success -> {
                        loadingDialog.dismissWithAnimation()
                        result.data?.let { response ->
                            val cartItems = viewModel.cartItems.value ?: emptyList()
                            if (cartItems.isNotEmpty()) {
                                try {
                                    val pdfFile = invoiceGenerator.generatedPdfReceipt(
                                        response.data,
                                        cartItems,
                                        response.data.id.toString()
                                    )
                                    showSuccessDialog(pdfFile, response.data, cartItems, response.data.id.toString())
                                } catch (e: Exception) {
                                    handleReceiptError(e)
                                }
                            } else {
                                Log.e("CheckoutFragment", "Cart is empty")
                                loadingDialog.changeAlertType(SweetAlertDialog.ERROR_TYPE)
                                loadingDialog.titleText = "Error"
                                loadingDialog.contentText = "Cart is empty"
                            }
                        }
                    }
                    is Resource.Failure -> {
                        loadingDialog.dismissWithAnimation()
                        handleApiError(result)
                    }
                    is Resource.Loading -> {
                    }
                }
            })

            viewModel.checkout(paymentMethod)
        }
    }

    private fun showSuccessDialog(
        pdfFile: File,
        transaction: Transaction,
        cartItems: List<CartItem>,
        orderId: String
    ) {
        if (!isAdded) return
        context?.let { ctx->
            SweetAlertDialog(requireContext(), SweetAlertDialog.SUCCESS_TYPE).apply {
                titleText = "Success!"
                contentText = "Order completed successfully"
                setCanceledOnTouchOutside(false)
                Log.i("order complete", "pdf file = ${pdfFile.exists()}")
                setConfirmButton("OK") {
                    dismissWithAnimation()
                    if (pdfFile.exists()) {
                        showReceiptOptionsDialog(pdfFile, transaction, cartItems, orderId)
                    } else {
                        findNavController().navigate(R.id.action_checkoutFragment_to_transactionFragment)
                    }
                }
                show()
            }
        }
    }

    private fun showReceiptOptionsDialog(
        pdfFile: File,
        transaction: Transaction,
        cartItems: List<CartItem>,
        orderId: String
    ) {
        SweetAlertDialog(requireContext(), SweetAlertDialog.NORMAL_TYPE).apply {
            titleText = "Receipt Options"
            contentText = "What would you like to do with the receipt?"

            setConfirmButton("Print") {
                dismissWithAnimation()
                invoiceGenerator.printReceipt(transaction, cartItems, orderId)
                findNavController().navigate(R.id.action_checkoutFragment_to_transactionFragment)
            }

            setCancelButton("Share") {
                dismissWithAnimation()
                invoiceGenerator.shareReceipt(pdfFile)
                findNavController().navigate(R.id.action_checkoutFragment_to_transactionFragment)
            }

            setNeutralButton("Skip") {
                dismissWithAnimation()
                findNavController().navigate(R.id.action_checkoutFragment_to_transactionFragment)
            }
            show()
        }
    }

    private fun handleReceiptError(error: Exception) {
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