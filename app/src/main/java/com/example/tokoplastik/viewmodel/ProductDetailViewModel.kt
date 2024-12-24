package com.example.tokoplastik.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.tokoplastik.data.repository.ProductRepository
import com.example.tokoplastik.data.responses.GetProduct
import com.example.tokoplastik.data.responses.GetProductByIdResponses
import com.example.tokoplastik.data.responses.UpdateProductRequest
import com.example.tokoplastik.ui.base.BaseViewModel
import com.example.tokoplastik.util.Resource
import kotlinx.coroutines.launch

class ProductDetailViewModel(
    private val repository: ProductRepository
) : BaseViewModel(repository) {

    private val _productDetail: MutableLiveData<Resource<GetProductByIdResponses>> = MutableLiveData()
    val productDetail: LiveData<Resource<GetProductByIdResponses>> = _productDetail

    private val _updateProduct: MutableLiveData<Resource<GetProductByIdResponses>> = MutableLiveData()
    val updateProduct: LiveData<Resource<GetProductByIdResponses>> = _updateProduct

    fun getProductDetail(productId: Int) {
        viewModelScope.launch {
            _productDetail.value = Resource.Loading
            _productDetail.value = repository.getProductDetail(productId)

        }
    }

    fun updateProduct(productId: Int, name: String?, supplier: String?, capitalPrice: Int?, newestCapitalPrice: Int?, lowestUnit: String?) {
        viewModelScope.launch {
            _updateProduct.value = Resource.Loading
            val product = UpdateProductRequest(name, supplier, capitalPrice, newestCapitalPrice, lowestUnit)
            _updateProduct.value = repository.updateProduct(productId, product)
        }
    }
}