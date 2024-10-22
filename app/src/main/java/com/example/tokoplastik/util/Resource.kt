package com.example.tokoplastik.util

import okhttp3.ResponseBody

sealed class Resource<out T>(
    val data: T?=null,
    val message: String?=null
) {
    class Success<T>(data: T): Resource<T>(data)
    object Loading: Resource<Nothing>()
    data class Failure(
        val isNetworkError: Boolean,
        val errorCode: Int?,
        val errorBody: ResponseBody?
    ) : Resource<Nothing> ()
}