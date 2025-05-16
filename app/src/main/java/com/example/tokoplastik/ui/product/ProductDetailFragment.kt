package com.example.tokoplastik.ui.product

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import cn.pedant.SweetAlert.SweetAlertDialog
import com.example.tokoplastik.R
import com.example.tokoplastik.data.network.GetProductApi
import com.example.tokoplastik.data.repository.ProductRepository
import com.example.tokoplastik.databinding.FragmentProductDetailBinding
import com.example.tokoplastik.ui.base.BaseFragment
import com.example.tokoplastik.util.Resource
import com.example.tokoplastik.util.getRawValue
import com.example.tokoplastik.util.handleApiError
import com.example.tokoplastik.util.setNumberFormatter
import com.example.tokoplastik.viewmodel.ProductDetailViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class ProductDetailFragment :
    BaseFragment<ProductDetailViewModel, FragmentProductDetailBinding, ProductRepository>() {

    private val args: ProductDetailFragmentArgs by navArgs()
    private var productId: Int = -1
    private lateinit var spinner: Spinner
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
    private var defaultPosition: Int = 0
    private var latestCapitalPrice: Int? = null
    private var newestCapitalPrice: Int? = null
    private lateinit var currentCapitalInput: EditText

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        productId = args.productId
        spinner = binding.lowestUnit

        currentCapitalInput = view.findViewById(R.id.currentCapital)
        currentCapitalInput.setNumberFormatter()

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            binding.productDetailScrollView.updatePadding(bottom = imeHeight)
            insets
        }

        setupViews()
        setupObservers()
        setupSaveButton()
    }

    private fun setupViews() {

        binding.buttonBack.setOnClickListener {
            // Siapkan bundle dengan data yang ingin dikirim
            val bundle = Bundle().apply {
                putInt("productId", productId)
            }

            // Navigasi kembali dan kirim bundle
            findNavController().navigate(R.id.productFragment, bundle)
        }

        binding.stockCardButton.setOnClickListener {
            val name = binding.productName.text.toString()
            val supplier = binding.supplierName.text.toString()
            val notes = binding.notes.text.toString()
            var latest: Int?
            var newest: Int?

            if (binding.currentCapital.text.toString() == "") {
                latest = latestCapitalPrice
                newest = newestCapitalPrice
            } else {
                newest = currentCapitalInput.getRawValue()
                latest = currentCapitalInput.getRawValue()
            }

            val lowestUnit = selectedUnit

            // Hanya memanggil update product, tanpa mendaftarkan observer baru
            viewModel.updateProduct(productId, name, supplier, latest, newest, lowestUnit, notes)

            val intent = Intent(requireContext(), AddProductActivity::class.java).apply {
                putExtra("openProductPriceFragment", true)
                putExtra("productId", productId)
            }
            addStockProductLauncher.launch(intent)

//            val direction =
//                ProductDetailFragmentDirections.actionDetailProductFragmentToStockFragment(productId)
//            findNavController().navigate(direction)
        }

        binding.goToProductPriceButton.setOnClickListener {
            val name = binding.productName.text.toString()
            val supplier = binding.supplierName.text.toString()
            val notes = binding.notes.text.toString()
            var latest: Int?
            var newest: Int?

            if (binding.currentCapital.text.toString() == "") {
                latest = latestCapitalPrice
                newest = newestCapitalPrice
            } else {
                newest = currentCapitalInput.getRawValue()
                latest = currentCapitalInput.getRawValue()
            }

            val lowestUnit = selectedUnit

            viewModel.updateProduct(productId, name, supplier, latest, newest, lowestUnit, notes)

            val intent = Intent(requireContext(), AddProductActivity::class.java).apply {
                putExtra("openProductPriceFragment", true)
                putExtra("productId", productId)
            }
            addProductPricesLauncher.launch(intent)
        }

//        spinner
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            units
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parentView: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                selectedUnit = units[position]
            }

            override fun onNothingSelected(parentView: AdapterView<*>?) {
                selectedUnit = units[defaultPosition]
            }
        }
    }

    private fun setupObservers() {
        viewModel.getProductDetail(productId)
        viewModel.productDetail.observe(viewLifecycleOwner, Observer {
            when (it) {
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.textProductDetail.text = it.data?.data?.product?.name
                    binding.productName.setText(it.data?.data?.product?.name)
                    binding.supplierName.setText(it.data?.data?.product?.supplier)

                    val symbols = DecimalFormatSymbols(Locale.getDefault()).apply {
                        groupingSeparator = '.'
                    }
                    val formatter = DecimalFormat("#,###", symbols)
                    val formattedLatestCapital =
                        formatter.format(it.data?.data?.product?.newestCapitalPrice)

                    binding.latestCapital.setText("Rp${formattedLatestCapital}")
                    binding.latestStock.setText("${it.data?.data?.product?.latestStock}")
                    binding.notes.setText(it.data?.data?.product?.notes)

                    val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'")
                    parser.timeZone = TimeZone.getTimeZone("UTC")
                    val date = parser.parse(it.data?.data?.product?.updatedAt)
                    binding.latestCapitalDate.text =
                        "Terakhir diperbarui: ${SimpleDateFormat("dd MMM Y").format(date)}"

                    defaultPosition = units.indexOf(it.data?.data?.product?.lowestUnit)
                    spinner.setSelection(defaultPosition)

                    latestCapitalPrice = it.data?.data?.product?.capitalPrice
                    newestCapitalPrice = it.data?.data?.product?.newestCapitalPrice
                }

                is Resource.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }

                is Resource.Failure -> {
                    binding.progressBar.visibility = View.GONE
                    handleApiError(it)
                }
            }
        })
    }

    private val addProductPricesLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val returnedProductId = data?.getIntExtra("PRODUCT_ID", -1) ?: -1
            if (returnedProductId != -1) {
                viewModel.getProductDetail(returnedProductId)
            }
        }
    }
    private val addStockProductLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val returnedProductId = data?.getIntExtra("PRODUCT_ID", -1) ?: -1
            if (returnedProductId != -1) {
                viewModel.getProductDetail(returnedProductId)
            }
        }
    }

    private fun setupSaveButton() {
        viewModel.updateProduct.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(
                        requireContext(),
                        "Harga berhasil ditambahkan",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.saveButton.isEnabled = true
                }

                is Resource.Failure -> {
                    binding.progressBar.visibility = View.GONE
                    handleApiError(result)
                    binding.saveButton.isEnabled = true
                }

                is Resource.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.saveButton.isEnabled = false
                }
            }
        }

        binding.saveButton.setOnClickListener {
            val name = binding.productName.text.toString()
            val supplier = binding.supplierName.text.toString()
            val notes = binding.notes.text.toString()
            var latest: Int?
            var newest: Int?

            if (binding.currentCapital.text.toString() == "") {
                latest = latestCapitalPrice
                newest = newestCapitalPrice
            } else {
                newest = currentCapitalInput.getRawValue()
                latest = currentCapitalInput.getRawValue()
            }

            val lowestUnit = selectedUnit

            // Hanya memanggil update product, tanpa mendaftarkan observer baru
            viewModel.updateProduct(productId, name, supplier, latest, newest, lowestUnit, notes)
        }
    }

    override fun getViewModel() = ProductDetailViewModel::class.java

    override fun getFragmentBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentProductDetailBinding.inflate(inflater, container, false)

    override fun getFragmentRepository(): ProductRepository {
        val token = runBlocking { userPreferences.authToken.first() }
        val api = remoteDataSource.buildApi(GetProductApi::class.java, token)
        return ProductRepository(api)
    }

}