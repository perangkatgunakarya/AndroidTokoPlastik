package com.example.tokoplastik.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.tokoplastik.data.repository.AddProductPricesRepository
import com.example.tokoplastik.data.responses.AddProductPrices
import com.example.tokoplastik.data.responses.AddProductPricesResponses
import com.example.tokoplastik.ui.base.BaseViewModel
import com.example.tokoplastik.util.Resource
import kotlinx.coroutines.launch

class AddProductPricesViewModel(
    private val repository: AddProductPricesRepository
) : BaseViewModel(repository) {

    private val _addProductPrices: MutableLiveData<Resource<AddProductPricesResponses>> = MutableLiveData()
    val addProductPrices: LiveData<Resource<AddProductPricesResponses>>
        get() = _addProductPrices

    private val _productPrices: MutableLiveData<Resource<AddProductPricesResponses>> = MutableLiveData()
    val productPrices: LiveData<Resource<AddProductPricesResponses>> = _productPrices

    fun addProductPrices(productId: Int, price: Int, unit: String, quantityPerUnit: String) = viewModelScope.launch {
        _addProductPrices.value = Resource.Loading
        val product = AddProductPrices(productId, price, unit, quantityPerUnit)
        _addProductPrices.value = repository.addProductPrices(product)
    }

    fun getProductPrices(productId: Int) {
        viewModelScope.launch {
            _productPrices.value = Resource.Loading
            _productPrices.value = repository.getProductPrices(productId)
        }
    }
}