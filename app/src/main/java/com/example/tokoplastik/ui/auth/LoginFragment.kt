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

            binding.loginProgressBar.visible(false)

            when(it) {
                is Resource.Success -> {
                    viewModel.saveAuthToken(it.data?.data!!.token.toString())
                    requireActivity().startNewActivity(HomeActivity::class.java)
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

        binding.passwordTextField.addTextChangedListener {
            val email = binding.emailTextField.text.toString().trim()
            binding.buttonLogin.enable(email.isNotEmpty() && it.toString().isNotEmpty())
        }

        binding.buttonLogin.setOnClickListener {
            val email = binding.emailTextField.text.toString().trim()
            val password = binding.passwordTextField.text.toString().trim()

            binding.loginProgressBar.visible(true)

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