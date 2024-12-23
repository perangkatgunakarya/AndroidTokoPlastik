package com.example.tokoplastik.ui.product

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioGroup
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.ViewModelProvider
import com.example.tokoplastik.R
import com.example.tokoplastik.viewmodel.ProductViewModel
import com.example.tokoplastik.viewmodel.SortOrder
import com.example.tokoplastik.viewmodel.SortType
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ProductSortBottomSheet : BottomSheetDialogFragment() {

    private lateinit var viewModel : ProductViewModel

    private lateinit var sortRadioGroup : RadioGroup
    private lateinit var sortOrderSwitch : SwitchCompat
    private lateinit var doneButton : Button
    private lateinit var sortType : SortType

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = ViewModelProvider(requireParentFragment()).get(ProductViewModel::class.java)
        return inflater.inflate(R.layout.bottom_sheet_product_sort, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sortRadioGroup = view.findViewById(R.id.sortRadioGroup)
        sortOrderSwitch = view.findViewById(R.id.sortOrderSwitch)
        doneButton = view.findViewById(R.id.doneButton)

        sortRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.sortByName -> sortType = SortType.NAME
                R.id.sortByCapital -> sortType = SortType.CAPITAL
            }
        }

        sortOrderSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setSortOrder(if (isChecked) SortOrder.DESCENDING else SortOrder.ASCENDING)
        }

        doneButton.setOnClickListener {
            viewModel.applySort(sortType)
            Log.d("ProductSortBottomSheet", "sortType: $sortType, sortOrder: ${viewModel.sortOrder.value}")
            dismiss()
        }

        restorePreviousState()
    }

    private fun restorePreviousState() {
        when (viewModel.sortType.value) {
            SortType.NAME -> sortRadioGroup.check(R.id.sortByName)
            SortType.CAPITAL -> sortRadioGroup.check(R.id.sortByCapital)
            SortType.DATE -> TODO()
            null -> sortRadioGroup.check(R.id.sortByDate)
        }

        sortOrderSwitch.isChecked = viewModel.sortOrder.value == SortOrder.DESCENDING
    }

    companion object {
        const val TAG = "ProductSortBottomSheet"
    }
}