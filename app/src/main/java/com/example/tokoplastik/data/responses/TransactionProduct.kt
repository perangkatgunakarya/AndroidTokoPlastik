package com.example.tokoplastik.data.responses

import com.example.tokoplastik.util.Resource
import com.google.gson.annotations.SerializedName

data class TransactionProduct (
    val id: Int,
    @SerializedName("transaction_id")
    val transactionId: Int,
    @SerializedName("product_price_id")
    val productPriceId: Int,
    @SerializedName("price_adjustment")
    val priceAdjustment: Int,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String
)

data class CartItem(
    val product: GetProductByIdResponses?,
    val productPrice: List<ProductPrice>,
    var selectedPrice: ProductPrice,
    var quantity: Int = 1,
    var customPrice: Int = 0
)