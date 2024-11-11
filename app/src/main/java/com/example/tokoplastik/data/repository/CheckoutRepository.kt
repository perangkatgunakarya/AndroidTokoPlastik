package com.example.tokoplastik.data.repository

import com.example.tokoplastik.data.network.CheckoutApi

class CheckoutRepository (
    private val api : CheckoutApi
) : BaseRepository () {

    suspend fun getProduct() = safeApiCall {
        api.getProduct()
    }

    suspend fun getProductDetail(productId: Int) = safeApiCall {
        api.getProductDetail(productId)
    }

    suspend fun getProductPrices(productId: Int) = safeApiCall {
        api.getProductPrices(productId)
    }
}