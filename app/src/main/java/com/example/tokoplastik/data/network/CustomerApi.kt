package com.example.tokoplastik.data.network

import com.example.tokoplastik.data.responses.GetCustomerResponses
import retrofit2.http.GET

interface CustomerApi {

    @GET("customer")
    suspend fun getCustomer(): GetCustomerResponses
}