package com.example.tokoplastik.data.responses

data class GetCustomerResponses(
    val data: List<Customer>,
    val message: String,
    val success: Boolean
)

data class AddCustomerResponses(
    val data: AddCustomer,
    val message: String,
    val success: Boolean
)

data class GetCustomerByIdResponses(
    val data: Customer,
    val message: String,
    val success: Boolean
)

data class UpdateCustomerResponses(
    val data: Customer,
    val message: String,
    val success: Boolean
)