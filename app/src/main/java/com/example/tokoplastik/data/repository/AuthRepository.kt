package com.example.tokoplastik.data.repository

import com.example.tokoplastik.data.UserPreferences
import com.example.tokoplastik.data.network.AuthApi

class AuthRepository (

    private val api: AuthApi,
    private val preferences: UserPreferences

): BaseRepository() {

    suspend fun login (
        email: String,
        password: String
    ) = safeApiCall {
        api.login(email, password)
    }

    suspend fun saveAuthToken(token: String) {
        preferences.saveAuthToken(token)
    }
}