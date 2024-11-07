package com.example.tokoplastik.data.repository

import com.example.tokoplastik.data.network.CustomerApi

class CustomerRepository (
    private val api : CustomerApi
) : BaseRepository () {

    suspend fun getCustomer () = safeApiCall {
        api.getCustomer()
    }
}