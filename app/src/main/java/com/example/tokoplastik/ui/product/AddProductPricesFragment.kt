package com.example.tokoplastik.ui.product

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import com.example.tokoplastik.R
import com.example.tokoplastik.adapter.ProductPricesAdapter
import com.example.tokoplastik.data.network.AddProductPricesApi
import com.example.tokoplastik.data.repository.AddProductPricesRepository
import com.example.tokoplastik.data.responses.GetProduct
import com.example.tokoplastik.data.responses.ProductPrice
import com.example.tokoplastik.databinding.FragmentAddProductPricesBinding
import com.example.tokoplastik.ui.base.BaseFragment
import com.example.tokoplastik.util.Resource
import com.example.tokoplastik.util.handleApiError
import com.example.tokoplastik.util.visible
import com.example.tokoplastik.viewmodel.AddProductPricesViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class AddProductPricesFragment :
    BaseFragment<AddProductPricesViewModel, FragmentAddProductPricesBinding, AddProductPricesRepository>() {

    private val args: AddProductPricesFragmentArgs by navArgs()
    private val adapter = ProductPricesAdapter()
    private lateinit var getProductPrices: List<ProductPrice>
    private var productId: Int = -1

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let {
            productId = it.getInt(ARG_PRODUCT_ID)
        }

        binding.productPricesProgressbar.visible(false)

        setupViews()
        setupUnitSpinner()
        viewModel.getProductPrices(args.productId)
        observeViewModel()
    }

    companion object {
        private const val ARG_PRODUCT_ID = "arg_product_id"

        fun newInstance(productId: Int) =
            AddProductPricesFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_PRODUCT_ID, productId)
                }
            }
    }

    private fun setupViews() {
        binding.productPricesRecyclerView.adapter = adapter
        binding.productPricesRecyclerView.addItemDecoration(
            DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        )

        binding.buttonAddPrice.setOnClickListener {
            val unit = binding.unitSpinner.selectedItem.toString()
            val quantity = binding.quantityEditText.text.toString()
            val price = binding.priceEditText.text.toString().toIntOrNull() ?: 0

            if (validateInputs(quantity, price, unit)) {
                viewModel.addProductPrices(args.productId, price, unit, quantity)
            }
        }
    }

    private fun setupUnitSpinner() {
        val units = arrayOf("Dus", "Ball", "Ikat", "Pack", "Pcs", "Roll")
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            units
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.unitSpinner.adapter = adapter
    }

    private fun validateInputs(quantity: String, price: Int, unit: String): Boolean {
        var isValid = true

        if (quantity.isEmpty()) {
            binding.quantityEditText.error = "Kuantiti tidak boleh kosong"
            isValid = false
        }
        if (price <= 0) {
            binding.priceEditText.error = "Harga harus lebih dari 0"
            isValid = false
        }

        return isValid
    }

    private fun observeViewModel() {

        viewModel.productPrices.observe(viewLifecycleOwner) { result ->
            binding.productPricesProgressbar.visible(result is Resource.Loading)

            when (result) {
                is Resource.Success -> {
                    binding.productPricesProgressbar.visible(false)
                    result.data?.let { response ->
                        getProductPrices = listOf(response.data)
                        adapter.updateList(getProductPrices)
                    }
                }

                is Resource.Failure -> {
                    handleApiError(result)
                }

                Resource.Loading -> {}
            }
        }

        viewModel.addProductPrices.observe(viewLifecycleOwner) { result ->
            binding.productPricesProgressbar.visible(result is Resource.Loading)

            when (result) {
                is Resource.Success -> {
                    binding.productPricesProgressbar.visible(false)
                    Toast.makeText(
                        requireContext(),
                        "Harga berhasil ditambahkan",
                        Toast.LENGTH_SHORT
                    ).show()
                    clearInputs()
                    viewModel.getProductPrices(args.productId)
                }

                is Resource.Failure -> {
                    Toast.makeText(requireContext(), result.errorBody.toString(), Toast.LENGTH_SHORT).show()
                }

                is Resource.Loading -> {}
            }
        }
    }

    private fun clearInputs() {
        binding.quantityEditText.text?.clear()
        binding.priceEditText.text?.clear()
        binding.unitSpinner.setSelection(0)
    }


    override fun getViewModel() = AddProductPricesViewModel::class.java

    override fun getFragmentBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentAddProductPricesBinding.inflate(inflater, container, false)

    override fun getFragmentRepository(): AddProductPricesRepository {
        val token = runBlocking { userPreferences.authToken.first() }
        val api = remoteDataSource.buildApi(AddProductPricesApi::class.java, token)
        return AddProductPricesRepository(api)
    }
}