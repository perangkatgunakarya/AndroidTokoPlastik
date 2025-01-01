package com.example.tokoplastik.data.network

import com.example.tokoplastik.data.responses.AddStockRequest
import com.example.tokoplastik.data.responses.AddStockResponses
import com.example.tokoplastik.data.responses.DeleteStockResponses
import com.example.tokoplastik.data.responses.GetProductByIdResponses
import com.example.tokoplastik.data.responses.GetStockResponses
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface StockApi {
    @GET("product/{id}")
    suspend fun getProduct(
        @Path("id") productId: Int
    ): GetProductByIdResponses

    @GET("stock-history")
    suspend fun getStock(
        @Query("product_id") productId: Int? = null,
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null
    ): GetStockResponses

    @POST("stock-history")
    suspend fun addStock(
        @Body request: AddStockRequest
    ): AddStockResponses

    @DELETE("stock-history/{id}")
    suspend fun deleteStock(
        @Path("id") id : Int
    ): DeleteStockResponses
}