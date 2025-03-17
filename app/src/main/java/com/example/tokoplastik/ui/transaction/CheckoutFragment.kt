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
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
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
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class CheckoutFragment :
    BaseFragment<CheckoutViewModel, FragmentCheckoutBinding, CheckoutRepository>() {

    private lateinit var cartAdapter: CartAdapter
    private lateinit var invoiceGenerator: ReceiptGenerator
    private var loadingDialog: SweetAlertDialog? = null

    // Track observer registrations to avoid duplicates
    private val observersRegistered = mutableSetOf<String>()

    // Debounce mechanism for total update
    private var totalUpdateJob: Job? = null

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

    override fun onDestroyView() {
        // Cleanup resources
        loadingDialog?.dismissWithAnimation()
        loadingDialog = null

        // Clean adapter resources
        if (::cartAdapter.isInitialized) {
            cartAdapter.onDetached()
        }

        // Clear all potentially leaking observers
        viewModel.selectedProductPricesLoaded.removeObservers(viewLifecycleOwner)
        viewModel.searchResults.removeObservers(viewLifecycleOwner)
        viewModel.cartItems.removeObservers(viewLifecycleOwner)
        viewModel.paidAmount.removeObservers(viewLifecycleOwner)
        viewModel.dueDate.removeObservers(viewLifecycleOwner)
        viewModel.productPrices.removeObservers(viewLifecycleOwner)
        viewModel.addTransaction.removeObservers(viewLifecycleOwner)

        // Cancel any pending jobs
        totalUpdateJob?.cancel()

        super.onDestroyView()
    }

    private fun setupViews() {
        // Initialize adapter only once
        if (!::cartAdapter.isInitialized) {
            cartAdapter = CartAdapter(
                onQuantityChanged = { item, newQuantity ->
                    viewModel.updateItemQuantity(item, newQuantity)
                    debouncedTotalUpdate()
                },
                onPriceChanged = { item, newPrice ->
                    viewModel.updateItemPrice(item, newPrice)
                    debouncedTotalUpdate()
                },
                onUnitChanged = { item, newUnit ->
                    viewModel.updateItemUnit(item, newUnit)
                    debouncedTotalUpdate()
                },
                onDeleteItem = { item ->
                    viewModel.removeCartItem(item)
                    // Total will be updated through cartItems observer
                },
                onItemFocused = { position ->
                    // Only scroll if the recycler view exists and has items
                    if (position >= 0 && ::cartAdapter.isInitialized &&
                        binding.cartRecyclerView.adapter != null &&
                        position < (cartAdapter.itemCount)) {
                        binding.cartRecyclerView.postDelayed({
                            try {
                                binding.cartRecyclerView.smoothScrollToPosition(position)
                            } catch (e: Exception) {
                                Log.e("CheckoutFragment", "Scroll error: ${e.message}")
                            }
                        }, 100)
                    }
                }
            )
        }

        // Setup RecyclerView once
        binding.cartRecyclerView.apply {
            if (adapter == null) {
                adapter = cartAdapter
                layoutManager = LinearLayoutManager(requireContext())

                // Only attach helper if not already attached
                val swipeToDeleteCallback = CartAdapter.createSwipeToDelete(cartAdapter)
                val itemTouchHelper = ItemTouchHelper(swipeToDeleteCallback)
                itemTouchHelper.attachToRecyclerView(this)
            }
        }

        // Fetch products only once if needed
        if (viewModel.product.value?.data?.data.isNullOrEmpty()) {
            viewModel.getProducts()
        }

        setupProductsObserver()
        setupButtonListeners()
    }

    private fun setupProductsObserver() {
        if (observersRegistered.contains("products")) return

        viewModel.product.observe(viewLifecycleOwner) { result ->
            val productList = result.data?.data ?: emptyList()
            if (productList.isEmpty()) return@observe

            val displayList = productList.map { "${it.supplier} - ${it.name}" to it.id }

            // Create adapter only once or when data changes
            val productAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                displayList.map { it.first }
            )

            binding.productDropdown.apply {
                setAdapter(productAdapter)
                threshold = 1

                setOnItemClickListener { _, _, position, _ ->
                    val item = productAdapter.getItem(position)
                    val selectedItem = displayList.find { it.first == item }
                    val selectedProductId = selectedItem?.second

                    val selectedProduct = productList.find { it.id == selectedProductId }

                    selectedProduct?.let {
                        viewModel.selectProduct(it.id)
                        setupProductPricesLoadedObserver()
                    }
                }
            }
        }

        observersRegistered.add("products")
    }

    private fun setupProductPricesLoadedObserver() {
        // First remove any existing observer to avoid duplicates
        viewModel.selectedProductPricesLoaded.removeObservers(viewLifecycleOwner)

        viewModel.selectedProductPricesLoaded.observe(viewLifecycleOwner) { loaded ->
            if (loaded) {
                if (viewModel.selectedProductPrices.isNotEmpty()) {
                    hideKeyboard()
                    binding.productDropdown.clearFocus()
                } else {
                    showErrorDialog("No Price Found",
                        "Produk ini belum memiliki data harga. Silakan tambahkan harga terlebih dahulu.")
                    binding.productDropdown.text = null
                    viewModel.selectedProduct = null
                    viewModel.selectedProductPrices = emptyList()
                }
                // Reset the loading state for next selection
                viewModel.resetProductPricesLoadedState()

                // Remove this observer after it's done its job
                viewModel.selectedProductPricesLoaded.removeObservers(viewLifecycleOwner)
            }
        }
    }

    private fun setupButtonListeners() {
        binding.buttonBack.setOnClickListener {
            requireActivity().onBackPressed()
        }

        binding.buttonAddProduct.setOnClickListener {
            viewModel.addSelectedProductToCart()
            binding.productDropdown.text.clear()
        }

        binding.buttonCheckout.setOnClickListener {
            if (viewModel.currentCartItems.isNotEmpty()) {
                val bottomSheet = PaidBottomSheet()
                bottomSheet.show(childFragmentManager, "PAID_BOTTOM_SHEET")
            } else {
                // Tambahkan pesan bahwa keranjang kosong
                showErrorDialog("Keranjang Kosong", "Silakan tambahkan produk ke keranjang terlebih dahulu.")
            }
        }
    }

    private fun setupObservers() {
        // Avoid registering the same observer multiple times
        if (!observersRegistered.contains("searchResults")) {
            viewModel.searchResults.observe(viewLifecycleOwner) { products ->
                (binding.productDropdown.adapter as? ArrayAdapter<String>)?.let { adapter ->
                    adapter.clear()
                    adapter.addAll(products.map { it.name })
                }
            }
            observersRegistered.add("searchResults")
        }

        if (!observersRegistered.contains("cartItems")) {
            viewModel.cartItems.observe(viewLifecycleOwner) { items ->
                cartAdapter.submitList(items)  // Use submitList instead of custom method
                updateTotalAmount(items)
            }
            observersRegistered.add("cartItems")
        }

        if (!observersRegistered.contains("paymentFlow")) {
            setupPaymentFlowObservers()
            observersRegistered.add("paymentFlow")
        }
    }

    private var transactionStatus: String = ""

    private fun setupPaymentFlowObservers() {
        viewModel.paidAmount.observe(viewLifecycleOwner) { amount ->
            val totalAmount = viewModel.cartItems.value?.sumOf { it.customPrice * it.quantity } ?: 0

            val status = when {
                amount == 0 -> {
                    showDueDateSheet()
                    "belum lunas"
                }
                amount > 0 && amount < totalAmount -> {
                    showDueDateSheet()
                    "dalam cicilan"
                }
                else -> {
                    viewModel.setDueDate("-")
                    "lunas"
                }
            }

            // Store status for later use
            transactionStatus = status
        }

        viewModel.dueDate.observe(viewLifecycleOwner) { date ->
            if (transactionStatus.isNotEmpty()) {
                processCheckout(transactionStatus, date.takeIf { it.isNotEmpty() } ?: "-")
            }
        }
    }

    private fun showDueDateSheet() {
        if (isAdded && childFragmentManager.findFragmentByTag("DUE_DATE_BOTTOM_SHEET") == null) {
            val bottomSheet = DueDateBottomSheet()
            bottomSheet.show(childFragmentManager, "DUE_DATE_BOTTOM_SHEET")
        }
    }

    private fun debouncedTotalUpdate() {
        totalUpdateJob?.cancel()
        totalUpdateJob = viewLifecycleOwner.lifecycleScope.launch {
            delay(300) // Debounce for 300ms
            val items = viewModel.cartItems.value ?: return@launch
            updateTotalAmount(items)
        }
    }

    private fun updateTotalAmount(items: List<CartItem>) {
        val total = items.sumOf { it.customPrice * it.quantity }

        val symbols = DecimalFormatSymbols(Locale.getDefault()).apply {
            groupingSeparator = '.'
        }
        val formatter = DecimalFormat("#,###", symbols)
        val formattedTotal = formatter.format(total.toDouble())
        binding.countTotal.text = "Rp$formattedTotal"
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    fun processCheckout(status: String, tempo: String) {
        if (!isAdded) return

        context?.let { ctx ->
            // Don't create multiple loading dialogs
            if (loadingDialog == null) {
                loadingDialog = SweetAlertDialog(ctx, SweetAlertDialog.PROGRESS_TYPE).apply {
                    titleText = "Processing"
                    contentText = "Processing order..."
                    setCancelable(false)
                    show()
                }
            }

            // Prevent multiple observer registrations
            if (!observersRegistered.contains("checkout")) {
                setupCheckoutObservers(status, tempo)
                observersRegistered.add("checkout")
            } else {
                // If already registered, just trigger checkout
                viewModel.checkout(status, tempo)
            }
        }
    }

    private fun setupCheckoutObservers(status: String, tempo: String) {
        // Get product prices only once if not already fetched
        if (viewModel.productPrices.value !is Resource.Success) {
            viewModel.getAllProductPrices()
        }

        viewModel.productPrices.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Resource.Success -> {
                    invoiceGenerator.allProductPrices = result.data?.data ?: emptyList()
                }
                is Resource.Failure -> {
                    handleApiError(result)
                }
                is Resource.Loading -> {
                    // No need to do anything while loading
                }
            }
        }

        viewModel.addTransaction.observe(viewLifecycleOwner) { result ->
            if (!isAdded) {
                loadingDialog?.dismissWithAnimation()
                loadingDialog = null
                return@observe
            }

            when (result) {
                is Resource.Success -> {
                    loadingDialog?.dismissWithAnimation()
                    loadingDialog = null

                    result.data?.let { response ->
                        val cartItems = viewModel.cartItems.value ?: emptyList()
                        if (cartItems.isNotEmpty()) {
                            try {
                                val pdfFile = invoiceGenerator.generatedPdfReceipt(
                                    response.data,
                                    cartItems,
                                    response.data.id.toString()
                                )
                                showSuccessDialog(
                                    pdfFile,
                                    response.data,
                                    cartItems,
                                    response.data.id.toString()
                                )
                            } catch (e: Exception) {
                                handleReceiptError(e)
                            }
                        } else {
                            showErrorDialog("Error", "Cart is empty")
                        }
                    }
                }
                is Resource.Failure -> {
                    loadingDialog?.dismissWithAnimation()
                    loadingDialog = null
                    handleApiError(result)
                }
                is Resource.Loading -> {
                    // Handled by progress dialog
                }
            }
        }

        // Now trigger the checkout
        viewModel.checkout(status, tempo)
    }

    private fun showSuccessDialog(
        pdfFile: File,
        transaction: Transaction,
        cartItems: List<CartItem>,
        orderId: String
    ) {
        if (!isAdded) return

        SweetAlertDialog(requireContext(), SweetAlertDialog.SUCCESS_TYPE).apply {
            titleText = "Success!"
            contentText = "Order completed successfully"
            setCanceledOnTouchOutside(false)
            setConfirmButton("OK") {
                dismissWithAnimation()
                if (pdfFile.exists()) {
                    showReceiptOptionsDialog(pdfFile, transaction, cartItems, orderId)
                } else {
                    navigateToTransactionFragment()
                }
            }
            show()
        }
    }

    private fun showReceiptOptionsDialog(
        pdfFile: File,
        transaction: Transaction,
        cartItems: List<CartItem>,
        orderId: String
    ) {
        if (!isAdded) return

        SweetAlertDialog(requireContext(), SweetAlertDialog.NORMAL_TYPE).apply {
            titleText = "Receipt Options"
            contentText = "What would you like to do with the receipt?"

            setConfirmButton("Print") {
                dismissWithAnimation()
                val invoiceText = invoiceGenerator.generateInvoiceText(transaction, cartItems, orderId)
                val file = invoiceGenerator.saveInvoiceToFile(
                    requireContext(),
                    invoiceText,
                    "Invoice_${orderId}.txt"
                )
                invoiceGenerator.shareReceiptTxt(file)
                navigateToTransactionFragment()
            }

            setCancelButton("Share") {
                dismissWithAnimation()
                invoiceGenerator.shareReceipt(pdfFile)
                navigateToTransactionFragment()
            }

            setNeutralButton("Skip") {
                dismissWithAnimation()
                navigateToTransactionFragment()
            }
            show()
        }
    }

    private fun showErrorDialog(title: String, message: String) {
        if (!isAdded) return

        SweetAlertDialog(requireContext(), SweetAlertDialog.ERROR_TYPE).apply {
            titleText = title
            contentText = message
            setConfirmButton("OK") { dismissWithAnimation() }
            show()
        }
    }

    private fun handleReceiptError(error: Exception) {
        if (!isAdded) return

        showErrorDialog("Receipt Generation Error",
            "Failed to generate receipt: ${error.message}")
    }

    private fun navigateToTransactionFragment() {
        if (isAdded) {
            findNavController().navigate(R.id.action_checkoutFragment_to_transactionFragment)
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