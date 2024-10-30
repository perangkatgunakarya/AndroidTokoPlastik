package com.example.tokoplastik.ui.product

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.tokoplastik.data.network.AddProductApi
import com.example.tokoplastik.data.repository.AddProductRepository
import com.example.tokoplastik.databinding.FragmentAddProductBinding
import com.example.tokoplastik.ui.base.BaseFragment
import com.example.tokoplastik.util.Resource
import com.example.tokoplastik.util.handleApiError
import com.example.tokoplastik.util.visible
import com.example.tokoplastik.viewmodel.AddProductViewModel
import com.example.tokoplastik.viewmodel.ProductViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class AddProductFragment : BaseFragment<AddProductViewModel, FragmentAddProductBinding, AddProductRepository> () {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.addProductProgressBar.visible(false)

        setupViews()
        observeViewModel()
    }

    private fun setupViews () {
        binding.buttonAddProduct.setOnClickListener {
            val name = binding.productNameTextField.text.toString()
            val supplier = binding.supplierTextField.text.toString()
            val capitalPrice = binding.capitalPriceTextField.text.toString()

            if (validateInputs(name, supplier, capitalPrice)) {
                viewModel.addProduct(name, supplier, capitalPrice)
            }
        }
    }

    private fun validateInputs(name: String, supplier: String, capitalPrice: String): Boolean {
        var isValid = true

        if (name.isEmpty()) {
            binding.productNameTextField.error = "Nama barang tidak boleh kosong"
            isValid = false
        }

        if (supplier.isEmpty()) {
            binding.supplierTextField.error = "Supplier tidak boleh kosong"
            isValid = false
        }

        if (capitalPrice.isEmpty()) {
            binding.capitalPriceTextField.error = "Harga modal tidak boleh kosong"
            isValid = false
        }

        return isValid
    }

    private fun observeViewModel () {
        viewModel.addProduct.observe(viewLifecycleOwner) { result ->

            binding.addProductProgressBar.visible(result is Resource.Loading)

            when (result) {
                is Resource.Success -> {
                    binding.addProductProgressBar.visible(false)
                    Toast.makeText( requireContext(), "Barang berhasil ditambahkan", Toast.LENGTH_SHORT ).show()
                }

                is Resource.Failure -> {
                    handleApiError(result)
                }
                is Resource.Loading -> {  }
            }
        }
    }

    override fun getViewModel(): Class<AddProductViewModel> = AddProductViewModel::class.java

    override fun getFragmentBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentAddProductBinding.inflate(inflater, container, false)

    override fun getFragmentRepository(): AddProductRepository {
        val token = runBlocking { userPreferences.authToken.first() }
        val api = remoteDataSource.buildApi(AddProductApi::class.java, token)
        return AddProductRepository(api)
    }

}