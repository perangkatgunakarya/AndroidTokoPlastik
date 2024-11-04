package com.example.tokoplastik.data.responses

data class AddProductPricesResponses(
    val data: ProductPrice,
    val message: String,
    val success: Boolean
)


data class ProductPricesResponses(
    val data: List<ProductPrice>,
    val message: String,
    val success: Boolean
)