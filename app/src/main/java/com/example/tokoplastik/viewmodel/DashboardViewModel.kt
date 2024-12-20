package com.example.tokoplastik.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.tokoplastik.data.repository.DashboardRepository
import com.example.tokoplastik.data.responses.DashboardResponses
import com.example.tokoplastik.data.responses.GetChartResponses
import com.example.tokoplastik.ui.base.BaseViewModel
import com.example.tokoplastik.util.Resource
import kotlinx.coroutines.launch

class DashboardViewModel (
    private val repository: DashboardRepository
) : BaseViewModel(repository) {

    private val _chart: MutableLiveData<Resource<GetChartResponses>> = MutableLiveData()
    val chart: LiveData<Resource<GetChartResponses>>
        get() = _chart

    private val _dashboardData: MutableLiveData<Resource<DashboardResponses>> = MutableLiveData()
    val dashboardData: LiveData<Resource<DashboardResponses>>
        get() = _dashboardData

    fun getChart() = viewModelScope.launch {
        _chart.value = Resource.Loading
        _chart.value = repository.getChart()
    }

    fun getChartMonthly() = viewModelScope.launch {
        _chart.value = Resource.Loading
        _chart.value = repository.getChartMonthly()
    }

    fun getDashboardData() = viewModelScope.launch {
        _dashboardData.value = Resource.Loading
        _dashboardData.value = repository.getDashboard()
    }
}