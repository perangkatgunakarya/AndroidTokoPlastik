package com.example.tokoplastik.ui.product

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

        setupViews()
        setupObservers()
        setupSaveButton()
    }

    private fun setupViews() {
        binding.notes.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                view.post {
                    val scrollY = view.top
                    binding.root.scrollTo(0, scrollY)
                }
            }
        }

        binding.buttonBack.setOnClickListener {
            requireActivity().onBackPressed()
        }

        binding.stockCardButton.setOnClickListener {
            val direction =
                ProductDetailFragmentDirections.actionDetailProductFragmentToStockFragment(productId)
            findNavController().navigate(direction)
        }

        binding.goToProductPriceButton.setOnClickListener {
            val intent = Intent(requireContext(), AddProductActivity::class.java).apply {
                putExtra("openProductPriceFragment", true)
                putExtra("productId", productId)
            }
            startActivity(intent)
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

    private fun setupSaveButton() {
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

//            if (newestCapitalPrice == 0) {
//                if (binding.currentCapital.text.toString() == "") {
//                    latest = binding.currentCapital.text.toString().toInt()
//                    newest = binding.currentCapital.text.toString().toInt()
//                } else {
//                    latest = 0
//                    newest = 0
//                }
//            } else {
//                if (binding.currentCapital.text.toString() == "") {
//                    newest = 0
//                } else {
//                    newest = binding.currentCapital.text?.toString()?.toInt()
//                }
//                latest = newestCapitalPrice
//            }

            val lowestUnit = selectedUnit

            viewModel.updateProduct(productId, name, supplier, latest, newest, lowestUnit, notes)
            viewModel.updateProduct.observe(viewLifecycleOwner, Observer {
                when (it) {
                    is Resource.Success -> {
                        binding.progressBar.visibility = View.GONE
                        SweetAlertDialog(context, SweetAlertDialog.SUCCESS_TYPE).apply {
                            contentText = "Data berhasil diperbarui"
                            setConfirmButton("OK") {
                                dismissWithAnimation()
                            }
                            show()
                        }
                        requireActivity().onBackPressed()
                    }

                    is Resource.Failure -> {
                        binding.progressBar.visibility = View.GONE
                        handleApiError(it)
                    }

                    is Resource.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                }
            })
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