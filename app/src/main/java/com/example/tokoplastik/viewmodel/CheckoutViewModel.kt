package com.example.tokoplastik.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.example.tokoplastik.data.repository.CheckoutRepository
import com.example.tokoplastik.data.responses.AddProductResponse
import com.example.tokoplastik.data.responses.CartItem
import com.example.tokoplastik.data.responses.GetProduct
import com.example.tokoplastik.data.responses.GetProductByIdResponses
import com.example.tokoplastik.data.responses.GetProductResponses
import com.example.tokoplastik.data.responses.ProductPrice
import com.example.tokoplastik.data.responses.ProductPricesResponses
import com.example.tokoplastik.data.responses.TransactionRequest
import com.example.tokoplastik.data.responses.TransactionResponses
import com.example.tokoplastik.ui.base.BaseViewModel
import com.example.tokoplastik.util.Resource
import kotlinx.coroutines.launch

class CheckoutViewModel(
    private val repository: CheckoutRepository
) : BaseViewModel(repository) {

    var customerId: Int = 0

    private val _product = MutableLiveData<Resource<GetProductResponses?>>()
    val product: LiveData<Resource<GetProductResponses?>>
        get() = _product

    private val _searchResults = MutableLiveData<List<GetProduct>>()
    val searchResults: LiveData<List<GetProduct>> = _searchResults

    private val _cartItems = MutableLiveData<List<CartItem>>()
    val cartItems: LiveData<List<CartItem>> = _cartItems

    private val _checkoutStatus = MutableLiveData<Boolean>()
    val checkoutStatus: LiveData<Boolean> = _checkoutStatus

    private val _addTransaction: MutableLiveData<Resource<TransactionResponses>> = MutableLiveData()
    val addTransaction: LiveData<Resource<TransactionResponses>>
        get() = _addTransaction

    private var selectedProduct: Resource<GetProductByIdResponses>? = null
    private var selectedProductPriceProducts: Resource<ProductPricesResponses>? = null
    private var selectedProductPrices: List<ProductPrice> = emptyList()
    var currentCartItems = mutableListOf<CartItem>()


    fun getProducts() = viewModelScope.launch {
        _product.value = Resource.Loading
        _product.value = repository.getProduct()
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

    fun checkout(paymentStatus: String) = viewModelScope.launch {
        val productPriceIds = currentCartItems.map { it.selectedPrice.id }
        val priceAdjustments = currentCartItems.map { it.customPrice }
        val quantity = currentCartItems.map { it.quantity }
        try {
            val transaction = TransactionRequest(
                customerId,
                currentCartItems.sumOf { it.customPrice * it.quantity },
                productPriceIds,
                priceAdjustments,
                paymentStatus,
                quantity
            )
            val transactionResult = repository.addTransaction(transaction)
            _addTransaction.value = transactionResult
            _checkoutStatus.value = true
        } catch (e: Exception) {
            _checkoutStatus.value = false
        }
    }
}