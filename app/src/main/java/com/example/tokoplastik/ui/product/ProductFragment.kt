package com.example.tokoplastik.ui.product

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.appcompat.widget.SearchView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
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
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class ProductFragment :
    BaseFragment<ProductViewModel, FragmentProductBinding, ProductRepository>() {

    private lateinit var getProduct: List<GetProduct>
    private lateinit var productAdapter: ProductAdapter
    private var isNameSortAscending = true
    private var isPriceSortAscending = true
    private var lastViewedProductId: Int? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.productProgressBar.visible(false)

        getProduct = listOf()

        setupRecyclerView()
        setupSwipeRefresh()
        observeProducts()

        setupSearchView()
        binding.root.requestFocus() // Memberikan fokus ke root layout agar SearchView tidak mendapat fokus


        // Atur listener untuk WindowInsets
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())

            // Sesuaikan margin bottom button_add_product
            val params = binding.buttonAddProduct.layoutParams as ViewGroup.MarginLayoutParams
            params.bottomMargin =
                insets.bottom + resources.getDimensionPixelSize(R.dimen.add_button_margin) // Tambahkan margin tambahan
            binding.buttonAddProduct.layoutParams = params

            // Kembalikan insets yang telah dikonsumsi
            WindowInsetsCompat.CONSUMED
        }

        binding.buttonAddProduct.setOnClickListener {
            val intent = Intent(requireContext(), AddProductActivity::class.java)
            startActivity(intent)
        }

        binding.searchIcon.setOnClickListener {
            binding.toolbarDetailProduct.visible(false)
        }

        binding.closeIcon.setOnClickListener {
            binding.toolbarDetailProduct.visible(true)
        }

        binding.menuIcon.setOnClickListener {
            showPopupMenu(it)
        }
    }


    private fun showPopupMenu(view: View) {
        val popupMenu = androidx.appcompat.widget.PopupMenu(requireContext(), view)
        popupMenu.inflate(R.menu.product_fragment_menu)
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_filter -> {
                    val bottomSheet = ProductSortBottomSheet()
                    bottomSheet.show(childFragmentManager, "PRODUCT_SORT_BOTTOM_SHEET")
                    true
                }

                else -> false
            }
        }
        popupMenu.show()
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
                // Simpan ID produk yang diklik
                lastViewedProductId = product.id

                val directions =
                    ProductFragmentDirections.actionProductFragmentToProductDetailFragment(product.id)
                findNavController().navigate(directions)
            }
        }

        binding.productRecycler.apply {
            adapter = productAdapter
            layoutManager = LinearLayoutManager(requireContext())

            val swipeToDeleteCallback =
                ProductAdapter.createSwipeToDelete(productAdapter, this.context)
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

                        // Setelah data dimuat, scroll ke posisi produk yang terakhir dilihat
                        scrollToLastViewedProduct()
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

    private fun scrollToLastViewedProduct() {
        // Hanya scroll jika memiliki ID produk terakhir
        lastViewedProductId?.let { productId ->
            val position = getProduct.indexOfFirst { it.id == productId }
            if (position != -1) {
                binding.productRecycler.post {
                    (binding.productRecycler.layoutManager as? LinearLayoutManager)?.let { layoutManager ->
                        val offset = binding.productRecycler.height / 2
                        layoutManager.scrollToPositionWithOffset(position, offset)
                    }

                }
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

//            isIconified = false
            setIconifiedByDefault(false)
            clearFocus()
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

    override fun getFragmentRepository(): ProductRepository {
        val token = runBlocking { userPreferences.authToken.first() }
        val api = remoteDataSource.buildApi(GetProductApi::class.java, token)
        return ProductRepository(api)
    }
}