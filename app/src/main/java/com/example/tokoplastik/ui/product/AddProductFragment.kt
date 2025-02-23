package com.example.tokoplastik.ui.product

import android.R
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.example.tokoplastik.data.network.AddProductApi
import com.example.tokoplastik.data.repository.AddProductRepository
import com.example.tokoplastik.databinding.FragmentAddProductBinding
import com.example.tokoplastik.ui.base.BaseFragment
import com.example.tokoplastik.util.Resource
import com.example.tokoplastik.util.getRawValue
import com.example.tokoplastik.util.handleApiError
import com.example.tokoplastik.util.setNumberFormatter
import com.example.tokoplastik.util.visible
import com.example.tokoplastik.viewmodel.AddProductViewModel
import com.example.tokoplastik.viewmodel.ProductViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class AddProductFragment : BaseFragment<AddProductViewModel, FragmentAddProductBinding, AddProductRepository> () {

    private val units = listOf("pcs", "unit", "pack", "unit", "buah", "pasang", "kotak", "lusin", "lembar", "keping", "batang", "bungkus", "potong", "tablet", "ekor", "rim", "karat", "botol", "butir", "roll", "dus", "karung", "koli", "sak", "bal", "kaleng", "set", "slop", "gulung", "ton", "kg", "gram", "mg", "meter", "m2", "m3", "inch", "cc", "liter")
    private var selectedUnit: String? = null

    private lateinit var capitalPriceInput: EditText

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        capitalPriceInput = view.findViewById(com.example.tokoplastik.R.id.capital_price_text_field)
        capitalPriceInput.setNumberFormatter()

        binding.buttonBack.setOnClickListener {
            requireActivity().onBackPressed()
        }

        binding.addProductProgressBar.visible(false)

        setupViews()
        observeViewModel()
    }

    private fun setupViews () {
        binding.buttonAddProduct.setOnClickListener {
            val name = binding.productNameTextField.text.toString()
            val supplier = binding.supplierTextField.text.toString()
            val capitalPrice = binding.capitalPriceTextField.getRawValue().toString()
            val lowesUnit = binding.unitDropdown.text.toString()
            val notes = binding.notesTextField.text.toString()

            if (validateInputs(name, supplier, capitalPrice, lowesUnit)) {
                viewModel.addProduct(name, supplier, capitalPrice, capitalPrice, lowesUnit, notes)
            }
        }

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

    private fun validateInputs(name: String, supplier: String, capitalPrice: String, lowesUnit: String): Boolean {
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

        if (lowesUnit.isEmpty()) {
            binding.unitDropdown.error = "Satuan tidak boleh kosong"
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
                    val directions = AddProductFragmentDirections
                        .actionAddProductFragmentToAddProductPricesFragment(result.data?.data?.id!!)
                    findNavController().navigate(directions)
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