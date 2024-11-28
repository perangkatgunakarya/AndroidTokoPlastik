package com.example.tokoplastik.data.repository

import com.example.tokoplastik.data.network.CustomerApi
import com.example.tokoplastik.data.responses.AddCustomerRequest
import com.example.tokoplastik.data.responses.UpdateCustomerRequest

class CustomerRepository (
    private val api : CustomerApi
) : BaseRepository () {

    suspend fun getCustomer () = safeApiCall {
        api.getCustomer()
    }

    suspend fun addCustomer (customer: AddCustomerRequest) = safeApiCall {
        api.addCustomer(customer)
    }

    suspend fun getCustomerById (customerId: Int) = safeApiCall {
        api.getCustomerById(customerId)
    }

    suspend fun updateCustomer (customerId: Int, customer: UpdateCustomerRequest?) = safeApiCall {
        api.updateCustomer(customerId, customer)
    }
}