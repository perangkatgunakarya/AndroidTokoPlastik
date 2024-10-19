package com.example.tokoplastik.responses

data class LoginResponse(
    val data: User,
    val message: String,
    val success: Boolean
)