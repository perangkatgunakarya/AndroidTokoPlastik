package com.example.tokoplastik.data.responses

import com.google.gson.annotations.SerializedName

data class Customer(
    val address: String,
    @SerializedName("created_at")
    val createdAt: String,
    val email: String,
    val id: Int,
    @SerializedName("merchant_address")
    val merchantAddress: String,
    @SerializedName("merchant_name")
    val merchantName: String,
    val name: String,
    val phone: String,
    @SerializedName("updated_at")
    val updatedAt: String
)

data class CustomerSpinnerItem(
    val name: String,
)