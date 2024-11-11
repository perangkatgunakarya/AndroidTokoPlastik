package com.example.tokoplastik.data.responses

data class GetProductResponses(
    val data: List<GetProduct>,
    val message: String,
    val success: Boolean
)

data class GetProductByIdResponses(
    val data: GetProduct,
    val message: String,
    val success: Boolean,

    val prices: List<ProductPrice>
)

data class AddProductResponse(
    val data: GetProduct,
    val message: String,
    val success: Boolean
)