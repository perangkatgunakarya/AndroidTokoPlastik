package com.example.tokoplastik.ui.customer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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

class UpdateCustomerFragment :
    BaseFragment<CustomerViewModel, FragmentUpdateCustomerBinding, CustomerRepository>() {

    private val args: UpdateCustomerFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.updateCustomerProgressBar.visible(false)

        setupViews()
        setupObserver()
    }

    private fun setupViews() {
        val customerId = args.customerId

        binding.buttonBack.setOnClickListener {
            requireActivity().onBackPressed()
        }

        viewModel.getCustomerById(customerId)
        viewModel.getCustomer.observe(viewLifecycleOwner, { result ->
            binding.customerNameToolbar.setText(result.data?.data?.name)
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

        binding.btnWhatsApp.setOnClickListener {
            val phoneNumber = binding.phoneTextField.text.toString().trim()

            if (phoneNumber.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "Masukkan nomor telepon terlebih dahulu",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                openWhatsApp(phoneNumber)
            }
        }
    }

    private fun openWhatsApp(phoneNumber: String) {
        val url = "https://api.whatsapp.com/send?phone=$phoneNumber"
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(url)
        if (intent.resolveActivity(requireActivity().packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(
                requireContext(),
                "WhatsApp Business tidak terinstall",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun setupObserver() {
        viewModel.updateCustomer.observe(viewLifecycleOwner, { result ->
            binding.updateCustomerProgressBar.visible(result is Resource.Loading)

            when (result) {
                is Resource.Success -> {
                    binding.updateCustomerProgressBar.visible(false)
                    SweetAlertDialog(requireContext(), SweetAlertDialog.SUCCESS_TYPE).apply {
                        titleText = "Berhasil"
                        contentText = "Data customer berhasil dirubah"
                        setConfirmButton("OK") {
                            it.dismissWithAnimation()
                            val directions =
                                UpdateCustomerFragmentDirections.actionUpdateCustomerFragmentToTransactionFragment()
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