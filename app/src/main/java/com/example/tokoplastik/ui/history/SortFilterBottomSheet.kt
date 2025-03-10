package com.example.tokoplastik.ui.history

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import androidx.appcompat.widget.SwitchCompat
import androidx.core.util.Pair
import androidx.lifecycle.ViewModelProvider
import com.example.tokoplastik.R
import com.example.tokoplastik.viewmodel.CheckoutViewModel
import com.example.tokoplastik.viewmodel.SortOrder
import com.example.tokoplastik.viewmodel.SortType
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class SortFilterBottomSheet : BottomSheetDialogFragment() {
    // Use activityViewModels to share ViewModel with the parent Fragment
    private lateinit var viewModel: CheckoutViewModel

    // UI Components
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
        viewModel = ViewModelProvider(requireParentFragment()).get(CheckoutViewModel::class.java)

        // Inflate the layout for the bottom sheet
        return inflater.inflate(R.layout.bottom_sheet_sort_filter, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize UI components
        sortRadioGroup = view.findViewById(R.id.sortHistoryByRadioGroup)
        dateRangeButton = view.findViewById(R.id.dateHistoryRangeButton)
        startDateEditText = view.findViewById(R.id.startHistoryDateEditText)
        endDateEditText = view.findViewById(R.id.endHistoryDateEditText)
        doneButton = view.findViewById(R.id.doneHistoryButton)

        // Setup sort type radio group
        sortRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.newestDateHistorySortButton -> viewModel.setSortOrder(SortOrder.DESCENDING)
                R.id.oldestDateHistorySortButton -> viewModel.setSortOrder(SortOrder.ASCENDING)
            }
        }

        viewModel.setSortType(SortType.DATE)

        // Date range picker setup
        dateRangeButton.setOnClickListener {
            // Create date range picker
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

            // Show date picker
            datePicker.show(childFragmentManager, "DATE_RANGE_PICKER")
        }

        // Done button to apply filters
        doneButton.setOnClickListener {
            // Apply filters through ViewModel
            viewModel.applyFilters()
            Log.d(
                "SortFilterBottomSheet",
                viewModel.startDate.value.toString() + viewModel.endDate.value.toString() + viewModel.sortType.value.toString() + viewModel.sortOrder.value.toString() + viewModel.transactions.value
            )

            // Dismiss bottom sheet
            dismiss()
        }

        // Restore previous state if available
        restorePreviousState()
    }

    // Helper method to restore previous filter state
    private fun restorePreviousState() {
        // Restore sort type
        when (viewModel.sortOrder.value) {
            SortOrder.DESCENDING -> sortRadioGroup.check(R.id.newestDateHistorySortButton)
            SortOrder.ASCENDING -> sortRadioGroup.check(R.id.oldestDateHistorySortButton)
            null -> sortRadioGroup.check(R.id.oldestDateStockSortButton) // Default
        }

        // Restore date range
        viewModel.startDate.value?.let { start ->
            startDateEditText.setText(formatDate(start))
        }
        viewModel.endDate.value?.let { end ->
            endDateEditText.setText(formatDate(end))
        }
    }

    // Helper function to format date to readable string
    private fun formatDate(timestamp: Long): String {
        return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(timestamp))
    }

    companion object {
        const val TAG = "SortFilterBottomSheet"
    }
}