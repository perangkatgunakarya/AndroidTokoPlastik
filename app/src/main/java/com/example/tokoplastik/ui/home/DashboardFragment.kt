package com.example.tokoplastik.ui.home

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import com.example.tokoplastik.R
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
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class DashboardFragment :
    BaseFragment<DashboardViewModel, FragmentDashboardBinding, DashboardRepository>() {

    private lateinit var lineChart: LineChart

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.homeProgressBar.visible(false)
        binding.todayDate.text =
            SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date())
        binding.userName.text = "Hello, ${runBlocking { userPreferences.username.first() }}"

//        lineChart = binding.chart
//        setupChipGroupListener()
        setupObserver()
        viewModel.getDashboardData()
        viewModel.getChart()

        binding.buttonLogout.setOnClickListener { logout() }

        // Atur listener untuk WindowInsets
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val navigationHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom

            val params = binding.dailyNotesLayout.layoutParams as ViewGroup.MarginLayoutParams
            params.bottomMargin = if (imeVisible) imeHeight + 100 else navigationHeight
            binding.dailyNotesLayout.layoutParams = params

            WindowInsetsCompat.CONSUMED
        }


        setupDailyNote()
    }

    private fun setupDailyNote() {
        // Get references to views
        val dailyNotes = binding.dailyNotes
        val dailyNotesCard = binding.dailyNotesCard

        // Load saved notes
        val savedNotes = loadSavedNotes()

        // Set the saved text (if any)
        if (!savedNotes.isNullOrEmpty()) {
            dailyNotes.setText(savedNotes)
        }

        // Initial setup based on whether text is empty or not
        updateDailyNotesStyle(dailyNotes.text.isNullOrEmpty(), false)

        // Focus change listener to handle when user clicks on or away from EditText
        dailyNotes.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                // When EditText gets focus - switch to editing mode style with focus indication
                dailyNotes.requestFocus()
                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(dailyNotes, InputMethodManager.SHOW_IMPLICIT)
                updateDailyNotesStyle(false, true)
            } else {
                // When EditText loses focus - check if empty and remove focus indication
                updateDailyNotesStyle(dailyNotes.text.isNullOrEmpty(), false)
            }
        }

        // Text change listener to save notes when text changes
        dailyNotes.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                // Save notes whenever text changes
                saveNotes(s.toString())
            }
        })
    }

    // Updated helper function to handle both empty state and focus state
    private fun updateDailyNotesStyle(isEmpty: Boolean, isFocused: Boolean) {
        val dailyNotes = binding.dailyNotes
        val dailyNotesCard = binding.dailyNotesCard

        // Set text styling based on empty state
        if (isEmpty) {
            dailyNotes.gravity = Gravity.CENTER
            dailyNotes.hint = "Tidak Ada Catatan Harian"
            dailyNotes.textSize = 18F
            dailyNotes.setTypeface(null, Typeface.BOLD)
        } else {
            dailyNotes.gravity = Gravity.TOP or Gravity.START
            dailyNotes.hint = null
            dailyNotes.textSize = 14F
            dailyNotes.setTypeface(null, Typeface.NORMAL)
        }

        // Set background color based on focus state
        if (isFocused) {
            // Darker background when focused - user is typing
            dailyNotesCard.setCardBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.secondary_note_focus
                )
            )
        } else {
            // Normal background when not focused
            dailyNotesCard.setCardBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.secondary_note
                )
            )
        }
    }

    // Function to save notes to SharedPreferences
    private fun saveNotes(notes: String) {
        val sharedPreferences =
            requireActivity().getSharedPreferences("DailyNotesPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("daily_notes", notes)
        editor.apply()
    }

    // Function to load saved notes from SharedPreferences
    private fun loadSavedNotes(): String? {
        val sharedPreferences =
            requireActivity().getSharedPreferences("DailyNotesPrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getString("daily_notes", null)
    }

//    private fun setupChipGroupListener() {
//        binding.radioGroup.setOnCheckedChangeListener { group, checkedId ->
//            when (checkedId) {
//                binding.radioButtonDaily.id -> {
//                    viewModel.getChart()
//                    binding.TitleTextView.text = "Grafik Penjualan Harian"
//                }
//
//                binding.radioButtonMonthly.id -> {
//                    viewModel.getChartMonthly()
//                    binding.TitleTextView.text = "Grafik Penjualan Bulanan"
//                }
//
//                else -> {
//                    Log.d("DashboardFragment", "No radio button selected")
//                }
//            }
//        }
//    }

    private fun setupObserver() {

        viewModel.dashboardData.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    val symbols = DecimalFormatSymbols(Locale.getDefault()).apply {
                        groupingSeparator = '.'
                    }
                    val formatter = DecimalFormat("#,###", symbols)

                    binding.homeProgressBar.visible(false)
                    binding.topText.text = it.data?.data?.topProduct?.product?.name
                    binding.unpaidText.text = it.data?.data?.unpaidOrder.toString()

//                    if (it.data?.data?.todayIncome!! < 0) {
//                        val formattedOmzet = formatter.format(abs(it.data.data.todayIncome))
//                        binding.omzetText.text = "-Rp$formattedOmzet"
//                    } else {
//                        val formattedOmzet = formatter.format(it.data.data.todayIncome)
//                        binding.omzetText.text = "Rp$formattedOmzet"
//                    }


//                    if (it.data?.data?.monthlyProfit!! < 0) {
//                        val formattedProfit = formatter.format(abs(it.data.data.monthlyProfit))
//                        binding.profitText.text = "-Rp${formattedProfit}"
//                    } else {
//                        val formattedProfit = formatter.format(it.data.data.monthlyProfit)
//                        binding.profitText.text = "Rp$formattedProfit"
//                    }

                }

                is Resource.Failure -> {
                    binding.homeProgressBar.visible(false)
                    handleApiError(it)
                }

                is Resource.Loading -> {}
            }
        }
    }

    private fun convertDateToIndex(dateStr: String): Float {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = sdf.parse(dateStr) ?: Date()
        return date.time.toFloat()
    }

    private fun dateValueFormatter(): ValueFormatter {
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

    private fun dateValueFormatterMonthly(): ValueFormatter {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        return object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val date = Date(value.toLong())
                return sdf.format(date)
            }
        }
    }

    private fun setupLineChart(
        chart: LineChart,
        omzet: List<Entry>,
        cost: List<Entry>,
        range: String
    ) {
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