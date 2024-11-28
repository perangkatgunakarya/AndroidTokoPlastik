package com.example.tokoplastik.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.tokoplastik.data.repository.CustomerRepository
import com.example.tokoplastik.data.responses.AddCustomerRequest
import com.example.tokoplastik.data.responses.AddCustomerResponses
import com.example.tokoplastik.ui.base.BaseViewModel
import com.example.tokoplastik.util.Resource
import kotlinx.coroutines.launch

class CustomerViewModel (
    private val repository: CustomerRepository
) : BaseViewModel(repository) {

    private val _addCustomer = MutableLiveData<Resource<AddCustomerResponses>>()
    val addCustomer: LiveData<Resource<AddCustomerResponses>>
        get() = _addCustomer

    fun addCustomer(name: String, address: String, phoneNumber: String) = viewModelScope.launch {
        _addCustomer.value = Resource.Loading
        val customer = AddCustomerRequest(name, address, phoneNumber)
        _addCustomer.value = repository.addCustomer(customer)
    }
}