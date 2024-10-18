package com.example.tokoplastik.data

data class User(val username: String, val email: String) {
    constructor(): this("", "")
}
