package com.example.tokoplastik.data.responses

data class LoginResponse(
    val data: User,
    val message: String,
    val success: Boolean
)