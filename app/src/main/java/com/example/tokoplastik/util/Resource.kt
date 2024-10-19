package com.example.tokoplastik.util

import okhttp3.ResponseBody

sealed class Resource<out T>(
    val data: T?=null,
    val message: String?=null
) {
    class Success<T>(data: T): Resource<T>(data)
    class Error<T>(message: String): Resource<T>(message = message)
    class Loading<T>: Resource<T>()
    data class Failure(
        val isNetworkError: Boolean,
        val errorCode: Int?,
        val errorBody: ResponseBody?
    ) : Resource<Nothing> ()
}