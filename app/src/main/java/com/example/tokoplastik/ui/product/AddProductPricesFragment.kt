package com.example.tokoplastik.ui.product

import android.R
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tokoplastik.HomeActivity
import com.example.tokoplastik.MainActivity
import com.example.tokoplastik.adapter.ProductPricesAdapter
import com.example.tokoplastik.data.network.AddProductPricesApi
import com.example.tokoplastik.data.network.GetProductApi
import com.example.tokoplastik.data.repository.AddProductPricesRepository
import com.example.tokoplastik.data.responses.ProductPrice
import com.example.tokoplastik.databinding.FragmentAddProductPricesBinding
import com.example.tokoplastik.ui.base.BaseFragment
import com.example.tokoplastik.util.Resource
import com.example.tokoplastik.util.getRawValue
import com.example.tokoplastik.util.handleApiError
import com.example.tokoplastik.util.setNumberFormatter
import com.example.tokoplastik.util.visible
import com.example.tokoplastik.viewmodel.AddProductPricesViewModel
import com.example.tokoplastik.viewmodel.ProductDetailViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class AddProductPricesFragment :
    BaseFragment<AddProductPricesViewModel, FragmentAddProductPricesBinding, AddProductPricesRepository>() {

    private val args: AddProductPricesFragmentArgs by navArgs()
    private lateinit var pricesAdapter: ProductPricesAdapter
    private lateinit var getProductPrices: List<ProductPrice>
    private var productId: Int = -1

    private lateinit var productDetailViewModel: ProductDetailViewModel

    private val units = listOf(
        "pcs",
        "ikat",
        "unit",
        "pack",
        "unit",
        "buah",
        "pasang",
        "kotak",
        "lusin",
        "lembar",
        "keping",
        "batang",
        "bungkus",
        "potong",
        "tablet",
        "ekor",
        "rim",
        "karat",
        "botol",
        "butir",
        "roll",
        "dus",
        "karung",
        "koli",
        "sak",
        "bal",
        "kaleng",
        "set",
        "slop",
        "gulung",
        "ton",
        "kg",
        "gram",
        "mg",
        "meter",
        "m2",
        "m3",
        "inch",
        "cc",
        "liter"
    )
    private var selectedUnit: String? = null

    private lateinit var priceEdit: EditText
    private var existingProductPrice: ProductPrice? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initProductDetailViewModel()

        observeViewModel()

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            // Ambil productId dari arguments fragment ini
            val productId = args.productId

            // Buat action yang sama untuk pindah ke ProductDetailFragment
            val action = AddProductPricesFragmentDirections
                .actionAddProductPricesFragmentToDetailProductFragment(productId)

            // Lakukan navigasi
            findNavController().navigate(action)
        }

        arguments?.let {
            productId = it.getInt(ARG_PRODUCT_ID)
        }

        binding.productPricesProgressbar.visible(false)

        binding.buttonBack.setOnClickListener {
            val productId = args.productId

            // Buat action untuk pindah ke ProductDetailFragment
            val action = AddProductPricesFragmentDirections
                .actionAddProductPricesFragmentToDetailProductFragment(productId)

            // Lakukan navigasi
            findNavController().navigate(action)
        }

        priceEdit = view.findViewById(com.example.tokoplastik.R.id.price_edit_text)
        priceEdit.setNumberFormatter()

        setupViews()
        setupUnitSpinner()
        viewModel.getProductPrices(args.productId)

        productDetailViewModel.getProductDetail(args.productId)
    }

    private val detailProductLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val returnedProductId = data?.getIntExtra("PRODUCT_ID", -1) ?: -1
            if (returnedProductId != -1) {
                viewModel.getProductPrices(returnedProductId)
            }
        }
    }

    private fun initProductDetailViewModel() {
        val token = runBlocking { userPreferences.authToken.first() }
        val productApi = remoteDataSource.buildApi(GetProductApi::class.java, token)
        val productRepository =
            com.example.tokoplastik.data.repository.ProductRepository(productApi)

        // Buat instance ProductDetailViewModel
        productDetailViewModel = ProductDetailViewModel(productRepository)

        // Observe perubahan pada productDetail
        productDetailViewModel.productDetail.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Resource.Success -> {
                    result.data?.let { response ->
                        // Dapatkan lowestUnit dari detail produk
                        val lowestUnit = response.data.product.lowestUnit

                        // Perbarui TextView
                        binding.lowestUnit.text = lowestUnit
                    }
                }

                is Resource.Failure -> handleApiError(result)
                Resource.Loading -> {}
            }
        }
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
        pricesAdapter = ProductPricesAdapter(
            onItemClick = { item ->
                fillFormWithProductPrice(item)
            },
            onDeleteItem = { item ->
                viewModel.deleteProductPrice(item.id)
                viewModel.getProductPrices(args.productId)
                clearInputs()
            }
        )

        binding.productPricesRecyclerView.apply {
            adapter = pricesAdapter
            layoutManager = LinearLayoutManager(requireContext())

            val swipeToDeleteCallback =
                ProductPricesAdapter.createSwipeToDelete(pricesAdapter, this.context)
            val itemTouchHelper = ItemTouchHelper(swipeToDeleteCallback)
            itemTouchHelper.attachToRecyclerView(this)
        }

        binding.buttonAddPrice.setOnClickListener {
            val unit = binding.unitDropdown.text.toString().lowercase()
            val quantity = binding.quantityEditText.text.toString()
            val price = binding.priceEditText.getRawValue().toString().toIntOrNull() ?: 0

            if (validateInputs(quantity, price, unit)) {
                if (existingProductPrice != null) {
                    viewModel.updateProductPrices(
                        existingProductPrice!!.id,
                        args.productId,
                        price,
                        unit,
                        quantity
                    )
                } else {
                    viewModel.addProductPrices(args.productId, price, unit, quantity)
                }
            }
        }
    }

    private fun fillFormWithProductPrice(productPrice: ProductPrice) {
        existingProductPrice = productPrice
        binding.apply {
            unitDropdown.setText(productPrice.unit)
            quantityEditText.setText(productPrice.quantityPerUnit)

            val formattedPrice = productPrice.price.toString()
            priceEditText.setText(formattedPrice)

            buttonAddPrice.text = "Perbarui Harga Satuan"
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

        // Cek jika ini bukan update dari item yang sudah ada
        if (existingProductPrice == null && ::getProductPrices.isInitialized) {
            // Cari apakah unit sudah ada
            val duplicate = getProductPrices.find { productPrice ->
                productPrice.unit.equals(unit, ignoreCase = true)
            }

            if (duplicate != null) {
                binding.unitDropdown.error = "Unit ini sudah ada"
                isValid = false
            }
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
                        pricesAdapter.updateList(getProductPrices)
                    }
                }

                is Resource.Failure -> handleApiError(result)
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
                    // Refresh the list after adding
                    viewModel.getProductPrices(args.productId)
                }

                is Resource.Failure -> {
                    Toast.makeText(
                        requireContext(),
                        result.errorBody.toString(),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                Resource.Loading -> {}
            }
        }

        viewModel.updateResult.observe(viewLifecycleOwner) { result ->
            binding.productPricesProgressbar.visible(result is Resource.Loading)

            when (result) {
                is Resource.Success -> {
                    binding.productPricesProgressbar.visible(false)
                    Toast.makeText(
                        requireContext(),
                        "Harga berhasil diperbarui",
                        Toast.LENGTH_SHORT
                    ).show()
                    clearInputs()
                    viewModel.getProductPrices(args.productId)
                }

                is Resource.Failure -> {
                    Toast.makeText(
                        requireContext(),
                        result.errorBody.toString(),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                Resource.Loading -> {}
            }
        }
    }

    private fun clearInputs() {
        binding.apply {
            quantityEditText.text?.clear()
            priceEditText.text?.clear()
            unitDropdown.text?.clear()
            buttonAddPrice.text = "Tambah Harga Satuan"
        }
        existingProductPrice = null
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