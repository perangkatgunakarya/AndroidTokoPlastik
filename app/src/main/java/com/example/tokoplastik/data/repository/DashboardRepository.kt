package com.example.tokoplastik.data.repository

import com.example.tokoplastik.data.network.DashboardApi

class DashboardRepository (
    private val api : DashboardApi
) : BaseRepository () {

    suspend fun getChart () = safeApiCall {
        api.getChart()
    }

    suspend fun getChartMonthly () = safeApiCall {
        api.getChartMonthly()
    }

    suspend fun getDashboard () = safeApiCall {
        api.getDashboard()
    }
}