package com.example.tokoplastik.data.responses

import com.google.gson.annotations.SerializedName

data class Customer(
    val id: Int,
    val name: String,
    val address: String,
    val phone: String,
    val email: String,
    @SerializedName("merchant_name")
    val merchantName: String,
    @SerializedName("merchant_address")
    val merchantAddress: String,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String
)

data class AddCustomer(
    val name: String,
    val address: String,
    val phone: String,
    val email: String,
    @SerializedName("merchant_name")
    val merchantName: String,
    @SerializedName("merchant_address")
    val merchantAddress: String,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String
)

data class AddCustomerRequest(
    val name: String,
    val address: String,
    val phone: String,
)

data class UpdateCustomerRequest(
    val name: String?,
    val address: String?,
    val phone: String?,
)