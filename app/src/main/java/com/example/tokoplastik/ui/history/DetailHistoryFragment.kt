package com.example.tokoplastik.ui.history

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import cn.pedant.SweetAlert.SweetAlertDialog
import com.example.tokoplastik.R
import com.example.tokoplastik.adapter.DetailHistoryAdapter
import com.example.tokoplastik.data.network.CheckoutApi
import com.example.tokoplastik.data.repository.CheckoutRepository
import com.example.tokoplastik.data.responses.TransactionDetail
import com.example.tokoplastik.databinding.FragmentDetailHistoryBinding
import com.example.tokoplastik.ui.base.BaseFragment
import com.example.tokoplastik.ui.transaction.DueDateBottomSheet
import com.example.tokoplastik.ui.transaction.PaidBottomSheet
import com.example.tokoplastik.util.HistoryReceiptGenerator
import com.example.tokoplastik.util.Resource
import com.example.tokoplastik.util.handleApiError
import com.example.tokoplastik.viewmodel.CheckoutViewModel
import com.rajat.pdfviewer.PdfViewerActivity
import com.rajat.pdfviewer.util.saveTo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File
import java.text.SimpleDateFormat
import java.util.TimeZone

class DetailHistoryFragment :
    BaseFragment<CheckoutViewModel, FragmentDetailHistoryBinding, CheckoutRepository>() {

    private val args: DetailHistoryFragmentArgs by navArgs()
    private var transactionId: Int = -1
    private lateinit var detailTransactions: TransactionDetail
    private lateinit var invoiceGenerator: HistoryReceiptGenerator
    private lateinit var statusPayment: String
    private var totalPaid: Int = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        transactionId = args.transactionId
        invoiceGenerator = HistoryReceiptGenerator(requireContext())

        setupViews()
        setupObservers()
        binding.menuIcon.setOnClickListener {
            showPopupMenu(it)
        }

        binding.buttonPreview.setOnClickListener {
            if (::detailTransactions.isInitialized) {
                val pdfFile = invoiceGenerator.generatedPdfReceipt(
                    detailTransactions,
                    detailTransactions.transactionProduct,
                    detailTransactions.id.toString()
                )

                val pdfPath = "/storage/emulated/0/Android/data/${requireContext().packageName}/files/Documents/Invoice_${detailTransactions.id}.pdf"

                val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'")
                parser.timeZone = TimeZone.getTimeZone("UTC")
                val date = parser.parse(detailTransactions.createdAt)
                val transactionDate = SimpleDateFormat("dd MMM Y HH:mm").format(date)

                launchPdfFromUri(uri = pdfPath, title = "${detailTransactions.customer.name}\n${transactionDate}")
            }
        }
    }

    private fun launchPdfFromUri(uri: String, title:String) {
        startActivity(
            PdfViewerActivity.launchPdfFromPath(
                context = requireContext(), path = uri,
                pdfTitle = title, saveTo = saveTo.ASK_EVERYTIME,  fromAssets = false)
        )
    }

    private fun showPopupMenu(view: View) {
        val popupMenu = androidx.appcompat.widget.PopupMenu(requireContext(), view)
        popupMenu.inflate(R.menu.detail_history_fragment_menu)

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.button_print -> {
                    if (::detailTransactions.isInitialized) {
                        val invoiceText = invoiceGenerator.generateInvoiceText(
                            detailTransactions,
                            detailTransactions.transactionProduct,
                            detailTransactions.id.toString()
                        )
                        val file = invoiceGenerator.saveInvoiceToFile(
                            requireContext(),
                            invoiceText,
                            "Invoice_${detailTransactions.id}.txt"
                        )
                        invoiceGenerator.shareReceiptTxt(file)
                    }
                    true
                }

                R.id.button_share -> {
                    if (::detailTransactions.isInitialized) {
                        val pdfFile = invoiceGenerator.generatedPdfReceipt(
                            detailTransactions,
                            detailTransactions.transactionProduct,
                            detailTransactions.id.toString()
                        )
                        invoiceGenerator.shareReceipt(pdfFile)
                    }
                    true
                }

                R.id.button_lunas -> {
                    if (::detailTransactions.isInitialized) {
                        if (detailTransactions.paymentStatus != "lunas") {
                            val bottomSheet = CreditBottomSheet()
                            bottomSheet.show(childFragmentManager, "CREDIT_BOTTOM_SHEET")
                        }
                    }
                    true
                }

                R.id.button_due_date -> {
                    if (::detailTransactions.isInitialized) {
                        if (detailTransactions.paymentStatus != "lunas") {
                            val bottomSheet = DueDateBottomSheet()
                            bottomSheet.show(childFragmentManager, "DUE_DATE_BOTTOM_SHEET")

                            viewModel.dueDate.observe(viewLifecycleOwner) { date ->
                                if (date != viewModel.transactionDetail.value?.data?.data?.dueDate) {
                                    setPaymentStatus(
                                        transactionId,
                                        detailTransactions.paymentStatus,
                                        totalPaid,
                                        date
                                    )
                                }
                            }
                        }
                    }
                    true
                }

                else -> false
            }
        }
        popupMenu.show()
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
                        detailTransactions = response.data

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

                    }
                }

                is Resource.Failure -> {
                    handleApiError(result)
                }

                Resource.Loading -> {}
            }
        }

        viewModel.getAllProductPrices()
        viewModel.productPrices.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Resource.Success -> {
                    result.data?.let { response ->
                        invoiceGenerator.allProductPrices = response.data
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
                setPaymentStatus(
                    transactionId,
                    statusPayment,
                    totalPaid,
                    viewModel.transactionDetail.value?.data?.data?.dueDate
                )
            }
            if (amount > 0 && amount < detailTransactions.total) {
                statusPayment = "dalam cicilan"
                totalPaid = amount
                setPaymentStatus(
                    transactionId,
                    statusPayment,
                    totalPaid,
                    viewModel.transactionDetail.value?.data?.data?.dueDate
                )
            } else {
                statusPayment = "lunas"
                totalPaid = amount
                setPaymentStatus(
                    transactionId,
                    statusPayment,
                    totalPaid,
                    viewModel.transactionDetail.value?.data?.data?.dueDate
                )
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
                    setPaymentStatus(transactionId, "lunas", 0, null)
                }

                setCancelButton("Cancel") {
                    dismissWithAnimation()
                }

                show()
            }
        }
    }

    private fun setPaymentStatus(transactionId: Int, status: String, paid: Int, dueDate: String?) {
        if (!isAdded) return

        context?.let { ctx ->
            val loadingDialog = SweetAlertDialog(ctx, SweetAlertDialog.PROGRESS_TYPE)
            loadingDialog.apply {
                titleText = "Processing"
                contentText = "Processing payment..."
                setCancelable(false)
                show()
            }

            viewModel.updatePaymentStatus(transactionId, status, paid, dueDate)

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
                contentText = "Berhasil diperbarui."

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