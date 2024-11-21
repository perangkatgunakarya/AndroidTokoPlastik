package com.example.tokoplastik.data.network

import com.example.tokoplastik.data.responses.AddProductPrices
import com.example.tokoplastik.data.responses.AddProductPricesResponses
import com.example.tokoplastik.data.responses.DeleteProductPricesResponses
import com.example.tokoplastik.data.responses.ProductPricesResponses
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface AddProductPricesApi {

    @GET("product-price")
    suspend fun getProductPrices(
        @Query("product_id") productId: Int
    ): ProductPricesResponses

    @POST("product-price")
    suspend fun addProductPrices(
        @Body productPrice: AddProductPrices
    ): AddProductPricesResponses

    @DELETE("product-price/{id}")
    suspend fun deleteProductPrices(
        @Path("id") id: Int
    ): DeleteProductPricesResponses
}