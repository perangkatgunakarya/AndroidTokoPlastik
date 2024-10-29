package com.example.tokoplastik.data.responses

import com.google.gson.annotations.SerializedName

data class GetProduct(
    @SerializedName("capital_price")
    val capitalPrice: String,
    @SerializedName("created_at")
    val createdAt: String,
    val id: Int,
    val name: String,
    val supplier: String,
    @SerializedName("updated_at")
    val updatedAt: String
)