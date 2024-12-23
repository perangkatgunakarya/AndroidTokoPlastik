package com.example.tokoplastik.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import com.example.tokoplastik.data.UserPreferences
import com.example.tokoplastik.data.network.AuthApi
import com.example.tokoplastik.data.network.RemoteDataSource
import com.example.tokoplastik.data.repository.BaseRepository
import com.example.tokoplastik.ui.auth.AuthActivity
import com.example.tokoplastik.util.startNewActivity
import com.example.tokoplastik.viewmodel.ViewModelFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

abstract class BaseFragment<VM: BaseViewModel, B: ViewBinding, R: BaseRepository> : Fragment () {

    protected lateinit var userPreferences: UserPreferences
    protected lateinit var binding: B
    protected lateinit var viewModel: VM
    protected val remoteDataSource = RemoteDataSource()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        userPreferences = UserPreferences(requireContext())
        binding = getFragmentBinding(inflater, container)
        val factory = ViewModelFactory(getFragmentRepository())
        viewModel = ViewModelProvider(this, factory).get(getViewModel())

        lifecycleScope.launch { userPreferences.authToken.first() }

        return binding.root
    }

    abstract fun getViewModel(): Class<VM>
    abstract fun getFragmentBinding(inflater: LayoutInflater, container: ViewGroup?) : B
    abstract fun getFragmentRepository(): R

    fun logout() = lifecycleScope.launch {
        val authToken = userPreferences.authToken.first()
        val api = remoteDataSource.buildApi(AuthApi::class.java, authToken)

        viewModel.logout(api)
        userPreferences.clearAuthToken()
        userPreferences.clearUsername()

        requireActivity().startNewActivity(AuthActivity::class.java)
    }
}