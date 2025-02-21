package com.example.tokoplastik.ui.history

import android.os.Bundle
import android.util.Log
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
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class CreditBottomSheet : BottomSheetDialogFragment() {
    private lateinit var viewModel: CheckoutViewModel

    private lateinit var countTotal: TextView
    private lateinit var paidTotal: TextView
    private lateinit var paidDate: TextView
    private lateinit var remainingTotal: TextView
    private lateinit var paidInput: EditText
    private lateinit var paidButton: AppCompatButton
    private var paid: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = ViewModelProvider(requireParentFragment()).get(CheckoutViewModel::class.java)
        return inflater.inflate(R.layout.bottom_sheet_credit, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        countTotal = view.findViewById(R.id.count_total)
        paidTotal = view.findViewById(R.id.paid_total)
        paidDate = view.findViewById(R.id.paid_date)
        remainingTotal = view.findViewById(R.id.remaining_total)
        paidInput = view.findViewById(R.id.paid_input)
        paidButton = view.findViewById(R.id.paid_button)

        paidInput.setNumberFormatter()

        // observe detail transaksi
        viewModel.transactionDetail.observe(viewLifecycleOwner) { transactionDetail ->
            paid = transactionDetail.data?.data?.paid!!

            val symbols = DecimalFormatSymbols(Locale.getDefault()).apply {
                groupingSeparator = '.'
            }
            val formatter = DecimalFormat("#,###", symbols)
            val formattedCountTotal = formatter.format(transactionDetail.data.data.total.toDouble())
            countTotal.text = "Rp$formattedCountTotal"

            val formattedPaidTotal = formatter.format(transactionDetail.data.data.paid.toDouble())
            paidTotal.text = "Rp$formattedPaidTotal"

            //paid date
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'")
            parser.timeZone = TimeZone.getTimeZone("UTC")
            val date = parser.parse(transactionDetail.data?.data?.updatedAt)
            paidDate.text = "Terakhir bayar : ${SimpleDateFormat("dd MMM Y").format(date)}"

            //remaining total
            val formattedRemainingTotal = formatter.format(
                (transactionDetail.data?.data?.total?.toDouble()
                    ?: 0.0) - (transactionDetail.data?.data?.paid?.toDouble() ?: 0.0)
            )
            remainingTotal.text = "Rp$formattedRemainingTotal"
        }

        //klik bayar
        paidButton.setOnClickListener {
            viewModel.setPaidAmount(paidInput.getRawValue() + paid)
            dismiss()
        }
    }
}