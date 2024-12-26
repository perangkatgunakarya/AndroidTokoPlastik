package com.example.tokoplastik.data.repository

import com.example.tokoplastik.data.network.StockApi

class StockRepository (
    private val api: StockApi
) : BaseRepository() {

    suspend fun getStockByProductId(productId: Int? = null, startDate: String? = null, endDate: String? = null) = safeApiCall {
        api.getStock(productId, startDate, endDate)
    }

    suspend fun deleteStock(id: Int) = safeApiCall {
        api.deleteStock(id)
    }
}