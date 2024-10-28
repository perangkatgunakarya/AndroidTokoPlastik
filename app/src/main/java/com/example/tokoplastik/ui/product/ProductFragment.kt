package com.example.tokoplastik.ui.product

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class ProductFragment : BaseFragment<ProductViewModel, FragmentProductBinding, ProductRepository> () {

    private lateinit var getProduct: List<GetProduct>
    private lateinit var productAdapter: ProductAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getProduct = listOf()

        setupRecyclerView()
        observeProducts()
    }

    private fun setupRecyclerView() {
        productAdapter = ProductAdapter(getProduct)
        binding.productRecycler.apply {
            adapter = productAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
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