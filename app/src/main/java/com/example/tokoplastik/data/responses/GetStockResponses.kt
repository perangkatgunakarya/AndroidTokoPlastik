package com.example.tokoplastik.data.responses

data class GetStockResponses(
    val data: List<Stock>,
    val message: String,
    val success: Boolean
)

data class DeleteStockResponses(
    val data: List<Any>,
    val message: String,
    val success: Boolean
)

data class AddStockResponses(
    val data: Stock,
    val message: String,
    val success: Boolean
)