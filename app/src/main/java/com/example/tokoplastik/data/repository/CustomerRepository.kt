package com.example.tokoplastik.data.repository

import com.example.tokoplastik.data.network.CustomerApi
import com.example.tokoplastik.data.responses.AddCustomerRequest

class CustomerRepository (
    private val api : CustomerApi
) : BaseRepository () {

    suspend fun getCustomer () = safeApiCall {
        api.getCustomer()
    }

    suspend fun addCustomer (customer: AddCustomerRequest) = safeApiCall {
        api.addCustomer(customer)
    }
}