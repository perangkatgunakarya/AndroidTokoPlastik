package com.example.tokoplastik.data.responses

data class AllTransactionResponses(
    val data: List<AllTransaction>,
    val message: String,
    val success: Boolean
)

data class TransactionResponses(
    val data: Transaction,
    val message: String,
    val success: Boolean
)

data class TransactionDetailResponses(
    val data: TransactionDetail,
    val message: String,
    val success: Boolean
)

data class PaymentStatusUpdateResponses(
    val data: Transaction,
    val message: String,
    val success: Boolean
)

data class DeleteTransactionResponses(
    val data: List<Any>,
    val message: String,
    val success: Boolean
)