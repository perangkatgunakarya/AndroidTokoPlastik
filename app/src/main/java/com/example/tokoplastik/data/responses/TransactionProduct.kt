package com.example.tokoplastik.data.responses

import com.google.gson.annotations.SerializedName

data class AllTransaction(
    val id: Int,
    @SerializedName("user_id")
    val userId: Int,
    @SerializedName("customer_id")
    val customerId: Int,
    val total: Int,
    @SerializedName("status")
    val paymentStatus: String,
    val customer: Customer,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String
)

data class Transaction(
    @SerializedName("customer_id")
    val customerId: Int,
    val total: Int,
    @SerializedName("status")
    val paymentStatus: String,
    @SerializedName("user_id")
    val userId: Int,
    @SerializedName("updated_at")
    val updatedAt: String,
    @SerializedName("created_at")
    val createdAt: String,
    val id: Int,
    @SerializedName("transaction_product")
    val transactionProduct: List<TransactionProduct>,
    val customer: Customer
)

data class TransactionProduct (
    val id: Int,
    @SerializedName("transaction_id")
    val transactionId: Int,
    @SerializedName("product_price_id")
    val productPriceId: Int,
    @SerializedName("price_adjustment")
    val priceAdjustment: Int,
    val quantity: Int,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String
)

data class TransactionRequest (
    @SerializedName("customer_id")
    val customerId: Int,
    val total: Int,
    @SerializedName("product_price_id")
    val productPriceId: List<Int>,
    @SerializedName("price_adjustment")
    val priceAdjustment: List<Int>,
    @SerializedName("status")
    val paymentStatus: String,
    val quantity: List<Int>
)

data class CartItem(
    val product: GetProductByIdResponses?,
    val productPrice: List<ProductPrice>,
    var selectedPrice: ProductPrice,
    var quantity: Int = 1,
    var customPrice: Int = 0,
    var paymentStatus: String
)


data class TransactionDetail(
    @SerializedName("customer_id")
    val customerId: Int,
    val total: Int,
    @SerializedName("status")
    val paymentStatus: String,
    @SerializedName("user_id")
    val userId: Int,
    @SerializedName("updated_at")
    val updatedAt: String,
    @SerializedName("created_at")
    val createdAt: String,
    val id: Int,
    @SerializedName("transaction_product")
    val transactionProduct: List<TransactionDetailProduct>,
    val customer: Customer
)

data class TransactionDetailProduct(
    val id: Int,
    @SerializedName("transaction_id")
    val transactionId: Int,
    @SerializedName("product_price_id")
    val productPriceId: Int,
    @SerializedName("price_adjustment")
    val priceAdjustment: Int,
    val quantity: Int,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String,
    @SerializedName("product_price")
    val productPrice: ProductPrice
)

data class PaymentStatusUpdateRequest(
    val status: String
)