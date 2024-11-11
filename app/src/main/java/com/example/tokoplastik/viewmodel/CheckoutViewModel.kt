package com.example.tokoplastik.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.tokoplastik.data.repository.CheckoutRepository
import com.example.tokoplastik.data.responses.CartItem
import com.example.tokoplastik.data.responses.GetProduct
import com.example.tokoplastik.data.responses.GetProductByIdResponses
import com.example.tokoplastik.data.responses.GetProductResponses
import com.example.tokoplastik.data.responses.ProductPrice
import com.example.tokoplastik.ui.base.BaseViewModel
import com.example.tokoplastik.util.Resource
import kotlinx.coroutines.launch

class CheckoutViewModel (
    private val repository: CheckoutRepository
) : BaseViewModel(repository) {

    var customerId: Int? = null

    private val _product = MutableLiveData<Resource<GetProductResponses?>>()
    val product: LiveData<Resource<GetProductResponses?>>
        get() = _product

    private val _searchResults = MutableLiveData<List<GetProduct>>()
    val searchResults: LiveData<List<GetProduct>> = _searchResults

    private val _cartItems = MutableLiveData<List<CartItem>>()
    val cartItems: LiveData<List<CartItem>> = _cartItems

    private val _checkoutStatus = MutableLiveData<Boolean>()
    val checkoutStatus: LiveData<Boolean> = _checkoutStatus

    private var selectedProduct: Resource<GetProductByIdResponses>? = null
    private var selectedProductPrices: List<ProductPrice> = emptyList()
    private var currentCartItems = mutableListOf<CartItem>()


    fun getProducts() = viewModelScope.launch {
        _product.value = Resource.Loading
        _product.value = repository.getProduct()
    }

    fun selectProduct(position: Int) {
        viewModelScope.launch {
            selectedProduct = repository.getProductDetail(position)
            selectedProductPrices = selectedProduct?.data?.prices ?: emptyList()
        }
    }

    fun addSelectedProductToCart() {
        val product = selectedProduct
        if (product != null && selectedProductPrices.isNotEmpty()) {
            val defaultPrice = selectedProductPrices.first()
            val cartItem = CartItem(
                product = product,
                productPrice = selectedProductPrices,
                selectedPrice = defaultPrice,
                quantity = 1,
                customPrice = defaultPrice.price
            )
            currentCartItems.add(cartItem)
            _cartItems.value = currentCartItems.toList()

            // Reset selection
            selectedProduct = null
            selectedProductPrices = emptyList()
        }

        Log.i("CheckoutViewModel", "Cart items: $cartItems")
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

    fun checkout() {
        viewModelScope.launch {
            Log.i("CheckoutViewModel", "Checkout started with customerId: $customerId and cartItems: $currentCartItems and total price: ${currentCartItems.sumOf { it.customPrice * it.quantity }}")
//            try {
//                // Create transaction first
//                val transactionId = repository.createTransaction(
//                    customerId = customerId,
//                    userId = getCurrentUserId(), // Implement this to get logged in user ID
//                    total = currentCartItems.sumOf { it.customPrice * it.quantity }
//                )
//
//                // Add all transaction products
//                currentCartItems.forEach { item ->
//                    repository.addTransactionProduct(
//                        transactionId = transactionId,
//                        productPriceId = item.selectedPrice.id,
//                        priceAdjustment = item.customPrice - item.selectedPrice.price
//                    )
//                }
//
//                _checkoutStatus.value = true
//            } catch (e: Exception) {
//                _checkoutStatus.value = false
//                // Handle error
//            }
        }
    }
}