package com.example.tokoplastik.data.network

import com.example.tokoplastik.data.responses.GetChartResponses
import retrofit2.http.GET

interface DashboardApi {

    @GET("summary/daily")
    suspend fun getChart(): GetChartResponses

    @GET("summary/monthly")
    suspend fun getChartMonthly(): GetChartResponses
}