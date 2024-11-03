package com.example.tokoplastik.data.responses

data class AddProductPricesResponses(
    val data: List<ProductPrice>,
    val message: String,
    val success: Boolean
)