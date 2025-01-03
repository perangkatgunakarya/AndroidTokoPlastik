package com.example.tokoplastik.data.responses

import com.google.gson.annotations.SerializedName

data class GetProduct(
    @SerializedName("capital_price")
    val capitalPrice: Int,
    @SerializedName("newest_capital_price")
    val newestCapitalPrice: Int,
    @SerializedName("lowest_unit")
    val lowestUnit: String,
    val notes: String,
    val latestStock: Int,
    @SerializedName("created_at")
    val createdAt: String,
    val id: Int,
    val name: String,
    val supplier: String,
    @SerializedName("updated_at")
    val updatedAt: String
)

data class UpdateProductRequest(
    val name: String?,
    val supplier: String?,
    @SerializedName("capital_price")
    val capitalPrice: Int?,
    @SerializedName("newest_capital_price")
    val newestCapitalPrice: Int?,
    @SerializedName("lowest_unit")
    val lowestUnit: String?,
    val notes: String?
)