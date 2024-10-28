package com.example.tokoplastik.data.repository

import com.example.tokoplastik.data.network.GetProductApi

class ProductRepository (

    private val api: GetProductApi
): BaseRepository() {

    suspend fun getProduct () = safeApiCall {
        api.getProduct()
    }
}