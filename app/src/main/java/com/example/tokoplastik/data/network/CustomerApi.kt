package com.example.tokoplastik.data.network

import com.example.tokoplastik.data.responses.AddCustomerRequest
import com.example.tokoplastik.data.responses.AddCustomerResponses
import com.example.tokoplastik.data.responses.GetCustomerResponses
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface CustomerApi {

    @GET("customer")
    suspend fun getCustomer(): GetCustomerResponses

    @POST("customer")
    suspend fun addCustomer(
        @Body customer: AddCustomerRequest
    ): AddCustomerResponses
}