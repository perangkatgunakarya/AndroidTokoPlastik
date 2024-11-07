package com.example.tokoplastik.data.responses

data class GetCustomerResponses(
    val data: List<Customer>,
    val message: String,
    val success: Boolean
)