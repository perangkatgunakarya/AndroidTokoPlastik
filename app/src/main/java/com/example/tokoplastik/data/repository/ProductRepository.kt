package com.example.tokoplastik.data.repository

import com.example.tokoplastik.data.network.GetProductApi
import com.example.tokoplastik.data.responses.GetProduct

class ProductRepository (

    private val api: GetProductApi
): BaseRepository() {

    suspend fun getProduct () = safeApiCall {
        api.getProduct()
    }

    suspend fun getProductDetail(productId: Int) = safeApiCall {
        api.getProductDetail(productId)
    }
}