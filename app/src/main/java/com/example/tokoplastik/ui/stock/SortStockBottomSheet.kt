package com.example.tokoplastik.ui.stock

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import androidx.core.util.Pair
import androidx.lifecycle.ViewModelProvider
import com.example.tokoplastik.R
import com.example.tokoplastik.viewmodel.SortOrder
import com.example.tokoplastik.viewmodel.SortType
import com.example.tokoplastik.viewmodel.StockViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class SortStockBottomSheet : BottomSheetDialogFragment() {

    private lateinit var viewModel: StockViewModel
    private lateinit var sortRadioGroup: RadioGroup
    private lateinit var dateRangeButton: Button
    private lateinit var startDateEditText: EditText
    private lateinit var endDateEditText: EditText
    private lateinit var doneButton: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = ViewModelProvider(requireParentFragment()).get(StockViewModel::class.java)

        return inflater.inflate(R.layout.bottom_sheet_sort_stock, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sortRadioGroup = view.findViewById(R.id.sortStockByRadioGroup)
        dateRangeButton = view.findViewById(R.id.dateStockRangeButton)
        startDateEditText = view.findViewById(R.id.startStockDateEditText)
        endDateEditText = view.findViewById(R.id.endStockDateEditText)
        doneButton = view.findViewById(R.id.doneStockButton)

        sortRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.newestDateStockSortButton -> viewModel.setSortOrder(SortOrder.DESCENDING)
                R.id.oldestDateStockSortButton -> viewModel.setSortOrder(SortOrder.ASCENDING)
            }
        }

        viewModel.setSortType(SortType.DATE)


        dateRangeButton.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("Select Date Range")
                .setSelection(
                    Pair(
                        viewModel.startDate.value
                            ?: MaterialDatePicker.thisMonthInUtcMilliseconds(),

                        viewModel.endDate.value ?: MaterialDatePicker.todayInUtcMilliseconds()
                    )
                )
                .build()

            datePicker.addOnPositiveButtonClickListener { selection ->
                val endDate = if (selection.first == selection.second) {
                    Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                        timeInMillis = selection.second
                        add(Calendar.DATE, 1)
                    }.timeInMillis
                } else {
                    selection.second
                }

                viewModel.setDateRange(selection.first, endDate)

                startDateEditText.setText(formatDate(selection.first))
                endDateEditText.setText(formatDate(selection.second))
            }

            datePicker.show(childFragmentManager, "DATE_RANGE_PICKER")
        }

        doneButton.setOnClickListener {
            viewModel.applyFilters()
            dismiss()
        }

        restorePreviousState()
    }

    private fun restorePreviousState() {
        when (viewModel.sortOrder.value) {
            SortOrder.DESCENDING -> sortRadioGroup.check(R.id.newestDateStockSortButton)
            SortOrder.ASCENDING -> sortRadioGroup.check(R.id.oldestDateStockSortButton)
            null -> sortRadioGroup.check(R.id.newestDateHistorySortButton) // Default
        }

        viewModel.startDate.value?.let { start ->
            startDateEditText.setText(formatDate(start))
        }
        viewModel.endDate.value?.let { end ->
            endDateEditText.setText(formatDate(end))
        }
    }

    private fun formatDate(timestamp: Long): String {
        return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(timestamp))
    }

    companion object {
        const val TAG = "SortStockBottomSheet"
    }
}