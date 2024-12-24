package com.example.tokoplastik.data.responses

data class GetProductResponses(
    val data: List<GetProduct>,
    val message: String,
    val success: Boolean
)

data class GetProductByIdResponses(
    val data: Product,
    val message: String,
    val success: Boolean,
)

data class Product(
    val product: GetProduct
)

data class AddProductResponse(
    val data: GetProduct,
    val message: String,
    val success: Boolean
)

data class DeleteProductResponse(
    val data: List<Any>,
    val message: String,
    val success: Boolean
)