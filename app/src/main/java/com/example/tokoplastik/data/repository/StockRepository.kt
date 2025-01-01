package com.example.tokoplastik.data.repository

import com.example.tokoplastik.data.network.StockApi
import com.example.tokoplastik.data.responses.AddStockRequest

class StockRepository (
    private val api: StockApi
) : BaseRepository() {

    suspend fun getProduct(productId: Int) = safeApiCall {
        api.getProduct(productId)
    }

    suspend fun getStockByProductId(productId: Int? = null, startDate: String? = null, endDate: String? = null) = safeApiCall {
        api.getStock(productId, startDate, endDate)
    }

    suspend fun addStock(stock: AddStockRequest) = safeApiCall {
        api.addStock(stock)
    }

    suspend fun deleteStock(id: Int) = safeApiCall {
        api.deleteStock(id)
    }
}