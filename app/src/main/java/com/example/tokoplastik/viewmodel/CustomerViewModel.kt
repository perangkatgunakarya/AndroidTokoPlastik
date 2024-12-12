package com.example.tokoplastik.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.tokoplastik.data.repository.CustomerRepository
import com.example.tokoplastik.data.responses.AddCustomerRequest
import com.example.tokoplastik.data.responses.AddCustomerResponses
import com.example.tokoplastik.data.responses.GetCustomerByIdResponses
import com.example.tokoplastik.data.responses.UpdateCustomerRequest
import com.example.tokoplastik.data.responses.UpdateCustomerResponses
import com.example.tokoplastik.ui.base.BaseViewModel
import com.example.tokoplastik.util.Resource
import kotlinx.coroutines.launch

class CustomerViewModel (
    private val repository: CustomerRepository
) : BaseViewModel(repository) {

    private val _addCustomer = MutableLiveData<Resource<AddCustomerResponses>>()
    val addCustomer: LiveData<Resource<AddCustomerResponses>>
        get() = _addCustomer

    private val _getCustomer = MutableLiveData<Resource<GetCustomerByIdResponses>>()
    val getCustomer: LiveData<Resource<GetCustomerByIdResponses>>
        get() = _getCustomer

    private val _updateCustomer = MutableLiveData<Resource<UpdateCustomerResponses>>()
    val updateCustomer: LiveData<Resource<UpdateCustomerResponses>>
        get() = _updateCustomer

    fun addCustomer(name: String, address: String, phoneNumber: String) = viewModelScope.launch {
        _addCustomer.value = Resource.Loading
        val customer = AddCustomerRequest(name, address, phoneNumber)
        _addCustomer.value = repository.addCustomer(customer)
    }

    fun getCustomerById(customerId: Int) = viewModelScope.launch {
        _getCustomer.value = Resource.Loading
        _getCustomer.value = repository.getCustomerById(customerId)
    }

    fun updateCustomer(customerId: Int, name: String?, address: String?, phoneNumber: String?) = viewModelScope.launch {
        _updateCustomer.value = Resource.Loading
        val customer = UpdateCustomerRequest(name, address, phoneNumber)
        _updateCustomer.value = repository.updateCustomer(customerId, customer)
    }
}