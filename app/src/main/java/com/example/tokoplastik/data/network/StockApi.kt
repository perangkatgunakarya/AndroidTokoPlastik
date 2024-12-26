package com.example.tokoplastik.data.network

import com.example.tokoplastik.data.responses.DeleteStockResponses
import com.example.tokoplastik.data.responses.GetStockResponses
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface StockApi {
    @GET("stock-history")
    suspend fun getStock(
        @Query("product_id") productId: Int? = null,
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null
    ): GetStockResponses

    @DELETE("stock-history/{id}")
    suspend fun deleteStock(
        @Path("id") id : Int
    ): DeleteStockResponses
}