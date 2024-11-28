package com.example.tokoplastik.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import cn.pedant.SweetAlert.SweetAlertDialog
import com.example.tokoplastik.adapter.DetailHistoryAdapter
import com.example.tokoplastik.data.network.CheckoutApi
import com.example.tokoplastik.data.repository.CheckoutRepository
import com.example.tokoplastik.databinding.FragmentDetailHistoryBinding
import com.example.tokoplastik.ui.base.BaseFragment
import com.example.tokoplastik.util.HistoryReceiptGenerator
import com.example.tokoplastik.util.Resource
import com.example.tokoplastik.util.handleApiError
import com.example.tokoplastik.viewmodel.CheckoutViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class DetailHistoryFragment :
    BaseFragment<CheckoutViewModel, FragmentDetailHistoryBinding, CheckoutRepository>() {

    private val args: DetailHistoryFragmentArgs by navArgs()
    private var transactionId: Int = -1
    private lateinit var invoiceGenerator: HistoryReceiptGenerator

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        transactionId = args.transactionId
        invoiceGenerator = HistoryReceiptGenerator(requireContext())

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

        binding.buttonLunas.setOnClickListener {
            showLunasDialog(transactionId)
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
                            detailTransactions.transactionProduct,
                            detailTransactions.total.toString()
                        )
                        binding.textTransactionDetail.text = response.data.customer.name

                        val pdfFile = invoiceGenerator.generatedPdfReceipt(
                            detailTransactions,
                            detailTransactions.transactionProduct,
                            response.data.id.toString()
                        )

                        binding.buttonPrint.setOnClickListener {
                            invoiceGenerator.printReceipt(
                                detailTransactions,
                                detailTransactions.transactionProduct,
                                response.data.id.toString()
                            )
                        }
                        binding.buttonShare.setOnClickListener {
                            invoiceGenerator.shareReceipt(pdfFile)
                        }
                    }
                }

                is Resource.Failure -> {
                    handleApiError(result)
                }

                Resource.Loading -> {}
            }
        }
    }

    private fun showLunasDialog(transactionId: Int) {
        if (!isAdded) return

        context?.let { ctx ->
            SweetAlertDialog(ctx, SweetAlertDialog.NORMAL_TYPE).apply {
                titleText = "Pembayaran"
                contentText = "Apakah Anda yakin untuk set status pembayaran menjadi lunas?"

                setConfirmButton("LUNAS") {
                    dismissWithAnimation()
                    setPaymentStatus(transactionId)
                }

                setCancelButton("Cancel") {
                    dismissWithAnimation()
                }

                show()
            }
        }
    }

    private fun setPaymentStatus(transactionId: Int) {
        if (!isAdded) return

        context?.let { ctx ->
            val loadingDialog = SweetAlertDialog(ctx, SweetAlertDialog.PROGRESS_TYPE)
            loadingDialog.apply {
                titleText = "Processing"
                contentText = "Processing status..."
                setCancelable(false)
                show()
            }

            viewModel.updatePaymentStatus(transactionId, "lunas")

            viewModel.paymentStatus.observe(viewLifecycleOwner) { result ->
                if (!isAdded) {
                    loadingDialog.dismissWithAnimation()
                    return@observe
                }

                when (result) {
                    is Resource.Success -> {
                        loadingDialog.dismissWithAnimation()
                        result.data?.let { response ->
                            showSuccessDialog()
                        }
                    }

                    is Resource.Failure -> {
                        handleApiError(result)
                    }

                    Resource.Loading -> {}
                }
            }
        }
    }

    private fun showSuccessDialog() {
        if (!isAdded) return
        context?.let { ctx ->
            SweetAlertDialog(
                ctx,
                SweetAlertDialog.SUCCESS_TYPE
            ).apply {
                titleText = "Berhasil"
                contentText = "Status pembayaran berhasil diubah."

                setConfirmButton("OK") {
                    dismissWithAnimation()
                }

                show()
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