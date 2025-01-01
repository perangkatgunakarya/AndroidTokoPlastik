package com.example.tokoplastik.ui.stock

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.example.tokoplastik.R
import com.example.tokoplastik.util.Resource
import com.example.tokoplastik.viewmodel.StockViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class RestockQuantityBottomSheet : BottomSheetDialogFragment() {

    private lateinit var viewModel: StockViewModel

    private lateinit var restockQuantity: EditText
    private lateinit var restockUnit: TextView
    private lateinit var saveStockButton: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = ViewModelProvider(requireParentFragment()).get(StockViewModel::class.java)
        return inflater.inflate(R.layout.bottom_sheet_restock_quantity, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        restockQuantity = view.findViewById(R.id.restockQuantity)
        restockUnit = view.findViewById(R.id.restockUnit)
        saveStockButton = view.findViewById(R.id.saveStockButton)

        saveStockButton.setOnClickListener {
            val quantity = restockQuantity.text.toString().toIntOrNull()
            if (quantity != null) {
                viewModel.productId?.let { it1 -> viewModel.addStock(it1, "restock", quantity) }

                viewModel.addStock.observe(viewLifecycleOwner) {
                    when (it) {
                        is Resource.Success -> {
                            viewModel.setAddStockStatus(true)
                            dismiss()
                        }
                        is Resource.Failure -> {
                            dismiss()
                        }
                        is Resource.Loading -> {
                            dismiss()
                        }
                    }
                }

                dismiss()
            }
        }

        viewModel.product.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    restockUnit.text = it.data?.data?.product?.lowestUnit
                }
                is Resource.Failure -> {
                    dismiss()
                }
                is Resource.Loading -> {
                    dismiss()
                }
            }
        }
    }
}