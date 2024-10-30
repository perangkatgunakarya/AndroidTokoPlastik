package com.example.tokoplastik.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.tokoplastik.data.repository.AddProductRepository
import com.example.tokoplastik.data.responses.AddProduct
import com.example.tokoplastik.data.responses.AddProductResponse
import com.example.tokoplastik.ui.base.BaseViewModel
import com.example.tokoplastik.util.Resource
import kotlinx.coroutines.launch

class AddProductViewModel(
    val repository: AddProductRepository
) : BaseViewModel(repository) {

    private val _addProduct: MutableLiveData<Resource<AddProductResponse>> = MutableLiveData()
    val addProduct: LiveData<Resource<AddProductResponse>>
        get() = _addProduct

    fun addProduct(name: String, supplier: String, capitalPrice: String) = viewModelScope.launch {
        _addProduct.value = Resource.Loading
        val product = AddProduct(name, supplier, capitalPrice)
        _addProduct.value = repository.addProduct(product)
    }
}