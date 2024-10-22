package com.example.tokoplastik.ui.base

import androidx.lifecycle.ViewModel
import com.example.tokoplastik.data.network.AuthApi
import com.example.tokoplastik.data.repository.BaseRepository

open class BaseViewModel (
    private val repository: BaseRepository
) : ViewModel() {

    suspend fun logout (api: AuthApi) = repository.logout(api)
}