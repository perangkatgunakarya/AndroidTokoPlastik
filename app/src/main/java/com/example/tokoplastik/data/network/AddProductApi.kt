package com.example.tokoplastik.data.network

import com.example.tokoplastik.data.responses.AddProduct
import com.example.tokoplastik.data.responses.AddProductResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface AddProductApi {

    @POST("product")
    suspend fun addProduct(
        @Body request: AddProduct
    ): AddProductResponse
}