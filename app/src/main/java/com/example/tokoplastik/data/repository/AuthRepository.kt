package com.example.tokoplastik.data.repository

import com.example.tokoplastik.data.network.AuthApi

class AuthRepository (

    private val api: AuthApi

): BaseRepository() {

    suspend fun login (
        email: String,
        password: String
    ) = safeApiCall {
        api.login(email, password)
    }
}