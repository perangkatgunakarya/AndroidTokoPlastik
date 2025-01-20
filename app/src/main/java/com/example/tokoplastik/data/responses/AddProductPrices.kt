package com.example.tokoplastik.data.responses

import com.google.gson.annotations.SerializedName

data class AddProductPrices(
    @SerializedName("product_id")
    val productId: Int,
    val price: Int,
    val unit: String,
    @SerializedName("quantity_per_lowest_unit")
    val quantityPerUnit: String,
)

data class ProductPrice(
    @SerializedName("created_at")
    val createdAt: String,
    val id: Int,
    val price: Int,
    val product: GetProduct,
    @SerializedName("product_id")
    val productId: Int,
    @SerializedName("quantity_per_lowest_unit")
    val quantityPerUnit: String,
    val unit: String,
    @SerializedName("updated_at")
    val updatedAt: String
)