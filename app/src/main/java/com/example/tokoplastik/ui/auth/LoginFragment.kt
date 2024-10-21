package com.example.tokoplastik.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.example.tokoplastik.R
import com.example.tokoplastik.databinding.FragmentLoginBinding
import com.example.tokoplastik.data.network.AuthApi
import com.example.tokoplastik.data.repository.AuthRepository
import com.example.tokoplastik.ui.base.BaseFragment
import com.example.tokoplastik.util.Resource
import com.example.tokoplastik.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

class LoginFragment: BaseFragment<AuthViewModel, FragmentLoginBinding, AuthRepository> () {

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel.loginResponses.observe(viewLifecycleOwner, Observer {
            when(it) {
                is Resource.Success -> {
                    lifecycleScope.launch {
                        userPreferences.saveAuthToken(it.data?.data!!.token)
                    }
                }
                is Resource.Failure -> {
                    Toast.makeText(requireContext(), "Login Failed", Toast.LENGTH_SHORT).show()
                }
                is Resource.Error -> {
                    Toast.makeText(requireContext(), "Login Failed", Toast.LENGTH_SHORT).show()
                }
                is Resource.Loading -> TODO()
            }
        })

        binding.buttonLogin.setOnClickListener {
            val email = binding.emailTextField.text.toString().trim()
            val password = binding.passwordTextField.text.toString().trim()

            // @TODO add input validation
            viewModel.login(email, password)
        }
    }

    override fun getViewModel() = AuthViewModel::class.java

    override fun getFragmentBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentLoginBinding.inflate(inflater, container, false)

    override fun getFragmentRepository() = AuthRepository(remoteDataSource.buildApi(AuthApi::class.java))

}