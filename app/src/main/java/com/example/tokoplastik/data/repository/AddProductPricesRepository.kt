package com.example.tokoplastik.data.repository

import com.example.tokoplastik.data.network.AddProductPricesApi
import com.example.tokoplastik.data.responses.AddProductPrices
class AddProductPricesRepository (
    private val api: AddProductPricesApi
) : BaseRepository () {

    suspend fun getProductPrices(productId: Int) = safeApiCall {
        api.getProductPrices(productId)
    }

    suspend fun addProductPrices(product: AddProductPrices) = safeApiCall {
        api.addProductPrices(AddProductPrices(product.productId, product.price, product.unit, product.quantityPerUnit))
    }
}