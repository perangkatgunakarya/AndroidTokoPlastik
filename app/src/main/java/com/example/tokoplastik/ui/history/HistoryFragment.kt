package com.example.tokoplastik.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tokoplastik.R
import com.example.tokoplastik.adapter.HistoryAdapter
import com.example.tokoplastik.data.network.CheckoutApi
import com.example.tokoplastik.data.repository.CheckoutRepository
import com.example.tokoplastik.data.responses.AllTransaction
import com.example.tokoplastik.databinding.FragmentHistoryBinding
import com.example.tokoplastik.ui.base.BaseFragment
import com.example.tokoplastik.util.Resource
import com.example.tokoplastik.util.handleApiError
import com.example.tokoplastik.util.visible
import com.example.tokoplastik.viewmodel.CheckoutViewModel
import com.example.tokoplastik.viewmodel.SortOrder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


class HistoryFragment :
    BaseFragment<CheckoutViewModel, FragmentHistoryBinding, CheckoutRepository>() {

    private lateinit var transactions: List<AllTransaction>
    private lateinit var historyAdapter: HistoryAdapter
    private var isDateSortAscending: Boolean = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.historyProgressBar.visible(false)

        transactions = listOf()

        setupRecyclerView()
        setupSwipeRefresh()
        observeTransactions()

        setupSearchView()
        setupSortingButtons()
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter().apply {
            setOnItemClickListener { transaction ->
                val directions = HistoryFragmentDirections
                    .actionHistoryFragmentToDetailHistoryFragment(transaction.id)
                findNavController().navigate(directions)
            }
        }
        binding.historyRecycler.apply {
            adapter = historyAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshHistory.apply {
            setOnRefreshListener {
                viewModel.getTransactions()
            }
        }
    }

    private fun observeTransactions() {
        viewModel.getTransactions()
        viewModel.transactions.observe(viewLifecycleOwner) { result ->
            binding.swipeRefreshHistory.isRefreshing = false
            binding.historyProgressBar.visible(result is Resource.Loading)

            when (result) {
                is Resource.Success -> {
                    binding.historyProgressBar.visible(false)
                    result.data?.let { response ->
                        transactions = response.data
                        historyAdapter.updateList(transactions)
                    }
                }
                is Resource.Failure -> {
                    binding.historyProgressBar.visible(false)
                    handleApiError(result)
                }
                Resource.Loading -> binding.historyProgressBar.visible(true)
            }
        }

        viewModel.isDateSortAscending.observe(viewLifecycleOwner) { it ->
            historyAdapter.sortByDate(it)
        }
    }

    private fun setupSearchView() {
        var searchJob: Job? = null

        binding.searchView.apply {
            setOnQueryTextListener(object: SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return false
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    searchJob?.cancel()
                    searchJob = lifecycleScope.launch {
                        delay(300)
                        historyAdapter.filterTransactions(newText ?: "")
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
            val bottomSheet = SortFilterBottomSheet()
            bottomSheet.show(childFragmentManager, "SORT_FILTER_BOTTOM_SHEET")
        }
    }

    private fun updateSortButtonIcon(button: ImageButton, ascending: Boolean) {
        button.setImageResource(
            if (ascending) R.drawable.align_bot
            else R.drawable.align_top
        )
    }

    override fun getViewModel() = CheckoutViewModel::class.java

    override fun getFragmentBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentHistoryBinding.inflate(inflater, container, false)

    override fun getFragmentRepository(): CheckoutRepository {
        val token = runBlocking { userPreferences.authToken.first() }
        val api = remoteDataSource.buildApi(CheckoutApi::class.java, token)
        return CheckoutRepository(api)
    }

}