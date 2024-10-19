package com.example.tokoplastik.fragments.login

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.tokoplastik.R

class IntroductionFragment: Fragment(R.layout.fragment_introduction) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val startButon: Button = view.findViewById(R.id.start_button)
        startButon.setOnClickListener {
            findNavController().navigate(R.id.to_login_fragment)
        }
    }
}