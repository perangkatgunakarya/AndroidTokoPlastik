package com.example.tokoplastik.data.repository

import com.example.tokoplastik.data.network.AddProductApi
import com.example.tokoplastik.data.responses.AddProduct

class AddProductRepository (
    private val api: AddProductApi
) : BaseRepository() {

    suspend fun addProduct(product: AddProduct) = safeApiCall {
        api.addProduct(AddProduct(product.name, product.supplier, product.capitalPrice))
    }
}