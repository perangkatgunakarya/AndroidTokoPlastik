package com.example.tokoplastik.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.tokoplastik.data.repository.CustomerRepository
import com.example.tokoplastik.data.responses.GetCustomerResponses
import com.example.tokoplastik.ui.base.BaseViewModel
import com.example.tokoplastik.util.Resource
import kotlinx.coroutines.launch

class TransactionViewModel (
    private val repository: CustomerRepository
) : BaseViewModel(repository) {

    private val _transaction: MutableLiveData<Resource<GetCustomerResponses>> = MutableLiveData()
    val transaction: LiveData<Resource<GetCustomerResponses>>
        get() = _transaction

    fun getTransaction() = viewModelScope.launch {
        _transaction.value = Resource.Loading
        _transaction.value = repository.getCustomer()
    }
}