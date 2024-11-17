package com.example.tokoplastik.util

import android.content.Context
import android.content.Intent
import android.print.PrintAttributes
import android.print.pdf.PrintedPdfDocument
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

class ReceiptHandler(private val context: Context) {

    private fun generatePDF(html: String): File {
        val file = File(context.cacheDir, "receipt_${System.currentTimeMillis()}.pdf")

        try {
            val printAttributes = PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setResolution(PrintAttributes.Resolution("pdf", "pdf", 600, 600))
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build()

            val pdfDocument = PrintedPdfDocument(context, printAttributes)
            val webView = WebView(context)
            webView.loadDataWithBaseURL(null, html, "text/HTML", "UTF-8", null)

            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    val page = pdfDocument.startPage(0)
                    webView.draw(page.canvas)
                    pdfDocument.finishPage(page)

                    FileOutputStream(file).use { out ->
                        pdfDocument.writeTo(out)
                    }

                    pdfDocument.close()
                }
            }

            return file
        } catch (e: Exception) {
            throw Exception("Failed to generate PDF: ${e.message}")
        }
    }

    fun printReceipt(html: String) {
        val webView = WebView(context)
        webView.loadDataWithBaseURL(null, html, "text/HTML", "UTF-8", null)

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as android.print.PrintManager
                val jobName = "Receipt_${System.currentTimeMillis()}"

                val printAdapter = webView.createPrintDocumentAdapter(jobName)
                printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
            }
        }
    }

    fun shareReceipt(html: String) {
        try {
            val pdfFile = generatePDF(html)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share Receipt")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to share receipt: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}