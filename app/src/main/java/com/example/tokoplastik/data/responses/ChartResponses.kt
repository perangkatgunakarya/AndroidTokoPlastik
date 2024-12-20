package com.example.tokoplastik.data.responses

data class GetChartResponses(
    val data: ChartData,
    val message: String,
    val success: Boolean
)

data class ChartData(
    val type: String,
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