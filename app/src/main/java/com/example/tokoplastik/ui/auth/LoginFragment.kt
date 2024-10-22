package com.example.tokoplastik.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.example.tokoplastik.R
import com.example.tokoplastik.databinding.FragmentLoginBinding
import com.example.tokoplastik.data.network.AuthApi
import com.example.tokoplastik.data.repository.AuthRepository
import com.example.tokoplastik.ui.base.BaseFragment
import com.example.tokoplastik.ui.home.HomeActivity
import com.example.tokoplastik.util.Resource
import com.example.tokoplastik.util.enable
import com.example.tokoplastik.util.handleApiError
import com.example.tokoplastik.util.startNewActivity
import com.example.tokoplastik.util.visible
import com.example.tokoplastik.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

class LoginFragment: BaseFragment<AuthViewModel, FragmentLoginBinding, AuthRepository> () {

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        binding.loginProgressBar.visible(false)
        binding.buttonLogin.enable(false)

        viewModel.loginResponses.observe(viewLifecycleOwner, Observer {

            // Show progress bar only while loading
            binding.loginProgressBar.visible(it is Resource.Loading)

            when(it) {
                is Resource.Success -> {
                    binding.loginProgressBar.visible(false)
                    lifecycleScope.launch {
                        viewModel.saveAuthToken(it.data?.data!!.token.toString())
                        requireActivity().startNewActivity(HomeActivity::class.java)
                    }
                }
                is Resource.Failure -> {
                    binding.loginProgressBar.visible(false)
                    handleApiError(it)
                }
                is Resource.Loading -> {
                    binding.loginProgressBar.visible(true)
                }
            }
        })

        binding.passwordTextField.addTextChangedListener {
            val email = binding.emailTextField.text.toString().trim()
            binding.buttonLogin.enable(email.isNotEmpty() && it.toString().isNotEmpty())
        }

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

    override fun getFragmentRepository() = AuthRepository(remoteDataSource.buildApi(AuthApi::class.java), userPreferences)

}