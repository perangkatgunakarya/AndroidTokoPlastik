package com.example.tokoplastik.data.responses

data class GetProductResponses(
    val data: List<GetProduct>,
    val message: String,
    val success: Boolean
)