package com.example.tokoplastik.ui.product

import android.R
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.addCallback
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tokoplastik.adapter.ProductPricesAdapter
import com.example.tokoplastik.data.network.AddProductPricesApi
import com.example.tokoplastik.data.repository.AddProductPricesRepository
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
    private lateinit var pricesAdapter: ProductPricesAdapter
    private lateinit var getProductPrices: List<ProductPrice>
    private var productId: Int = -1

    private val units = listOf("pcs", "unit", "pack", "unit", "buah", "pasang", "kotak", "lusin", "lembar", "keping", "batang", "bungkus", "potong", "tablet", "ekor", "rim", "karat", "botol", "butir", "roll", "dus", "karung", "koli", "sak", "bal", "kaleng", "set", "slop", "gulung", "ton", "kg", "gram", "mg", "meter", "m2", "m3", "inch", "cc", "liter")
    private var selectedUnit: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewModel()

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            requireActivity().finish()
        }
        arguments?.let {
            productId = it.getInt(ARG_PRODUCT_ID)
        }

        binding.productPricesProgressbar.visible(false)

        setupViews()
        setupUnitSpinner()
        viewModel.getProductPrices(args.productId)
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
        pricesAdapter = ProductPricesAdapter (
            onDeleteItem = { item ->
                viewModel.deleteProductPrice(item.id)
            }
        )
        binding.productPricesRecyclerView.apply {
            adapter = pricesAdapter
            layoutManager = LinearLayoutManager(requireContext())

            val swipeToDeleteCallback = ProductPricesAdapter.createSwipeToDelete(pricesAdapter, this.context)
            val itemTouchHelper = ItemTouchHelper(swipeToDeleteCallback)
            itemTouchHelper.attachToRecyclerView(this)
        }

        binding.buttonAddPrice.setOnClickListener {
            val unit = binding.unitDropdown.text.toString()
            val quantity = binding.quantityEditText.text.toString()
            val price = binding.priceEditText.text.toString().toIntOrNull() ?: 0

            if (validateInputs(quantity, price, unit)) {
                Log.d("Fragment", "Attempting to add product price")
                viewModel.addProductPrices(args.productId, price, unit, quantity)
            }
        }
    }

    private fun setupUnitSpinner() {
        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.simple_dropdown_item_1line,
            units
        )
        binding.unitDropdown.setAdapter(adapter)
        binding.unitDropdown.threshold = 1

        binding.unitDropdown.setOnItemClickListener { _, _, position, _ ->
            selectedUnit = adapter.getItem(position)
            binding.unitDropdown.setText(selectedUnit, false)
        }
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
                        getProductPrices = response.data
                        Log.d("ProductPrices", "Received ${getProductPrices.size} items")
                        pricesAdapter.updateList(getProductPrices)
                        pricesAdapter.notifyDataSetChanged()
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
        binding.unitDropdown.text?.clear()
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