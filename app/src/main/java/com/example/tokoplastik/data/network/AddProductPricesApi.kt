package com.example.tokoplastik.data.network

import com.example.tokoplastik.data.responses.AddProductPrices
import com.example.tokoplastik.data.responses.AddProductPricesResponses
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface AddProductPricesApi {

    @GET("product-price/{productId}")
    suspend fun getProductPrices(
        @Path("productId") productId: Int
    ): AddProductPricesResponses

    @POST("product-price")
    suspend fun addProductPrices(
        @Body productPrice: AddProductPrices
    ): AddProductPricesResponses
}