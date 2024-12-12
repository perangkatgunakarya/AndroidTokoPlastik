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
import java.util.Date
import java.util.Locale

class SortFilterBottomSheet : BottomSheetDialogFragment() {
    // Use activityViewModels to share ViewModel with the parent Fragment
    private lateinit var viewModel: CheckoutViewModel

    // UI Components
    private lateinit var sortRadioGroup: RadioGroup
    private lateinit var sortOrderSwitch: SwitchCompat
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
        sortRadioGroup = view.findViewById(R.id.sortRadioGroup)
        sortOrderSwitch = view.findViewById(R.id.sortOrderSwitch)
        dateRangeButton = view.findViewById(R.id.dateRangeButton)
        startDateEditText = view.findViewById(R.id.startDateEditText)
        endDateEditText = view.findViewById(R.id.endDateEditText)
        doneButton = view.findViewById(R.id.doneButton)

        // Setup sort type radio group
        sortRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.sortByDate -> viewModel.setSortType(SortType.DATE)
            }
        }

        // Setup sort order switch
        sortOrderSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setSortOrder(if (isChecked) SortOrder.DESCENDING else SortOrder.ASCENDING)
        }

        // Date range picker setup
        dateRangeButton.setOnClickListener {
            // Create date range picker
            val datePicker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("Select Date Range")
                .setSelection(
                    Pair(
                        viewModel.startDate.value ?: MaterialDatePicker.thisMonthInUtcMilliseconds(),
                        viewModel.endDate.value ?: MaterialDatePicker.todayInUtcMilliseconds()
                    )
                )
                .build()

            // Handle date selection
            datePicker.addOnPositiveButtonClickListener { selection ->
                // Set date range in ViewModel
                viewModel.setDateRange(selection.first, selection.second)

                // Update EditText fields with selected dates
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
            Log.d("SortFilterBottomSheet", viewModel.startDate.value.toString() + viewModel.endDate.value.toString() + viewModel.sortType.value.toString() + viewModel.sortOrder.value.toString() + viewModel.transactions.value)

            // Dismiss bottom sheet
            dismiss()
        }

        // Restore previous state if available
        restorePreviousState()
    }

    // Helper method to restore previous filter state
    private fun restorePreviousState() {
        // Restore sort type
        when (viewModel.sortType.value) {
            SortType.DATE -> sortRadioGroup.check(R.id.sortByDate)
            null -> sortRadioGroup.check(R.id.sortByDate) // Default
        }

        // Restore sort order
        sortOrderSwitch.isChecked = viewModel.sortOrder.value == SortOrder.DESCENDING

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