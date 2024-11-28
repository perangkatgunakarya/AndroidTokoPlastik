package com.example.tokoplastik.data.network

import com.example.tokoplastik.data.responses.AddCustomerRequest
import com.example.tokoplastik.data.responses.AddCustomerResponses
import com.example.tokoplastik.data.responses.GetCustomerByIdResponses
import com.example.tokoplastik.data.responses.GetCustomerResponses
import com.example.tokoplastik.data.responses.UpdateCustomerRequest
import com.example.tokoplastik.data.responses.UpdateCustomerResponses
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface CustomerApi {

    @GET("customer")
    suspend fun getCustomer(): GetCustomerResponses

    @POST("customer")
    suspend fun addCustomer(
        @Body customer: AddCustomerRequest
    ): AddCustomerResponses

    @GET("customer/{id}")
    suspend fun getCustomerById(
        @Path("id") customerId: Int
    ): GetCustomerByIdResponses

    @PUT("customer/{id}")
    suspend fun updateCustomer(
        @Path("id") customerId: Int,
        @Body customer: UpdateCustomerRequest?
    ): UpdateCustomerResponses
}