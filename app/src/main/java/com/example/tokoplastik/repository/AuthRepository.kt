package com.example.tokoplastik.repository

import com.example.tokoplastik.network.AuthApi

class AuthRepository (

    private val api: AuthApi

): BaseRepository () {

    suspend fun login (
        email: String,
        password: String
    ) = safeApiCall {
        api.login(email, password)
    }
}