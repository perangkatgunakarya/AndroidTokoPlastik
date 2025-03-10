package com.example.tokoplastik.ui.customer

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tokoplastik.R
import com.example.tokoplastik.adapter.CustomerAdapter
import com.example.tokoplastik.adapter.ProductAdapter
import com.example.tokoplastik.data.network.CustomerApi
import com.example.tokoplastik.data.repository.CustomerRepository
import com.example.tokoplastik.data.responses.Customer
import com.example.tokoplastik.databinding.FragmentCustomerBinding
import com.example.tokoplastik.ui.base.BaseFragment
import com.example.tokoplastik.ui.product.ProductFragmentDirections
import com.example.tokoplastik.ui.product.ProductSortBottomSheet
import com.example.tokoplastik.util.Resource
import com.example.tokoplastik.util.handleApiError
import com.example.tokoplastik.util.visible
import com.example.tokoplastik.viewmodel.CustomerViewModel
import com.example.tokoplastik.viewmodel.SortType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class CustomerFragment : BaseFragment<CustomerViewModel, FragmentCustomerBinding, CustomerRepository>() {

    private lateinit var getCustomer: List<Customer>
    private lateinit var customerAdapter: CustomerAdapter
    private var isNameSortAscending = true
    private var isPriceSortAscending = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.customerProgressBar.visible(false)

        getCustomer = listOf()

        setupRecyclerView()
        setupSwipeRefresh()
        observeCustomers()

        setupSearchView()
        binding.root.requestFocus()

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())

            val params = binding.buttonAddCustomer.layoutParams as ViewGroup.MarginLayoutParams
            params.bottomMargin = insets.bottom + resources.getDimensionPixelSize(R.dimen.add_button_margin)
            binding.buttonAddCustomer.layoutParams = params

            WindowInsetsCompat.CONSUMED
        }

        binding.buttonAddCustomer.setOnClickListener {
            val directions = CustomerFragmentDirections.actionCustomerFragmentToAddCustomerFragment()
            findNavController().navigate(directions)
        }

        binding.searchIcon.setOnClickListener {
            binding.toolbarDetailCustomer.visible(false)
        }

        binding.closeIcon.setOnClickListener {
            binding.toolbarDetailCustomer.visible(true)
        }

        binding.menuIcon.setOnClickListener {
            showPopupMenu(it)
        }
    }

    private fun showPopupMenu(view: View) {
        val popupMenu = androidx.appcompat.widget.PopupMenu(requireContext(), view)
        popupMenu.inflate(R.menu.customer_fragment_menu)
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_filter -> {
                    val bottomSheet = CustomerSortBottomSheet()
                    bottomSheet.show(childFragmentManager, "CUSTOMER_SORT_BOTTOM_SHEET")
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshCustomer.apply {
            setOnRefreshListener {
                viewModel.getAllCustomer()
            }
        }
    }

    private fun setupRecyclerView() {
        customerAdapter = CustomerAdapter()
        customerAdapter.apply {
            setOnItemClickListener { customer ->
                val directions = CustomerFragmentDirections.actionCustomerFragmentToUpdateCustomerFragment(customer.id)
                findNavController().navigate(directions)
            }
        }

        binding.customerRecycler.apply {
            adapter = customerAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observeCustomers() {
        viewModel.getAllCustomer()
        viewModel.getAllCustomer.observe(viewLifecycleOwner) { result ->
            binding.swipeRefreshCustomer.isRefreshing = false
            binding.customerProgressBar.visible(result is Resource.Loading)

            when (result) {
                is Resource.Success -> {
                    binding.customerProgressBar.visible(false)
                    result.data?.let { response ->
                        getCustomer = response.data
                        customerAdapter.updateList(getCustomer)
                    }
                }
                is Resource.Failure -> {
                    binding.customerProgressBar.visible(false)
                    handleApiError(result)
                }
                is Resource.Loading -> {
                    binding.customerProgressBar.visible(true)
                }
            }
        }

        viewModel.sortType.observe(viewLifecycleOwner) {
            when (it) {
                SortType.NAME -> viewModel.isDataSortAscending.observe(viewLifecycleOwner) { ascending ->
                    when (ascending) {
                        true -> customerAdapter.sortByName(true)
                        false -> customerAdapter.sortByName(false)
                    }
                }
                SortType.DATE -> TODO()
                SortType.CAPITAL -> TODO()
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
                        customerAdapter.filterCustomers(newText ?: "")
                    }
                    return true
                }
            })

            setIconifiedByDefault(false)
            clearFocus()
        }
    }

    override fun getViewModel() = CustomerViewModel::class.java

    override fun getFragmentBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentCustomerBinding.inflate(inflater, container, false)

    override fun getFragmentRepository(): CustomerRepository {
        val token = runBlocking { userPreferences.authToken.first() }
        val api = remoteDataSource.buildApi(CustomerApi::class.java, token)
        return CustomerRepository(api)
    }

}