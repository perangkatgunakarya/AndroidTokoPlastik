package com.example.tokoplastik.data.responses

import com.google.gson.annotations.SerializedName

data class AddProduct(
    val name: String,
    val supplier: String,
    @SerializedName("capital_price")
    val capitalPrice: String,
    @SerializedName("newest_capital_price")
    val newestCapitalPrice: String,
    @SerializedName("lowest_unit")
    val lowesUnit: String
)
