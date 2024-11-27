package com.example.tokoplastik.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tokoplastik.adapter.DetailHistoryAdapter
import com.example.tokoplastik.data.network.CheckoutApi
import com.example.tokoplastik.data.repository.CheckoutRepository
import com.example.tokoplastik.databinding.FragmentDetailHistoryBinding
import com.example.tokoplastik.ui.base.BaseFragment
import com.example.tokoplastik.util.Resource
import com.example.tokoplastik.util.handleApiError
import com.example.tokoplastik.viewmodel.CheckoutViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class DetailHistoryFragment : BaseFragment<CheckoutViewModel, FragmentDetailHistoryBinding, CheckoutRepository>() {

    private val args: DetailHistoryFragmentArgs by navArgs()
    private var transactionId: Int = -1

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        transactionId = args.transactionId

        setupViews()
        setupObservers()
    }

    private fun setupViews() {
        binding.historyDetailRecycler.apply {
            adapter = DetailHistoryAdapter()
            layoutManager = LinearLayoutManager(requireContext())
        }

        binding.buttonBack.setOnClickListener {
            requireActivity().onBackPressed()
        }
    }

    private fun setupObservers() {
        viewModel.getTransactionDetail(transactionId)
        viewModel.transactionDetail.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Resource.Success -> {
                    result.data?.let { response ->
                        val detailTransactions = response.data
                        (binding.historyDetailRecycler.adapter as DetailHistoryAdapter).updateList(
                            detailTransactions.transactionProduct, detailTransactions.total.toString()
                        )
                        binding.textTransactionDetail.text = response.data.customer.name
                    }
                }
                is Resource.Failure -> {
                    handleApiError(result)
                }
                Resource.Loading -> {}
            }
        }
    }

    override fun getViewModel() = CheckoutViewModel::class.java

    override fun getFragmentBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentDetailHistoryBinding.inflate(inflater, container, false)

    override fun getFragmentRepository(): CheckoutRepository {
        val token = runBlocking { userPreferences.authToken.first() }
        val api = remoteDataSource.buildApi(CheckoutApi::class.java, token)
        return CheckoutRepository(api)
    }

}