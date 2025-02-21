package com.example.tokoplastik.ui.transaction

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.lifecycle.ViewModelProvider
import com.example.tokoplastik.R
import com.example.tokoplastik.viewmodel.CheckoutViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class DueDateBottomSheet : BottomSheetDialogFragment() {
    private lateinit var viewModel: CheckoutViewModel

    private lateinit var dateButton: Button
    private lateinit var dateEditText: EditText
    private lateinit var doneButton: Button

    private val calendar = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = ViewModelProvider(requireParentFragment()).get(CheckoutViewModel::class.java)
        return inflater.inflate(R.layout.bottom_sheet_due_date, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dateButton = view.findViewById(R.id.dateButton)
        dateEditText = view.findViewById(R.id.dateEditText)
        doneButton = view.findViewById(R.id.doneButton)

        viewModel.transactionDetail.observe(viewLifecycleOwner) { transactionDetail ->
            dateEditText.setText(transactionDetail.data?.data?.dueDate)
        }

        dateButton.setOnClickListener {
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(
                requireContext(),
                {
                    _, selectedYear, selectedMonth, selectedDay ->
                    calendar.set(selectedYear, selectedMonth, selectedDay)
                    val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
                    val formattedDate = dateFormat.format(calendar.time)
                    dateEditText.setText(formattedDate)
                },
                year, month, day
            )

            datePickerDialog.show()
        }

        doneButton.setOnClickListener {
            val selectedDate = dateEditText.text.toString()
            viewModel.setDueDate(selectedDate)
        }
    }
}