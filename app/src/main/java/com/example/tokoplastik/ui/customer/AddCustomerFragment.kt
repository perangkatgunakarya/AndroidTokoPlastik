package com.example.tokoplastik.ui.customer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import cn.pedant.SweetAlert.SweetAlertDialog
import com.example.tokoplastik.data.network.CustomerApi
import com.example.tokoplastik.data.repository.CustomerRepository
import com.example.tokoplastik.databinding.FragmentAddCustomerBinding
import com.example.tokoplastik.ui.base.BaseFragment
import com.example.tokoplastik.util.Resource
import com.example.tokoplastik.util.handleApiError
import com.example.tokoplastik.util.visible
import com.example.tokoplastik.viewmodel.CustomerViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class AddCustomerFragment : BaseFragment<CustomerViewModel, FragmentAddCustomerBinding, CustomerRepository>() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.addCustomerProgressBar.visible(false)

        binding.buttonBack.setOnClickListener {
            requireActivity().onBackPressed()
        }

        setupViews()
        setupObserver()
    }

    private fun setupViews () {
        binding.buttonAddCustomer.setOnClickListener {
            val name = binding.customerNameTextField.text.toString()
            val address = binding.addressTextField.text.toString()
            val phoneNumber = binding.phoneTextField.text.toString()

            if (validateInputs(name, address, phoneNumber)) {
                viewModel.addCustomer(name, address, phoneNumber)
            }
        }

        binding.btnWhatsApp.setOnClickListener {
            val phoneNumber = binding.phoneTextField.text.toString().trim()

            if (phoneNumber.isEmpty()) {
                Toast.makeText(requireContext(), "Masukkan nomor telepon terlebih dahulu", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(requireContext(), "WhatsApp Business tidak terinstall", Toast.LENGTH_SHORT).show()
        }
    }

    private fun validateInputs(name: String, address: String, phoneNumber: String): Boolean {
        var isValid = true

        if (name.isEmpty()) {
            binding.customerNameTextField.error = "Nama customer tidak boleh kosong"
            isValid = false
        }

        if (address.isEmpty()) {
            binding.addressTextField.error = "Alamat tidak boleh kosong"
            isValid = false
        }

        if (phoneNumber.isEmpty()) {
            binding.phoneTextField.error = "Nomor HP tidak boleh kosong"
            isValid = false
        }

        return isValid
    }

    private fun setupObserver () {
        viewModel.addCustomer.observe(viewLifecycleOwner, { result ->
            binding.addCustomerProgressBar.visible(result is Resource.Loading)

            when(result) {
                is Resource.Success -> {
                    binding.addCustomerProgressBar.visible(false)
                    SweetAlertDialog(requireContext(), SweetAlertDialog.SUCCESS_TYPE).apply {
                        titleText = "Berhasil"
                        contentText = "Customer berhasil ditambahkan"
                        setConfirmButton("OK") {
                            it.dismissWithAnimation()
                            val directions = AddCustomerFragmentDirections.actionAddCustomerFragmentToTransactionFragment()
                            findNavController().navigate(directions)
                        }
                        show()
                    }
                }
                is Resource.Failure -> {
                    binding.addCustomerProgressBar.visible(false)
                    handleApiError(result)
                }
                is Resource.Loading -> {
                    binding.addCustomerProgressBar.visible(true)
                }
            }
        })
    }

    override fun getViewModel() = CustomerViewModel::class.java

    override fun getFragmentBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentAddCustomerBinding.inflate(inflater, container, false)

    override fun getFragmentRepository(): CustomerRepository {
        val token = runBlocking { userPreferences.authToken.first() }
        val api = remoteDataSource.buildApi(CustomerApi::class.java, token)
        return CustomerRepository(api)
    }

}