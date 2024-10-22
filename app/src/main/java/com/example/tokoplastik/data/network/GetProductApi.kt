package com.example.tokoplastik.data.network

import com.example.tokoplastik.data.responses.GetProductResponses
import retrofit2.http.GET

interface GetProductApi {

    @GET("product")
    suspend fun getProduct(): GetProductResponses
}