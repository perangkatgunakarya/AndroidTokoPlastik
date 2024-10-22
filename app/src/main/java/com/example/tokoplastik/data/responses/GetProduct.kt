package com.example.tokoplastik.data.responses

data class GetProduct(
    val capitalPrice: String,
    val createdAt: String,
    val id: Int,
    val name: String,
    val supplier: String,
    val updatedAt: String
)