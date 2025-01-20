package com.example.tokoplastik.util

import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.example.tokoplastik.data.responses.ProductPrice
import com.example.tokoplastik.data.responses.TransactionDetail
import com.example.tokoplastik.data.responses.TransactionDetailProduct
import com.itextpdf.text.BaseColor
import com.itextpdf.text.Document
import com.itextpdf.text.Element
import com.itextpdf.text.FontFactory
import com.itextpdf.text.PageSize
import com.itextpdf.text.Paragraph
import com.itextpdf.text.Phrase
import com.itextpdf.text.Rectangle
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryReceiptGenerator (
    private val context: Context
) {
    public var allProductPrices: List<ProductPrice> = emptyList()
    private lateinit var historyProductPrice: List<ProductPrice>

    fun getProductPricesByProductId(productId: Int, productPrices: List<ProductPrice>): List<ProductPrice> {
        return productPrices.filter { it.productId == productId }
    }

    fun generateTransactionDesc(unit: String, quantity: Int, productPrice: List<ProductPrice>): String {
        val sortedProductPrice = productPrice.sortedByDescending { it.quantityPerUnit }
        val selectedUnitIndex = sortedProductPrice.indexOfFirst { it.unit == unit }
        if (selectedUnitIndex == -1) {
            return "Unit tidak ditemukan"
        }

        val isLowestUnit = selectedUnitIndex == sortedProductPrice.size - 1
        if (isLowestUnit) {
            return "$quantity"
        }

        val result = StringBuilder("$quantity")

        for (i in selectedUnitIndex until sortedProductPrice.size - 1) {
            val currentUnit = sortedProductPrice[i]
            val nextLowerUnit = sortedProductPrice[i + 1]
            val ratio = currentUnit.quantityPerUnit.toInt() / nextLowerUnit.quantityPerUnit.toInt()

            result.append("x$ratio")
        }

        return result.toString()
    }

    fun generatedPdfReceipt(
        orderData: TransactionDetail,
        cartItems: List<TransactionDetailProduct>,
        orderId: String
    ): File {
        val filename = "Invoice_${orderId}.pdf"
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), filename)

        val document = Document(PageSize.A4)
        try {
            PdfWriter.getInstance(document, FileOutputStream(file))
            document.open()

            val blueHeader = BaseColor(51, 122, 183)
            val grayText = BaseColor(108, 117, 125)
            val alternateRow = BaseColor(249, 249, 249)

            val companyName = Paragraph(
                "Toko Plastik H. Ali",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18f)
            )
            companyName.alignment = Element.ALIGN_LEFT
            document.add(companyName)

            val invoice = Paragraph(
                "INVOICE",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18f, blueHeader)
            )
            invoice.alignment = Element.ALIGN_RIGHT
            document.add(invoice)

            val headerTable = PdfPTable(2)
            headerTable.widthPercentage = 100f
            headerTable.setWidths(floatArrayOf(5f, 5f))

            val customerInfoCell = PdfPCell().apply {
                border = Rectangle.NO_BORDER
                val customerFont = FontFactory.getFont(FontFactory.HELVETICA, 13f, BaseColor.BLACK)
                val addressFont = FontFactory.getFont(FontFactory.HELVETICA, 12f, grayText)
                addElement(Paragraph(orderData.customer.name, customerFont))
                addElement(Paragraph(orderData.customer.address, addressFont))
            }

            val invoiceDetailsCell = PdfPCell().apply {
                border = Rectangle.NO_BORDER
                horizontalAlignment = Element.ALIGN_RIGHT
                val detailsFont = FontFactory.getFont(FontFactory.HELVETICA, 11f)
                addElement(Paragraph("Referensi    : TPHA-${orderId}", detailsFont))
                addElement(
                    Paragraph("Tanggal      : ${
                        SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(
                            Date()
                        )}", detailsFont)
                )
                addElement(Paragraph("Status        : ${orderData.paymentStatus}", detailsFont))
            }

            headerTable.addCell(customerInfoCell)
            headerTable.addCell(invoiceDetailsCell)

            document.add(Paragraph("\n"))
            document.add(headerTable)
            document.add(Paragraph("\n"))

            document.add(Paragraph("\n"))

            val table = PdfPTable(5)
            table.widthPercentage = 100f
            table.setWidths(floatArrayOf(0.5f, 1.5f, 2.5f, 1.5f, 1.5f))

            val headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11f, BaseColor.WHITE)
            arrayOf("NO", "BANYAKNYA", "NAMA ITEM", "HARGA",  "JUMLAH").forEach { header ->
                val cell = PdfPCell(Phrase(header, headerFont))
                cell.backgroundColor = blueHeader
                cell.setPadding(8f)
                cell.horizontalAlignment = Element.ALIGN_CENTER
                cell.verticalAlignment = Element.ALIGN_MIDDLE
                table.addCell(cell)
            }

            val contentFont = FontFactory.getFont(FontFactory.HELVETICA, 10f)
            var isAlternateRow = false

            var index: Int = 0
            cartItems.forEach { item ->
                val cells = arrayOf(
                    "${++index}",
                    "${item.quantity} ${item.productPrice.unit}",
                    item.productPrice.product.name,
                    String.format(Locale.GERMANY, "Rp %,d", item.priceAdjustment),
                    String.format(Locale.GERMANY, "Rp %,d", item.priceAdjustment * item.quantity)
                )

                cells.forEach { content ->
                    val cell = PdfPCell(Phrase(content, contentFont))
                    if (isAlternateRow) {
                        cell.backgroundColor = alternateRow
                    }
                    cell.setPadding(8f)
                    cell.horizontalAlignment = Element.ALIGN_CENTER
                    table.addCell(cell)
                }
                isAlternateRow = !isAlternateRow
            }

            document.add(table)
            document.add(Paragraph("\n"))

            val totalPara = Paragraph(
                "Total: ${String.format(Locale.GERMANY, "Rp %,d", orderData.total.toLong())}",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12f, blueHeader)
            )
            totalPara.alignment = Element.ALIGN_RIGHT
            document.add(totalPara)

            document.add(Paragraph("\n\n"))

            val footer = Paragraph(
                "Dengan Hormat,\n\n\n\nToko Plastik H. Ali\n",
                FontFactory.getFont(FontFactory.HELVETICA, 11f)
            )
            footer.alignment = Element.ALIGN_CENTER
            document.add(footer)

        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        } finally {
            document.close()
        }

        return file
    }

    fun shareReceipt(file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Share Invoice"))
    }

    fun generateInvoiceText(
        orderData: TransactionDetail,
        cartItems: List<TransactionDetailProduct>,
        orderId: String
    ): String {
        // Centered Shop Name
        val shopName = "TOKO PLASTIK H. ALI"
        val centeredShopName = shopName.padStart((84 + shopName.length) / 2, ' ').padEnd(84, ' ')

        // Wrap the address if it exceeds 30 characters
        val address = if (orderData.customer.address.length > 30) { orderData.customer.address.take(30) } else { orderData.customer.address }
        val formattedRecipientInfo = """
            ${"Yth.".padEnd(59)}
            ${orderData.customer.name.padEnd(59)}
            ${address}
             
        """.trimIndent()

        // Invoice Details (Right-Aligned)
        val invoiceDetails = """
        INVOICE
        Referensi : TPHA-${orderId}
        ${"Tanggal   : ".padStart(31) + try {
            SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
                .format(SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault()).parse(orderData.createdAt))
        } catch (e: Exception) {
            "Invalid Date"
        }}
        ${"Status    : ".padStart(31) + orderData.paymentStatus.toUpperCase()}
    """.trimIndent()

        // Combine Recipient Info and Invoice Details
        val recipientLines = formattedRecipientInfo.lines()
        val detailLines = invoiceDetails.lines()
        val combinedInfo = recipientLines.zip(detailLines).joinToString("\n") { (recipient, detail) ->
            recipient.padEnd(40) + detail
        }

        // Table Header
        val tableHeader = """
        +----------------------------------------------------------------------------------------+
        | NO  | BANYAKNYA       | NAMA PRODUK              | HARGA           | JUMLAH            |
        |-----|-----------------|--------------------------|-----------------|-------------------|
    """.trimIndent()

        // Items
        val itemsText = cartItems.flatMapIndexed { index, item ->
            historyProductPrice = allProductPrices.filter { it.productId == item.productPrice.product.id }
            val description = generateTransactionDesc(item.productPrice.unit, item.quantity, historyProductPrice)

            val wrappedItemName = wrapText(item.productPrice.product.name, 24) // Wrap item name to 24 characters
            val firstLine = "| ${(index + 1).toString().padEnd(3)} | ${(item.quantity.toString() + " " + item.productPrice.unit).padEnd(15)} | ${wrappedItemName[0].padEnd(24)} | ${item.priceAdjustment.toString().padEnd(15)} | ${(item.priceAdjustment * item.quantity).toString().padEnd(17)} |"
            val descriptionLine = "|     | ${description.padEnd(15)} |                          |                 |                   |"
            val additionalLines = wrappedItemName.drop(1).map { "|     |                 | ${it.padEnd(24)} |                 |                   |" }
            listOf(firstLine, descriptionLine) + additionalLines
        }.joinToString("\n")

        // Footer
        val footer = """
        |-----|-----------------|--------------------------|-----------------|-------------------|
        | 		                                                Total: ${String.format(Locale.GERMANY, "Rp %,d", orderData.total.toLong()).padEnd(18)}|
        +----------------------------------------------------------------------------------------+

    """.trimIndent()

        val regard = "Dengan Hormat"
        val centeredRegard = regard.padStart((84 + regard.length) / 2, ' ').padEnd(84, ' ')

        val footer2 = """
            
            
            """.trimIndent()
        val footer3 = """
            
            
            
            
            
            
            
            
            
            
        """.trimIndent()
        return "$centeredShopName\n\n$combinedInfo\n\n$tableHeader\n$itemsText\n$footer\n$centeredRegard\n$footer2\n$centeredShopName\n$footer3"
    }

    // Helper function to wrap text
    fun wrapText(text: String, width: Int): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""

        for (word in words) {
            if (currentLine.length + word.length + 1 <= width) {
                currentLine += "$word "
            } else {
                lines.add(currentLine.trim())
                currentLine = "$word "
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.trim())
        }
        return lines
    }

    fun saveInvoiceToFile(context: Context, invoiceText: String, fileName: String): File {
        // Get the app's external files directory
        val directory = context.getExternalFilesDir(null)
        val file = File(directory, fileName)

        // Write the invoice text to the file
        file.writeText(invoiceText)

        return file
    }

    fun shareReceiptTxt(file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Print Invoice"))
    }
}