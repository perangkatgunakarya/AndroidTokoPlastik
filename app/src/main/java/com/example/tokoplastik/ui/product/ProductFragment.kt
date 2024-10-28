package com.example.tokoplastik.ui.product

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment.Companion.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tokoplastik.R
import com.example.tokoplastik.adapter.ProductAdapter
import com.example.tokoplastik.data.UserPreferences
import com.example.tokoplastik.data.network.GetProductApi
import com.example.tokoplastik.data.repository.ProductRepository
import com.example.tokoplastik.data.responses.GetProduct
import com.example.tokoplastik.databinding.FragmentProductBinding
import com.example.tokoplastik.ui.base.BaseFragment
import com.example.tokoplastik.util.Resource
import com.example.tokoplastik.util.handleApiError
import com.example.tokoplastik.util.visible
import com.example.tokoplastik.viewmodel.ProductViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class ProductFragment : BaseFragment<ProductViewModel, FragmentProductBinding, ProductRepository> () {

    private lateinit var getProduct: List<GetProduct>
    private lateinit var productAdapter: ProductAdapter
    private var isNameSortAscending = true
    private var isPriceSortAscending = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getProduct = listOf()

        setupRecyclerView()
        observeProducts()

        setupSearchView()
        setupSortingButtons()
    }

    private fun setupRecyclerView() {
        productAdapter = ProductAdapter(getProduct).apply {
            setOnItemClickListener { product ->
                // Navigate to detail fragment/activity
                val action = ProductFragmentDirections
                    .actionProductFragmentToProductDetailFragment(product.id)
                findNavController().navigate(action)
            }
        }

        binding.productRecycler.apply {
            adapter = productAdapter
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(
                DividerItemDecoration(
                    requireContext(),
                    DividerItemDecoration.VERTICAL
                )
            )
        }

        // Load initial data
        val products = getProduct // Get your data from repository/database
        productAdapter.updateList(products)
    }

    private fun observeProducts() {
        viewModel.getProduct()
        viewModel.product.observe(viewLifecycleOwner, Observer {
            when (it) {
                is Resource.Success -> {
                    // Access the list from your response wrapper
                    it.data?.let { response ->
                        getProduct = response.data  // Adjust 'products' to match your actual property name
                        productAdapter.updateList(getProduct)
                    }
                }
                is Resource.Failure -> {
                    handleApiError(it)
                }
                is Resource.Loading -> {
                    // Show loading state if needed
                }
            }
        })
    }

    private fun setupSearchView() {
        var searchJob: Job? = null

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(300) // 300ms delay
                    productAdapter.filterProducts(newText ?: "")
                }
                return true
            }
        })
    }

    private fun setupSortingButtons() {
        binding.btnSortName.setOnClickListener {
            isNameSortAscending = !isNameSortAscending
            productAdapter.sortByName(isNameSortAscending)
            updateSortButtonIcon(binding.btnSortName, isNameSortAscending)
        }

        binding.btnSortPrice.setOnClickListener {
            isPriceSortAscending = !isPriceSortAscending
            productAdapter.sortByPrice(isPriceSortAscending)
            updateSortButtonIcon(binding.btnSortPrice, isPriceSortAscending)
        }
    }

    private fun updateSortButtonIcon(button: ImageButton, ascending: Boolean) {
        button.setImageResource(
            if (ascending) R.drawable.align_bot
            else R.drawable.align_top
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
//        _binding = null
    }

    override fun getViewModel() = ProductViewModel::class.java

    override fun getFragmentBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentProductBinding.inflate(inflater, container, false)

    override fun getFragmentRepository() : ProductRepository {
        val token = runBlocking { userPreferences.authToken.first() }
        val api = remoteDataSource.buildApi(GetProductApi::class.java, token)
        return ProductRepository(api)
    }
}