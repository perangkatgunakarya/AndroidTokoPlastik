package com.example.tokoplastik.ui.auth

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.example.tokoplastik.databinding.FragmentLoginBinding
import com.example.tokoplastik.data.network.AuthApi
import com.example.tokoplastik.data.repository.AuthRepository
import com.example.tokoplastik.ui.base.BaseFragment
import com.example.tokoplastik.HomeActivity
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

            binding.loginProgressBar.visible(it is Resource.Loading)

            when(it) {
                is Resource.Success -> {
                    binding.loginProgressBar.visible(false)
                    lifecycleScope.launch {
                        viewModel.saveAuthToken(it.data?.data!!.token.toString())
                        viewModel.saveUsername(it.data?.data!!.name.toString())
                        requireActivity().startNewActivity(HomeActivity::class.java)
                    }
                }
                is Resource.Failure -> {
                    binding.loginProgressBar.visible(false)
                    handleApiError(it) { login() }
                }
                is Resource.Loading -> {
                    binding.loginProgressBar.visible(true)
                    hideKeyboard()
                }
            }
        })

        binding.passwordTextField.addTextChangedListener {
            val email = binding.emailTextField.text.toString().trim()
            binding.buttonLogin.enable(email.isNotEmpty() && it.toString().isNotEmpty())
        }

        binding.buttonLogin.setOnClickListener {
            login()
        }
    }

    private fun login() {
        val email = binding.emailTextField.text.toString().trim()
        val password = binding.passwordTextField.text.toString().trim()

        // @TODO add input validation
        viewModel.login(email, password)
    }

    private fun hideKeyboard() {
        val imm =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(requireView().windowToken, 0)
    }

    override fun getViewModel() = AuthViewModel::class.java

    override fun getFragmentBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentLoginBinding.inflate(inflater, container, false)

    override fun getFragmentRepository() = AuthRepository(remoteDataSource.buildApi(AuthApi::class.java), userPreferences)

}