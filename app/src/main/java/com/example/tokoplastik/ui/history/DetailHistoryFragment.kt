package com.example.tokoplastik.ui.history

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import cn.pedant.SweetAlert.SweetAlertDialog
import com.example.tokoplastik.adapter.DetailHistoryAdapter
import com.example.tokoplastik.data.network.CheckoutApi
import com.example.tokoplastik.data.repository.CheckoutRepository
import com.example.tokoplastik.data.responses.TransactionDetail
import com.example.tokoplastik.databinding.FragmentDetailHistoryBinding
import com.example.tokoplastik.ui.base.BaseFragment
import com.example.tokoplastik.ui.transaction.PaidBottomSheet
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
    private lateinit var detailTransactions: TransactionDetail
    private lateinit var invoiceGenerator: HistoryReceiptGenerator
    private lateinit var statusPayment : String
    private var totalPaid : Int = 0

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
            val bottomSheet = CreditBottomSheet()
            bottomSheet.show(childFragmentManager, "CREDIT_BOTTOM_SHEET")
        }
    }

    private fun setupObservers() {
        viewModel.getTransactionDetail(transactionId)
        viewModel.transactionDetail.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Resource.Success -> {
                    result.data?.let { response ->
                        detailTransactions = response.data

                        if (detailTransactions.paymentStatus == "lunas") {
                            binding.buttonLunas.visibility = View.GONE
                        } else {
                            binding.buttonLunas.visibility = View.VISIBLE
                        }

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

        viewModel.paidAmount.observe(viewLifecycleOwner) { amount ->
            Log.d("DetailHistoryFragment", "Paid amount: $amount")
            if (amount == 0) {
                statusPayment = "belum lunas"
                totalPaid = 0
                setPaymentStatus(transactionId, statusPayment, totalPaid)
            }
            if (amount > 0 && amount < detailTransactions.total) {
                statusPayment = "dalam cicilan"
                totalPaid = amount
                setPaymentStatus(transactionId, statusPayment, totalPaid)
            }
            else {
                statusPayment = "lunas"
                totalPaid = amount
                setPaymentStatus(transactionId, statusPayment, totalPaid)
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
                    setPaymentStatus(transactionId, "lunas", 0)
                }

                setCancelButton("Cancel") {
                    dismissWithAnimation()
                }

                show()
            }
        }
    }

    private fun setPaymentStatus(transactionId: Int, status: String, paid: Int) {
        if (!isAdded) return

        context?.let { ctx ->
            val loadingDialog = SweetAlertDialog(ctx, SweetAlertDialog.PROGRESS_TYPE)
            loadingDialog.apply {
                titleText = "Processing"
                contentText = "Processing payment..."
                setCancelable(false)
                show()
            }

            viewModel.updatePaymentStatus(transactionId, status, paid)

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
                contentText = "Status pembayaran berhasil diperbarui."

                setConfirmButton("OK") {
                    dismissWithAnimation()
                    requireActivity().onBackPressed()
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