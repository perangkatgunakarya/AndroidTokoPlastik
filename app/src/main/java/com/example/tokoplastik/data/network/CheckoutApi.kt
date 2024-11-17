package com.example.tokoplastik.data.network

import com.example.tokoplastik.data.responses.GetProductByIdResponses
import com.example.tokoplastik.data.responses.GetProductResponses
import com.example.tokoplastik.data.responses.ProductPricesResponses
import com.example.tokoplastik.data.responses.TransactionRequest
import com.example.tokoplastik.data.responses.TransactionResponses
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface CheckoutApi {

    @GET("product")
    suspend fun getProduct(): GetProductResponses

    @GET("product/{id}")
    suspend fun getProductDetail(
        @Path("id") productId: Int
    ): GetProductByIdResponses

    @GET("product-price")
    suspend fun getProductPrices(
        @Query("product_id") productId: Int
    ): ProductPricesResponses

    @POST("transaction")
    suspend fun addTransaction(
        @Body transaction: TransactionRequest
    ): TransactionResponses
}