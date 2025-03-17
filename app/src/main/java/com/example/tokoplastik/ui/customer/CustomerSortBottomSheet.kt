package com.example.tokoplastik.ui.customer

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import androidx.lifecycle.ViewModelProvider
import com.example.tokoplastik.R
import com.example.tokoplastik.viewmodel.CustomerViewModel
import com.example.tokoplastik.viewmodel.SortOrder
import com.example.tokoplastik.viewmodel.SortType
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class CustomerSortBottomSheet : BottomSheetDialogFragment() {
    private lateinit var viewModel: CustomerViewModel
    private lateinit var ascendingButton: RadioButton
    private lateinit var descendingButton: RadioButton
    private lateinit var highestPriceButton: RadioButton
    private lateinit var lowestPriceButton: RadioButton
    private lateinit var doneButton: Button
    private lateinit var sortType: SortType

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = ViewModelProvider(requireParentFragment()).get(CustomerViewModel::class.java)
        return inflater.inflate(R.layout.bottom_sheet_customer_sort, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ascendingButton = view.findViewById(R.id.ascending_customer_name_sort_button)
        descendingButton = view.findViewById(R.id.descending_customer_name_sort_button)
        doneButton = view.findViewById(R.id.doneButton)

        doneButton.setOnClickListener {
            if (ascendingButton.isChecked) {
                sortType = SortType.NAME
                viewModel.setSortOrder(SortOrder.ASCENDING)
            } else if (descendingButton.isChecked) {
                sortType = SortType.NAME
                viewModel.setSortOrder(SortOrder.DESCENDING)
            }

            viewModel.applySort(sortType)
            Log.d(
                "CustomerSortBottomSheet",
                "sortType: $sortType, sortOrder: ${viewModel.sortOrder.value}"
            )
            dismiss()
        }
        restorePreviousState()
    }

    private fun setRadioButtonListeners() {
        val radioButtons = listOf(
            ascendingButton,
            descendingButton,
            highestPriceButton,
            lowestPriceButton
        )

        radioButtons.forEach { radioButton ->
            radioButton.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) {
                    radioButtons.forEach { otherButton ->
                        if (otherButton != buttonView) {
                            otherButton.isChecked = false
                        }
                    }
                }
            }
        }
    }

    private fun restorePreviousState() {
        when {
            viewModel.sortOrder.value == SortOrder.ASCENDING && viewModel.sortType.value == SortType.NAME -> {
                ascendingButton.isChecked = true
            }

            viewModel.sortOrder.value == SortOrder.DESCENDING && viewModel.sortType.value == SortType.NAME -> {
                descendingButton.isChecked = true
            }

            viewModel.sortOrder.value == SortOrder.DESCENDING && viewModel.sortType.value == SortType.CAPITAL -> {
                highestPriceButton.isChecked = true
            }

            viewModel.sortOrder.value == SortOrder.ASCENDING && viewModel.sortType.value == SortType.CAPITAL -> {
                lowestPriceButton.isChecked = true
            }

            else -> ascendingButton.isChecked = true
        }
    }

    companion object {
        const val TAG = "CustomerSortBottomSheet"
    }
}