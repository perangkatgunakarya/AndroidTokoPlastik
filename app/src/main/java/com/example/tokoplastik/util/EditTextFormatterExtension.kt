package com.example.tokoplastik.util

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import java.text.DecimalFormat

fun EditText.setNumberFormatter() {
    this.addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: Editable?) {
            this@setNumberFormatter.removeTextChangedListener(this)

            try {
                val value = s.toString().replace(",", "")

                if (value.isNotEmpty()) {
                    val formatter = DecimalFormat("#,###")
                    val formattedString = formatter.format(value.toLong())

                    this@setNumberFormatter.setText(formattedString)
                    this@setNumberFormatter.setSelection(formattedString.length)
                }
            } catch (e: NumberFormatException) {
                e.printStackTrace()
            }

            this@setNumberFormatter.addTextChangedListener(this)
        }
    })
}

fun EditText.getRawValue(): Int {
    return this.text.toString().replace(",", "").toIntOrNull() ?: 0
}