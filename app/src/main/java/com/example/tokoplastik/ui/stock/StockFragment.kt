package com.example.tokoplastik.ui.stock

import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import cn.pedant.SweetAlert.SweetAlertDialog
import com.example.tokoplastik.R
import com.example.tokoplastik.adapter.StockAdapter
import com.example.tokoplastik.data.network.StockApi
import com.example.tokoplastik.data.repository.StockRepository
import com.example.tokoplastik.data.responses.Stock
import com.example.tokoplastik.databinding.FragmentStockBinding
import com.example.tokoplastik.ui.base.BaseFragment
import com.example.tokoplastik.ui.product.ProductSortBottomSheet
import com.example.tokoplastik.util.Resource
import com.example.tokoplastik.util.handleApiError
import com.example.tokoplastik.util.visible
import com.example.tokoplastik.viewmodel.StockViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class StockFragment : BaseFragment<StockViewModel, FragmentStockBinding, StockRepository>() {

    private val args: StockFragmentArgs by navArgs()
    private lateinit var stocks: List<Stock>
    private lateinit var stockAdapter: StockAdapter
    private var productId: Int = -1

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.stockProgressBar.visible(false)

        stocks = listOf()
        productId = args.productId
        viewModel.productId = productId

        binding.buttonBack.setOnClickListener {
            requireActivity().onBackPressed()
        }

        binding.buttonAddStock.setOnClickListener {
            val bottomSheet = RestockQuantityBottomSheet()
            bottomSheet.show(childFragmentManager, "RESTOCK_QUANTITY_BOTTOM_SHEET")
        }

        binding.menuIcon.setOnClickListener {
            showPopupMenu(it)
        }

        viewModel.addStockStatus.observe(viewLifecycleOwner) {
            if (it == true) {
                SweetAlertDialog(requireContext(), SweetAlertDialog.SUCCESS_TYPE).apply {
                    titleText = "Success!"
                    contentText = "Barang berhasil direstock"
                    setCanceledOnTouchOutside(false)
                    setConfirmButton("OK") {
                        dismissWithAnimation()
                    }
                    show()
                }
                viewModel.addStockStatus(false)
                observeStocks()
            }
        }

        setupSwipeRefresh()
        setupRecyclerView()
        observeStocks()
    }

    private fun showPopupMenu(view: View) {
        val popupMenu = androidx.appcompat.widget.PopupMenu(requireContext(), view)
        popupMenu.inflate(R.menu.stock_fragment_menu)
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_filter -> {
                    val bottomSheet = SortStockBottomSheet()
                    bottomSheet.show(childFragmentManager, "STOCK_SORT_BOTTOM_SHEET")
                    true
                }

                else -> false
            }
        }
        popupMenu.show()
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshStock.apply {
            setOnRefreshListener {
                viewModel.getStockByProductId(productId)
            }
        }
    }

    private fun setupRecyclerView() {
        stockAdapter = StockAdapter(
            onDeleteItem = { item ->
                viewModel.deleteStock(item.id)
            }
        )

        binding.stockRecycler.apply {
            adapter = stockAdapter
            layoutManager = LinearLayoutManager(requireContext())

            val swipeToDeleteCallback = StockAdapter.createSwipeToDelete(stockAdapter, this.context)
            val itemTouchHelper = ItemTouchHelper(swipeToDeleteCallback)
            itemTouchHelper.attachToRecyclerView(this)
        }
    }

    private fun observeStocks() {
        viewModel.getStockByProductId(productId)
        viewModel.stock.observe(viewLifecycleOwner) { result ->
            binding.swipeRefreshStock.isRefreshing = false
            binding.stockProgressBar.visible(result is Resource.Loading)

            when (result) {
                is Resource.Success -> {
                    binding.stockProgressBar.visible(false)
                    result.data?.let { response ->
                        stocks = response.data
                        stockAdapter.updateList(stocks)
                        if (response.data.isNotEmpty()) {
                            binding.textStock.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
                            binding.textSupplier.visible(true)
                            binding.textStock.text = "Stock of ${response.data[0].product?.name}"
                            binding.textSupplier.text = "(${response.data[0].product?.supplier})"
                        } else {
                            binding.textSupplier.visible(false)
                            binding.textStock.textAlignment = View.TEXT_ALIGNMENT_CENTER
                            binding.textStock.textSize = 16F
                            binding.textStock.text = "Data stock masih kosong"
                        }
                    }
                }

                is Resource.Failure -> {
                    binding.stockProgressBar.visible(false)
                    handleApiError(result)
                }

                Resource.Loading -> {
                    binding.stockProgressBar.visible(true)
                }
            }
        }

        viewModel.isDateSortAscending.observe(viewLifecycleOwner) { it ->
            stockAdapter.sortByDate(it)
        }
    }

    override fun getViewModel() = StockViewModel::class.java

    override fun getFragmentBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentStockBinding.inflate(inflater, container, false)

    override fun getFragmentRepository(): StockRepository {
        val token = runBlocking { userPreferences.authToken.first() }
        val api = remoteDataSource.buildApi(StockApi::class.java, token)
        return StockRepository(api)
    }

}