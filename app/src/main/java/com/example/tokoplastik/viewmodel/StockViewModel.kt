package com.example.tokoplastik.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.tokoplastik.data.repository.StockRepository
import com.example.tokoplastik.data.responses.AddStockRequest
import com.example.tokoplastik.data.responses.AddStockResponses
import com.example.tokoplastik.data.responses.DeleteStockResponses
import com.example.tokoplastik.data.responses.GetProductByIdResponses
import com.example.tokoplastik.data.responses.GetStockResponses
import com.example.tokoplastik.ui.base.BaseViewModel
import com.example.tokoplastik.util.Resource
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class StockViewModel (
    private val repository: StockRepository
) : BaseViewModel(repository) {

    private val _product = MutableLiveData<Resource<GetProductByIdResponses>>()
    val product: LiveData<Resource<GetProductByIdResponses>>
        get() = _product

    private val _stock = MutableLiveData<Resource<GetStockResponses>>()
    val stock: LiveData<Resource<GetStockResponses>>
        get() = _stock

    private val _addStock = MutableLiveData<Resource<AddStockResponses>>()
    val addStock: LiveData<Resource<AddStockResponses>>
        get() = _addStock

    private val _addStockStatus = MutableLiveData<Boolean>()
    val addStockStatus : LiveData<Boolean>
        get() = _addStockStatus

    private val _deleteStock = MutableLiveData<Resource<DeleteStockResponses>>()
    val deleteStock: LiveData<Resource<DeleteStockResponses>>
        get() = _deleteStock

    fun getStockByProductId(productId: Int? = null, startDate: String? = null, endDate: String? = null) = viewModelScope.launch {
        _stock.value = Resource.Loading
        _stock.value = repository.getStockByProductId(productId, startDate, endDate)
    }

    fun getProduct(productId: Int) = viewModelScope.launch {
        _product.value = Resource.Loading
        _product.value = repository.getProduct(productId)
    }

    fun addStock(productId: Int, type: String, quantity: Int) = viewModelScope.launch {
        _addStock.value = Resource.Loading
        val stock = AddStockRequest(productId, type, quantity)
        _addStock.value = repository.addStock(stock)
    }

    fun addStockStatus(status: Boolean) {
        _addStockStatus.value = status
    }

    fun deleteStock(id: Int) = viewModelScope.launch {
        _deleteStock.value = Resource.Loading
        _deleteStock.value = repository.deleteStock(id)
    }

    // sort and filter
    var productId: Int? = null

    private val _sortType = MutableLiveData(SortType.DATE)
    val sortType: LiveData<SortType> = _sortType

    private val _sortOrder = MutableLiveData(SortOrder.DESCENDING)
    val sortOrder: LiveData<SortOrder> = _sortOrder

    private val _isDateSortAscending = MutableLiveData(false)
    val isDateSortAscending: LiveData<Boolean> = _isDateSortAscending

    private val _startDate = MutableLiveData<Long>()
    val startDate: LiveData<Long> = _startDate

    private val _endDate = MutableLiveData<Long>()
    val endDate: LiveData<Long> = _endDate

    fun setSortType(type: SortType) {
        _sortType.value = type
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun setDateRange(start: Long, end: Long) {
        _startDate.value = start
        _endDate.value = end
    }

    fun applyFilters() = viewModelScope.launch {
        _stock.value = Resource.Loading
        _stock.value = repository.getStockByProductId(
            productId,
            startDate.value?.let { formatDate(it) },
            endDate.value?.let { formatDate(it) })

        _isDateSortAscending.value = sortOrder.value == SortOrder.ASCENDING
    }

    private fun formatDate(timestamp: Long): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp))
    }
}