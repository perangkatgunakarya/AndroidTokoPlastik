package com.example.tokoplastik.data.responses

import com.google.gson.annotations.SerializedName

data class Stock(
    val id: Int,
    @SerializedName("product_id")
    val productId: Int,
    val type: String,
    @SerializedName("latest_stock")
    val latestStock: Int,
    val quantity: Int,
    @SerializedName("current_stock")
    val currentStock: Int,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String,
    val product: GetProduct
)