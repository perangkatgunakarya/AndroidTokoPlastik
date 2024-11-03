package com.example.tokoplastik.viewmodel

import android.util.Log
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
        _addProductPrices.postValue(Resource.Loading)
        val product = AddProductPrices(productId, price, unit, quantityPerUnit)
        val result = repository.addProductPrices(product)
        _addProductPrices.postValue(result)
        Log.d("ViewModel", "Product price added: $result")
        getProductPrices(productId)
    }

    fun getProductPrices(productId: Int) {
        viewModelScope.launch {
            _productPrices.value = Resource.Loading
            _productPrices.value = repository.getProductPrices(productId)
            Log.d("ViewModel", "Fetched new data: ${_productPrices.value} items")
        }
    }
}