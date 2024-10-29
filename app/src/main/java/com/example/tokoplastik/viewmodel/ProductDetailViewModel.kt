package com.example.tokoplastik.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.tokoplastik.data.repository.ProductRepository
import com.example.tokoplastik.data.responses.GetProduct
import com.example.tokoplastik.data.responses.GetProductByIdResponses
import com.example.tokoplastik.ui.base.BaseViewModel
import com.example.tokoplastik.util.Resource
import kotlinx.coroutines.launch

class ProductDetailViewModel(
    private val repository: ProductRepository
) : BaseViewModel(repository) {

    private val _productDetail: MutableLiveData<Resource<GetProductByIdResponses>> = MutableLiveData()
    val productDetail: LiveData<Resource<GetProductByIdResponses>> = _productDetail

    fun getProductDetail(productId: Int) {
        viewModelScope.launch {
            _productDetail.value = Resource.Loading
            _productDetail.value = repository.getProductDetail(productId)

        }
    }
}