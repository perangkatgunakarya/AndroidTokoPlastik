package com.example.tokoplastik.data.responses

import com.google.gson.annotations.SerializedName

data class GetChartResponses(
    val data: ChartData,
    val message: String,
    val success: Boolean
)

data class ChartData(
    val type: String,
    val range: String,
    val data: List<ChartSeries>
)

data class ChartSeries(
    val label: String,
    val points: List<ChartPoint>
)

data class ChartPoint(
    val x: String,
    val y: String
)

data class DashboardResponses(
    val data: DashboardData,
    val message: String,
    val success: Boolean
)

data class DashboardData(
    @SerializedName("top_product")
    val topProduct: TopProduct,
    @SerializedName("unpaid_orders")
    val unpaidOrder: Int,
    @SerializedName("today_income")
    val todayIncome: Int,
    @SerializedName("monthly_profit")
    val monthlyProfit: Int
)

data class TopProduct(
    @SerializedName("product_quantity")
    val productQuantity: Int,
    val product: GetProduct
)