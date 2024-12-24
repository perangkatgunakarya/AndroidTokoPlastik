package com.example.tokoplastik.data.network

import com.example.tokoplastik.data.responses.AddProductResponse
import com.example.tokoplastik.data.responses.DeleteProductResponse
import com.example.tokoplastik.data.responses.GetProduct
import com.example.tokoplastik.data.responses.GetProductByIdResponses
import com.example.tokoplastik.data.responses.GetProductResponses
import com.example.tokoplastik.data.responses.UpdateProductRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

interface GetProductApi {

    @GET("product")
    suspend fun getProduct(): GetProductResponses

    @GET("product/{id}")
    suspend fun getProductDetail(
        @Path("id") productId: Int
    ): GetProductByIdResponses

    @PUT("product/{id}")
    suspend fun updateProduct(
        @Path("id") productId: Int,
        @Body request: UpdateProductRequest?
    ): GetProductByIdResponses

    @DELETE("product/{id}")
    suspend fun deleteProduct(
        @Path("id") id: Int
    ): DeleteProductResponse
}