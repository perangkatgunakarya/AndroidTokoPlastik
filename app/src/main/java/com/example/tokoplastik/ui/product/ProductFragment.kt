package com.example.tokoplastik.ui.product

import android.content.Intent
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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.tokoplastik.R
import com.example.tokoplastik.adapter.ProductAdapter
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
        setupSwipeRefresh()
        observeProducts()

        setupSearchView()
        setupSortingButtons()

        binding.buttonAddProduct.setOnClickListener {
            val intent = Intent(requireContext(), AddProductActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshProduct.apply {
            setOnRefreshListener {
                viewModel.getProduct()
            }
        }
    }

    private fun setupRecyclerView() {
        productAdapter = ProductAdapter(getProduct).apply {
            setOnItemClickListener { product ->
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
    }

    private fun observeProducts() {
        viewModel.getProduct()
        viewModel.product.observe(viewLifecycleOwner) { result ->
            binding.swipeRefreshProduct.isRefreshing = false

            when (result) {
                is Resource.Success -> {
                    result.data?.let { response ->
                        getProduct = response.data
                        productAdapter.updateList(getProduct)
                    }
                }
                is Resource.Failure -> {
                    handleApiError(result)
                }
                is Resource.Loading -> {  }
            }
        }
    }

    private fun setupSearchView() {
        var searchJob: Job? = null

        binding.searchView.apply {
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
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

            isIconified = false
            setIconifiedByDefault(false)
            clearFocus()
        }
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