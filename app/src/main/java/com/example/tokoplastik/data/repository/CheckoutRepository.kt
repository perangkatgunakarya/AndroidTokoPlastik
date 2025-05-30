package com.example.tokoplastik.data.repository

import com.example.tokoplastik.data.network.CheckoutApi
import com.example.tokoplastik.data.responses.PaymentStatusUpdateRequest
import com.example.tokoplastik.data.responses.TransactionRequest

class CheckoutRepository (
    private val api : CheckoutApi
) : BaseRepository () {

    suspend fun getProduct() = safeApiCall {
        api.getProduct()
    }

    suspend fun deleteTransaction(transactionId: Int) = safeApiCall {
        api.deleteTransaction(transactionId)
    }

    suspend fun getProductDetail(productId: Int) = safeApiCall {
        api.getProductDetail(productId)
    }

    suspend fun getAllProductPrices() = safeApiCall {
        api.getAllProductPrices()
    }

    suspend fun getProductPrices(productId: Int) = safeApiCall {
        api.getProductPrices(productId)
    }

    suspend fun addTransaction(transaction: TransactionRequest) = safeApiCall {
        api.addTransaction(transaction)
    }

    suspend fun getTransactions(startDate: String? = null, endDate: String? = null) = safeApiCall {
        api.getTransaction(startDate, endDate)
    }

    suspend fun getTransactionDetail(transactionId: Int) = safeApiCall {
        api.getTransactionDetail(transactionId)
    }

    suspend fun setPaymentStatus(transactionId: Int, statusUpdateRequest: PaymentStatusUpdateRequest) = safeApiCall {
        api.setPaymentStatus(transactionId, statusUpdateRequest)
    }
}