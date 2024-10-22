package com.example.tokoplastik.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.tokoplastik.data.repository.ProductRepository
import com.example.tokoplastik.data.responses.GetProductResponses
import com.example.tokoplastik.ui.base.BaseViewModel
import com.example.tokoplastik.util.Resource
import kotlinx.coroutines.launch

class HomeViewModel (
    private val repository: ProductRepository
) : BaseViewModel(repository) {

    private val _product: MutableLiveData<Resource<GetProductResponses>> = MutableLiveData()
    val product: LiveData<Resource<GetProductResponses>>
        get() = _product

    fun getProduct() = viewModelScope.launch {
        _product.value = Resource.Loading
        _product.value = repository.getProduct()
    }
}