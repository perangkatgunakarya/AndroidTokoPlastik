package com.example.tokoplastik.ui.transaction

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.lifecycle.ViewModelProvider
import com.example.tokoplastik.R
import com.example.tokoplastik.util.getRawValue
import com.example.tokoplastik.util.setNumberFormatter
import com.example.tokoplastik.viewmodel.CheckoutViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class PaidBottomSheet : BottomSheetDialogFragment() {
    private lateinit var viewModel : CheckoutViewModel

    private lateinit var countTotal : TextView
    private lateinit var paidInput : EditText
    private lateinit var paidButton : AppCompatButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = ViewModelProvider(requireParentFragment()).get(CheckoutViewModel::class.java)
        return inflater.inflate(R.layout.bottom_sheet_paid, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        countTotal = view.findViewById(R.id.count_total)
        paidInput = view.findViewById(R.id.paid_input)
        paidButton = view.findViewById(R.id.paid_button)

        paidInput.setNumberFormatter()

        //observe cartItems
        viewModel.cartItems.observe(viewLifecycleOwner) { items ->
            val total = items.sumOf { it.customPrice * it.quantity }
            countTotal.text = getString(R.string.price_format, total.toDouble())
        }

        //klik bayar and proceed checkout in checkout fragment
        paidButton.setOnClickListener {
            viewModel.setPaidAmount(paidInput.getRawValue())
        }
    }
}