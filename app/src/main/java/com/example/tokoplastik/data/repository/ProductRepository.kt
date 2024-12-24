package com.example.tokoplastik.data.repository

import com.example.tokoplastik.data.network.GetProductApi
import com.example.tokoplastik.data.responses.GetProduct
import com.example.tokoplastik.data.responses.UpdateProductRequest

class ProductRepository (

    private val api: GetProductApi
): BaseRepository() {

    suspend fun getProduct () = safeApiCall {
        api.getProduct()
    }

    suspend fun getProductDetail(productId: Int) = safeApiCall {
        api.getProductDetail(productId)
    }

    suspend fun updateProduct(productId: Int, product: UpdateProductRequest) = safeApiCall {
        api.updateProduct(productId, product)
    }

    suspend fun deleteProduct(productId: Int) = safeApiCall {
        api.deleteProduct(productId)
    }
}