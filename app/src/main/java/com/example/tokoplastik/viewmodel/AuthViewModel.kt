package com.example.tokoplastik.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tokoplastik.data.repository.AuthRepository
import com.example.tokoplastik.data.responses.LoginResponse
import com.example.tokoplastik.util.Resource
import kotlinx.coroutines.launch

class AuthViewModel (
   private val repository: AuthRepository
) : ViewModel() {

    private val _loginResponse : MutableLiveData<Resource<LoginResponse>> = MutableLiveData()
    val loginResponses: LiveData<Resource<LoginResponse>>
        get() = _loginResponse

    fun login (
        email: String,
        password: String
    ) = viewModelScope.launch {
        _loginResponse.value = repository.login(email, password)
    }
}