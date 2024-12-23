package com.example.tokoplastik.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import com.example.tokoplastik.data.network.DashboardApi
import com.example.tokoplastik.data.network.GetProductApi
import com.example.tokoplastik.data.repository.DashboardRepository
import com.example.tokoplastik.data.repository.ProductRepository
import com.example.tokoplastik.data.responses.ChartData
import com.example.tokoplastik.data.responses.GetProduct
import com.example.tokoplastik.databinding.FragmentDashboardBinding
import com.example.tokoplastik.ui.base.BaseFragment
import com.example.tokoplastik.util.Resource
import com.example.tokoplastik.util.handleApiError
import com.example.tokoplastik.util.visible
import com.example.tokoplastik.viewmodel.DashboardViewModel
import com.example.tokoplastik.viewmodel.HomeViewModel
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashboardFragment : BaseFragment <DashboardViewModel, FragmentDashboardBinding, DashboardRepository> () {

    private lateinit var lineChart: LineChart

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.homeProgressBar.visible(false)
        binding.todayDate.text = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date())
        binding.userName.text = "Hello, ${runBlocking { userPreferences.username.first() }}"

        lineChart = binding.chart
        setupChipGroupListener()
        setupObserver()
        viewModel.getDashboardData()
        viewModel.getChart()

        binding.buttonLogout.setOnClickListener { logout() }
    }

    private fun setupChipGroupListener() {
        binding.radioGroup.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                binding.radioButtonDaily.id -> {
                    viewModel.getChart()
                    binding.TitleTextView.text = "Grafik Penjualan Harian"
                }
                binding.radioButtonMonthly.id -> {
                    viewModel.getChartMonthly()
                    binding.TitleTextView.text = "Grafik Penjualan Bulanan"
                }
                else -> {
                    Log.d("DashboardFragment", "No radio button selected")
                }
            }
        }
    }

    private fun setupObserver() {
        viewModel.chart.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    binding.homeProgressBar.visible(false)
                    updateChart(it.data?.data)
                }

                is Resource.Failure -> {
                    binding.homeProgressBar.visible(false)
                    handleApiError(it)
                }

                is Resource.Loading -> {}
            }
        }

        viewModel.dashboardData.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    binding.homeProgressBar.visible(false)
                    binding.topText.text = it.data?.data?.topProduct?.product?.name
                    binding.omzetText.text = it.data?.data?.todayIncome.toString()
                    binding.profitText.text = it.data?.data?.monthlyProfit.toString()
                    binding.unpaidText.text = it.data?.data?.unpaidOrder.toString()
                }
                is Resource.Failure -> {
                    binding.homeProgressBar.visible(false)
                    handleApiError(it)
                }
                is Resource.Loading -> {}
            }
        }
    }

    private fun updateChart(chartData: ChartData?) {
        val omzetEntries = mutableListOf<Entry>()
        val costEntries = mutableListOf<Entry>()
        val range = chartData?.range

        chartData?.data?.forEach { series ->
            when (series.label) {
                "Omzet" -> {
                    series.points.forEach { point ->
                        if (range.toString() == "daily") {
                            val dateIndex = convertDateToIndex(point.x)
                            omzetEntries.add(Entry(dateIndex, point.y.toFloat()))
                        } else {
                            val dateIndex = convertDateToIndexMonthly(point.x)
                            omzetEntries.add(Entry(dateIndex, point.y.toFloat()))
                        }
                    }
                }
                "Cost" -> {
                    series.points.forEach { point ->
                        if (range.toString() == "daily") {
                            val dateIndex = convertDateToIndex(point.x)
                            costEntries.add(Entry(dateIndex, point.y.toFloat()))
                        } else {
                            val dateIndex = convertDateToIndexMonthly(point.x)
                            costEntries.add(Entry(dateIndex, point.y.toFloat()))
                        }
                    }
                }
            }
        }

        setupLineChart(lineChart, omzetEntries, costEntries, range.toString())
    }

    private fun convertDateToIndex(dateStr: String): Float {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = sdf.parse(dateStr) ?: Date()
        return date.time.toFloat()
    }

    private fun dateValueFormatter() : ValueFormatter {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val date = Date(value.toLong())
                return sdf.format(date)
            }
        }
    }

    private fun convertDateToIndexMonthly(dateStr: String): Float {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val date = sdf.parse(dateStr) ?: Date()
        return date.time.toFloat()
    }

    private fun dateValueFormatterMonthly() : ValueFormatter {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        return object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val date = Date(value.toLong())
                return sdf.format(date)
            }
        }
    }

    private fun setupLineChart(chart: LineChart, omzet: List<Entry>, cost: List<Entry>, range: String) {
        val omzetDataSet = LineDataSet(omzet, "Omzet").apply {
            color = android.graphics.Color.BLUE
            valueTextColor = android.graphics.Color.BLACK
            lineWidth = 2f
        }

        val costDataSet = LineDataSet(cost, "Cost").apply {
            color = android.graphics.Color.RED
            valueTextColor = android.graphics.Color.BLACK
            lineWidth = 2f
        }

        val lineData = LineData(omzetDataSet, costDataSet)

        if (range == "daily") {
            chart.xAxis.valueFormatter = dateValueFormatter()
        } else {
            chart.xAxis.valueFormatter = dateValueFormatterMonthly()
        }

        chart.apply {
            data = lineData
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
            xAxis.labelRotationAngle = -90f
            invalidate()
        }
    }

    override fun getViewModel() = DashboardViewModel::class.java

    override fun getFragmentBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentDashboardBinding.inflate(inflater, container, false)

    override fun getFragmentRepository(): DashboardRepository {
        val token = runBlocking { userPreferences.authToken.first() }
        val api = remoteDataSource.buildApi(DashboardApi::class.java, token)
        return DashboardRepository(api)
    }
}