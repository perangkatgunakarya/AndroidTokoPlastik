package com.example.tokoplastik.ui.customer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import cn.pedant.SweetAlert.SweetAlertDialog
import com.example.tokoplastik.data.network.CustomerApi
import com.example.tokoplastik.data.repository.CustomerRepository
import com.example.tokoplastik.databinding.FragmentUpdateCustomerBinding
import com.example.tokoplastik.ui.base.BaseFragment
import com.example.tokoplastik.ui.product.ProductDetailFragmentArgs
import com.example.tokoplastik.util.Resource
import com.example.tokoplastik.util.handleApiError
import com.example.tokoplastik.util.visible
import com.example.tokoplastik.viewmodel.CustomerViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class UpdateCustomerFragment : BaseFragment<CustomerViewModel, FragmentUpdateCustomerBinding, CustomerRepository>() {

    private val args: UpdateCustomerFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.updateCustomerProgressBar.visible(false)

        setupViews()
        setupObserver()
    }

    private fun setupViews () {
        val customerId = args.customerId

        viewModel.getCustomerById(customerId)
        viewModel.getCustomer.observe(viewLifecycleOwner, { result ->
            binding.customerNameTextField.setText(result.data?.data?.name)
            binding.addressTextField.setText(result.data?.data?.address)
            binding.phoneTextField.setText(result.data?.data?.phone)
        })

        binding.buttonEditCustomer.setOnClickListener {
            val name = binding.customerNameTextField.text.toString()
            val address = binding.addressTextField.text.toString()
            val phoneNumber = binding.phoneTextField.text.toString()

            viewModel.updateCustomer(customerId, name, address, phoneNumber)
        }
    }

    private fun setupObserver () {
        viewModel.updateCustomer.observe(viewLifecycleOwner, { result ->
            binding.updateCustomerProgressBar.visible(result is Resource.Loading)

            when(result) {
                is Resource.Success -> {
                    binding.updateCustomerProgressBar.visible(false)
                    SweetAlertDialog(requireContext(), SweetAlertDialog.SUCCESS_TYPE).apply {
                        titleText = "Berhasil"
                        contentText = "Data customer berhasil dirubah"
                        setConfirmButton("OK") {
                            it.dismissWithAnimation()
                            val directions = UpdateCustomerFragmentDirections.actionUpdateCustomerFragmentToTransactionFragment()
                            findNavController().navigate(directions)
                        }
                        show()
                    }
                }
                is Resource.Failure -> {
                    binding.updateCustomerProgressBar.visible(false)
                    handleApiError(result)
                }
                is Resource.Loading -> {
                    binding.updateCustomerProgressBar.visible(true)
                }
            }
        })
    }

    override fun getViewModel() = CustomerViewModel::class.java

    override fun getFragmentBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentUpdateCustomerBinding.inflate(inflater, container, false)

    override fun getFragmentRepository(): CustomerRepository {
        val token = runBlocking { userPreferences.authToken.first() }
        val api = remoteDataSource.buildApi(CustomerApi::class.java, token)
        return CustomerRepository(api)
    }
}