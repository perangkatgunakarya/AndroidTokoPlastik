package com.example.tokoplastik.ui.product

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tokoplastik.R
import com.example.tokoplastik.adapter.ProductAdapter
import com.example.tokoplastik.data.network.GetProductApi
import com.example.tokoplastik.data.repository.ProductRepository
import com.example.tokoplastik.data.responses.GetProduct
import com.example.tokoplastik.databinding.FragmentProductBinding
import com.example.tokoplastik.ui.base.BaseFragment
import com.example.tokoplastik.ui.history.SortFilterBottomSheet
import com.example.tokoplastik.util.Resource
import com.example.tokoplastik.util.handleApiError
import com.example.tokoplastik.util.visible
import com.example.tokoplastik.viewmodel.ProductViewModel
import com.example.tokoplastik.viewmodel.SortType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class ProductFragment : BaseFragment<ProductViewModel, FragmentProductBinding, ProductRepository> () {

    private lateinit var getProduct: List<GetProduct>
    private lateinit var productAdapter: ProductAdapter
    private var isNameSortAscending = true
    private var isPriceSortAscending = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.productProgressBar.visible(false)

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
        productAdapter = ProductAdapter(
            onDeleteItem = { item ->
                viewModel.deleteProduct(item.id)
            }
        )
        productAdapter.apply {
            setOnItemClickListener { product ->
                    val intent = Intent(requireContext(), AddProductActivity::class.java).apply {
                        putExtra("openProductPriceFragment", true)
                        putExtra("productId", product.id)
                    }
                    startActivity(intent)
            }
        }

        binding.productRecycler.apply {
            adapter = productAdapter
            layoutManager = LinearLayoutManager(requireContext())

            val swipeToDeleteCallback = ProductAdapter.createSwipeToDelete(productAdapter, this.context)
            val itemTouchHelper = ItemTouchHelper(swipeToDeleteCallback)
            itemTouchHelper.attachToRecyclerView(this)
        }
    }

    private fun observeProducts() {
        viewModel.getProduct()
        viewModel.product.observe(viewLifecycleOwner) { result ->
            binding.swipeRefreshProduct.isRefreshing = false
            binding.productProgressBar.visible(result is Resource.Loading)

            when (result) {
                is Resource.Success -> {
                    binding.productProgressBar.visible(false)
                    result.data?.let { response ->
                        getProduct = response.data
                        productAdapter.updateList(getProduct)
                    }
                }
                is Resource.Failure -> {
                    binding.productProgressBar.visible(false)
                    handleApiError(result)
                }
                is Resource.Loading -> {
                    binding.productProgressBar.visible(true)
                }
            }
        }

        viewModel.sortType.observe(viewLifecycleOwner) {
            when (it) {
                SortType.NAME -> viewModel.isDataSortAscending.observe(viewLifecycleOwner) { ascending ->
                    when (ascending) {
                        true -> productAdapter.sortByName(true)
                        false -> productAdapter.sortByName(false)
                    }
                }
                SortType.CAPITAL -> viewModel.isDataSortAscending.observe(viewLifecycleOwner) { ascending ->
                    when (ascending) {
                        true -> productAdapter.sortByPrice(true)
                        false -> productAdapter.sortByPrice(false)
                    }
                }
                SortType.DATE -> TODO()
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
        binding.sortFilterFab.setOnClickListener {
            val bottomSheet = ProductSortBottomSheet()
            bottomSheet.show(childFragmentManager, "PRODUCT_SORT_BOTTOM_SHEET")
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