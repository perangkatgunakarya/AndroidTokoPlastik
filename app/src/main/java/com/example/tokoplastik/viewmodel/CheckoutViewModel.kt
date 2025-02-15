package com.example.tokoplastik.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.tokoplastik.data.repository.CheckoutRepository
import com.example.tokoplastik.data.responses.AllTransactionResponses
import com.example.tokoplastik.data.responses.CartItem
import com.example.tokoplastik.data.responses.GetProduct
import com.example.tokoplastik.data.responses.GetProductByIdResponses
import com.example.tokoplastik.data.responses.GetProductResponses
import com.example.tokoplastik.data.responses.PaymentStatusUpdateRequest
import com.example.tokoplastik.data.responses.PaymentStatusUpdateResponses
import com.example.tokoplastik.data.responses.ProductPrice
import com.example.tokoplastik.data.responses.ProductPricesResponses
import com.example.tokoplastik.data.responses.TransactionDetailResponses
import com.example.tokoplastik.data.responses.TransactionRequest
import com.example.tokoplastik.data.responses.TransactionResponses
import com.example.tokoplastik.ui.base.BaseViewModel
import com.example.tokoplastik.util.Resource
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CheckoutViewModel(
    private val repository: CheckoutRepository
) : BaseViewModel(repository) {

    var customerId: Int = 0

    private val _product = MutableLiveData<Resource<GetProductResponses?>>()
    val product: LiveData<Resource<GetProductResponses?>>
        get() = _product

    private val _productPrices = MutableLiveData<Resource<ProductPricesResponses>>()
    val productPrices: LiveData<Resource<ProductPricesResponses>>
        get() = _productPrices

    private val _searchResults = MutableLiveData<List<GetProduct>>()
    val searchResults: LiveData<List<GetProduct>> = _searchResults

    private val _cartItems = MutableLiveData<List<CartItem>>()
    val cartItems: LiveData<List<CartItem>> = _cartItems

    private val _checkoutStatus = MutableLiveData<Boolean>()
    val checkoutStatus: LiveData<Boolean> = _checkoutStatus

    private val _transactions = MutableLiveData<Resource<AllTransactionResponses>>()
    val transactions: LiveData<Resource<AllTransactionResponses>>
        get() = _transactions

    private val _paidAmount = MutableLiveData<Int>()
    val paidAmount: LiveData<Int> = _paidAmount

    private val _addTransaction: MutableLiveData<Resource<TransactionResponses>> = MutableLiveData()
    val addTransaction: LiveData<Resource<TransactionResponses>>
        get() = _addTransaction

    private val _transactionDetail: MutableLiveData<Resource<TransactionDetailResponses>> = MutableLiveData()
    val transactionDetail: LiveData<Resource<TransactionDetailResponses>>
        get() = _transactionDetail

    private val _paymentStatus: MutableLiveData<Resource<PaymentStatusUpdateResponses>> = MutableLiveData()
    val paymentStatus: LiveData<Resource<PaymentStatusUpdateResponses>>
        get() = _paymentStatus

    var selectedProduct: Resource<GetProductByIdResponses>? = null
    private var selectedProductPriceProducts: Resource<ProductPricesResponses>? = null
    var selectedProductPrices: List<ProductPrice> = emptyList()
    var currentCartItems = mutableListOf<CartItem>()
    var unsignedproductPrice: Boolean = false

    // sort and filter
    private val _sortType = MutableLiveData(SortType.DATE)
    val sortType: LiveData<SortType> = _sortType

    private val _sortOrder = MutableLiveData(SortOrder.DESCENDING)
    val sortOrder: LiveData<SortOrder> = _sortOrder

    private val _isDateSortAscending = MutableLiveData(true)
    val isDateSortAscending: LiveData<Boolean> = _isDateSortAscending

    private val _startDate = MutableLiveData<Long>()
    val startDate: LiveData<Long> = _startDate

    private val _endDate = MutableLiveData<Long>()
    val endDate: LiveData<Long> = _endDate

    private val _filteredData = MutableLiveData<List<CartItem>>()
    val filteredData: LiveData<List<CartItem>> = _filteredData

    private val _dueDate = MutableLiveData<String>()
    val dueDate: LiveData<String>
        get() = _dueDate

    fun getTransactions() = viewModelScope.launch {
        _transactions.value = Resource.Loading
        _transactions.value = repository.getTransactions()
    }

    fun getTransactionDetail(transactionId: Int) = viewModelScope.launch {
        _transactionDetail.value = Resource.Loading
        _transactionDetail.value = repository.getTransactionDetail(transactionId)
    }

    fun getProducts() = viewModelScope.launch {
        _product.value = Resource.Loading
        _product.value = repository.getProduct()
    }

    fun getAllProductPrices() = viewModelScope.launch {
        _productPrices.value = Resource.Loading
        _productPrices.value = repository.getAllProductPrices()
    }

    fun setDueDate(date: String) {
        _dueDate.value = date
    }

    fun selectProduct(position: Int) {
        viewModelScope.launch {
            selectedProduct = repository.getProductDetail(position)
            selectedProductPriceProducts = repository.getProductPrices(position)
            selectedProductPrices = selectedProductPriceProducts?.data?.data ?: emptyList()
        }
    }

    fun addSelectedProductToCart() {
        val product = selectedProduct?.data

        if (product != null && selectedProductPrices.isNotEmpty()) {
            unsignedproductPrice = false
            val defaultPrice = selectedProductPrices.first()
            val cartItem = CartItem(
                product = product,
                productPrice = selectedProductPrices,
                selectedPrice = defaultPrice,
                quantity = 1,
                customPrice = defaultPrice.price,
                paymentStatus = "belum lunas"
            )
            currentCartItems.add(cartItem)
            _cartItems.value = currentCartItems.toList()

            selectedProduct = null
            selectedProductPrices = emptyList()
        } else {
            unsignedproductPrice = true
        }
    }

    fun updateItemQuantity(item: CartItem, newQuantity: Int) {
        item.quantity = newQuantity
        _cartItems.value = currentCartItems.toList()
    }

    fun updateItemPrice(item: CartItem, newPrice: Int) {
        item.customPrice = newPrice
        _cartItems.value = currentCartItems.toList()
    }

    fun updateItemUnit(item: CartItem, newUnit: ProductPrice) {
        item.selectedPrice = newUnit
        item.customPrice = newUnit.price
        _cartItems.value = currentCartItems.toList()
    }

    fun removeCartItem(item: CartItem) {
        currentCartItems.remove(item)
        _cartItems.value = currentCartItems.toList()
    }

    fun setPaidAmount(amount: Int) {
        _paidAmount.value = amount
    }

    fun checkout(paymentStatus: String, dueDate: String) = viewModelScope.launch {
        val productPriceIds = currentCartItems.map { it.selectedPrice.id }
        val priceAdjustments = currentCartItems.map { it.customPrice }
        val quantity = currentCartItems.map { it.quantity }
        val paid = paidAmount.value ?: 0
        try {
            val transaction = TransactionRequest(
                customerId,
                currentCartItems.sumOf { it.customPrice * it.quantity },
                paid,
                productPriceIds,
                priceAdjustments,
                paymentStatus,
                dueDate,
                quantity
            )
            val transactionResult = repository.addTransaction(transaction)
            _addTransaction.value = transactionResult
            _checkoutStatus.value = true
        } catch (e: Exception) {
            _checkoutStatus.value = false
        }
    }

    fun updatePaymentStatus(transactionId: Int, status: String, paid: Int?) = viewModelScope.launch {
        _paymentStatus.value = Resource.Loading
        val statusUpdateRequest = PaymentStatusUpdateRequest(paid, status)
        _paymentStatus.value = repository.setPaymentStatus(transactionId, statusUpdateRequest)
    }

    fun setSortType(type: SortType) {
        _sortType.value = type
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun setDateRange(start: Long, end: Long) {
        _startDate.value = start
        _endDate.value = end
    }

    fun applyFilters() = viewModelScope.launch {
        _transactions.value = Resource.Loading
        _transactions.value = repository.getTransactions(startDate.value?.let { formatDate(it) },
            endDate.value?.let { formatDate(it) })

        _isDateSortAscending.value = sortOrder.value == SortOrder.ASCENDING
    }

    private fun formatDate(timestamp: Long): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp))
    }
}