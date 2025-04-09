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

class HistoryReceiptGenerator(
    private val context: Context
) {
    public var allProductPrices: List<ProductPrice> = emptyList()
    private lateinit var historyProductPrice: List<ProductPrice>

    private fun formatDate(dateString: String): String {
        val outputFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

        // List format input yang didukung
        val inputFormats = listOf(
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault()),
            SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")),
            SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()),
        )

        // Coba parse dengan semua format yang tersedia
        for (format in inputFormats) {
            try {
                val date: Date = format.parse(dateString) ?: continue
                return outputFormat.format(date)
            } catch (e: Exception) {
                // Lanjut ke format berikutnya jika format ini gagal
                continue
            }
        }

        // Jika semua format gagal
        return "-"
    }

    private fun generateTransactionDesc(
        unit: String,
        quantity: Int,
        productPrice: List<ProductPrice>
    ): String {

        val sortedProductPrice = productPrice.sortedByDescending { it.quantityPerUnit }
        val selectedUnitIndex = sortedProductPrice.indexOfFirst { it.unit == unit }
        val selectedUnit = sortedProductPrice.firstOrNull() { it.unit == unit }
        if (selectedUnit == null) {
            return "Unit tidak ditemukan"
        }

        val isLowestUnit = selectedUnitIndex == sortedProductPrice.size - 1
        if (isLowestUnit) {
            return ""
        }

        val lowestProductPrice = sortedProductPrice[sortedProductPrice.size - 1]
        val result: String = if (unit === lowestProductPrice.unit) {
            ""
        } else {
            "($quantity x ${selectedUnit.quantityPerUnit} ${lowestProductPrice.unit})"
        }

        return result


//        val sortedProductPrice = productPrice.sortedByDescending { it.quantityPerUnit }
//        val selectedUnitIndex = sortedProductPrice.indexOfFirst { it.unit == unit }
//        if (selectedUnitIndex == -1) {
//            return "Unit tidak ditemukan"
//        }
//
//        val isLowestUnit = selectedUnitIndex == sortedProductPrice.size - 1
//        if (isLowestUnit) {
//            return "$quantity"
//        }
//
//        val result = StringBuilder("$quantity")
//
//        for (i in selectedUnitIndex until sortedProductPrice.size - 1) {
//            val currentUnit = sortedProductPrice[i]
//            val nextLowerUnit = sortedProductPrice[i + 1]
//            val ratio = currentUnit.quantityPerUnit.toInt() / nextLowerUnit.quantityPerUnit.toInt()
//
//            result.append(" x $ratio ${currentUnit.product.lowestUnit}")
//        }
//
//        return result.toString()
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

            val address = if (orderData.customer.address.length > 30) {
                orderData.customer.address.take(30)
            } else {
                orderData.customer.address
            }
            val customerInfoCell = PdfPCell().apply {
                border = Rectangle.NO_BORDER
                val customerFont = FontFactory.getFont(FontFactory.HELVETICA, 13f, BaseColor.BLACK)
                val addressFont = FontFactory.getFont(FontFactory.HELVETICA, 12f, grayText)
                addElement(Paragraph(orderData.customer.name, customerFont))
                addElement(Paragraph(address, addressFont))
            }

            val invoiceDetailsCell = PdfPCell().apply {
                border = Rectangle.NO_BORDER
                horizontalAlignment = Element.ALIGN_RIGHT
                val detailsFont = FontFactory.getFont(FontFactory.HELVETICA, 11f)

                // Create a PdfPTable for aligned details
                val detailsTable = PdfPTable(2)
                detailsTable.setWidths(floatArrayOf(4f, 5f))
                detailsTable.widthPercentage = 75f
                detailsTable.horizontalAlignment = Element.ALIGN_RIGHT

                // Helper function to create aligned detail rows
                fun createDetailRow(label: String, value: String): List<PdfPCell> {
                    val labelCell = PdfPCell(Phrase(label, detailsFont))
                    labelCell.border = Rectangle.NO_BORDER
                    labelCell.horizontalAlignment = Element.ALIGN_LEFT

                    val valueCell = PdfPCell(Phrase(value, detailsFont))
                    valueCell.border = Rectangle.NO_BORDER
                    valueCell.horizontalAlignment = Element.ALIGN_LEFT

                    return listOf(labelCell, valueCell)
                }

                // Add aligned detail rows
                createDetailRow(
                    "Referensi",
                    ": TPHA-${orderId}"
                ).forEach { detailsTable.addCell(it) }
                createDetailRow(
                    "Tanggal",
                    ": " + formatDate(orderData.createdAt)
                ).forEach { detailsTable.addCell(it) }
                createDetailRow(
                    "Status",
                    ": " + orderData.paymentStatus.toUpperCase()
                ).forEach { detailsTable.addCell(it) }
                createDetailRow(
                    "Jatuh Tempo",
                    ": " + formatDate(orderData.dueDate)
                ).forEach { detailsTable.addCell(it) }

                addElement(detailsTable)
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
            arrayOf("NO", "BANYAKNYA", "NAMA ITEM", "HARGA", "JUMLAH").forEach { header ->
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
                Log.d(
                    "DetailHistoryFragment",
                    "Item: ${allProductPrices} - ${item.productPrice.product.id}"
                )
                historyProductPrice =
                    allProductPrices.filter { it.productId == item.productPrice.product.id }
                val description = generateTransactionDesc(
                    item.productPrice.unit,
                    item.quantity,
                    historyProductPrice
                )

                val cells = arrayOf(
                    "${++index}",  // Kolom indeks
                    "${item.quantity} ${item.productPrice.unit} \n${description}",  // Kolom deskripsi
                    item.productPrice.product.name,  // Kolom nama produk
                    String.format(
                        Locale.GERMANY,
                        "%,d",
                        item.priceAdjustment
                    ),  // Kolom harga per unit
                    String.format(
                        Locale.GERMANY,
                        "%,d",
                        item.priceAdjustment * item.quantity
                    )  // Kolom total harga
                )

                cells.forEachIndexed { cellIndex, content ->
                    val cell = PdfPCell(Phrase(content, contentFont))
                    if (isAlternateRow) {
                        cell.backgroundColor = alternateRow
                    }
                    cell.setPadding(8f)

                    // Atur alignment berdasarkan indeks kolom
                    when (cellIndex) {
                        0 -> cell.horizontalAlignment = Element.ALIGN_CENTER
                        1 -> cell.horizontalAlignment = Element.ALIGN_CENTER
                        3 -> cell.horizontalAlignment = Element.ALIGN_RIGHT
                        4 -> cell.horizontalAlignment = Element.ALIGN_RIGHT
                        else -> cell.horizontalAlignment = Element.ALIGN_LEFT
                    }

                    table.addCell(cell)
                }
                isAlternateRow = !isAlternateRow
            }

            document.add(table)

            val attentionText = Paragraph(
                "\nPerhatian : Barang yang sudah dibeli tidak dapat dikembalikan lagi.",
                FontFactory.getFont(FontFactory.HELVETICA, 10f, BaseColor.RED)
            )
            attentionText.alignment = Element.ALIGN_LEFT
            document.add(attentionText)

            val totalPara = Paragraph(
                "Total: ${String.format(Locale.GERMANY, "Rp%,d", orderData.total.toLong())}",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12f, blueHeader)
            )
            totalPara.alignment = Element.ALIGN_RIGHT
            document.add(totalPara)

            document.add(Paragraph("\n\n"))

            val footerTable = PdfPTable(3)
            footerTable.widthPercentage = 100f
            footerTable.setWidths(floatArrayOf(3f, 3f, 2f))

            val penerimaCell = PdfPCell().apply {
                border = Rectangle.NO_BORDER
                val penerimaFont = FontFactory.getFont(FontFactory.HELVETICA, 11f)
                addElement(Paragraph("Penerima\n\n\n\n", penerimaFont))
                addElement(Paragraph("""(                                  )""".trimIndent(), penerimaFont))
            }

            val pengirimCell = PdfPCell().apply {
                border = Rectangle.NO_BORDER
                val pengirimFont = FontFactory.getFont(FontFactory.HELVETICA, 11f)
                addElement(Paragraph("Pengirim\n\n\n\n", pengirimFont))
                addElement(Paragraph("""(                                  )""".trimIndent(), pengirimFont))
            }

            val regardCell = PdfPCell().apply {
                border = Rectangle.NO_BORDER
                val regardFont = FontFactory.getFont(FontFactory.HELVETICA, 11f)
                addElement(Paragraph("Dengan Hormat\n\n\n\n", regardFont))
                addElement(Paragraph("""(                                  )""".trimIndent(), regardFont))
            }

            footerTable.addCell(penerimaCell)
            footerTable.addCell(pengirimCell)
            footerTable.addCell(regardCell)

            document.add(footerTable)

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
        val address = if (orderData.customer.address.length > 30) {
            orderData.customer.address.take(30)
        } else {
            orderData.customer.address
        }

        // Reduced whitespace in recipient info
        val formattedRecipientInfo = """
        ${"Yth.".padEnd(59)}
        ${orderData.customer.name.padEnd(59)}
        ${address}
    """.trimIndent()

        // Invoice Details (Right-Aligned)
        val invoiceDetails = """
        INVOICE
        Referensi : TPHA-${orderId}
        ${"Tanggal   : ".padStart(31) + formatDate(orderData.createdAt)}
        ${"Status    : ".padStart(31) + orderData.paymentStatus.toUpperCase()}
        ${"Jatuh Tempo : ".padStart(31) + formatDate(orderData.dueDate)}
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
        | NO  | BANYAKNYA       | NAMA PRODUK              |           HARGA |            JUMLAH |
        |-----|-----------------|--------------------------|-----------------|-------------------|
    """.trimIndent()

        // Create paginated result with maximum 5 items per page
        val result = StringBuilder()
        val itemsPerPage = 5
        val pages = (cartItems.size + itemsPerPage - 1) / itemsPerPage // Calculate number of pages

        for (page in 0 until pages) {
            if (page > 0) {
                result.append("\n\n\n\n") // Space between pages
            }

            // Add header content to each page
            result.append(centeredShopName)
            result.append("\n")
            result.append(combinedInfo)
            result.append("\n")
            result.append(tableHeader)
            result.append("\n")

            // Process items for current page
            val startIdx = page * itemsPerPage
            val endIdx = minOf(startIdx + itemsPerPage, cartItems.size)

            for (i in startIdx until endIdx) {
                val item = cartItems[i]
                val itemNumber = i + 1

                historyProductPrice = allProductPrices.filter { it.productId == item.productPrice.product.id }
                val description = generateTransactionDesc(item.productPrice.unit, item.quantity, historyProductPrice)

                // Trim item name to 24 characters max (no wrapping)
                val trimmedItemName = item.productPrice.product.name.take(24).padEnd(24)

                // Add the item line
                val itemLine = "| ${itemNumber.toString().padEnd(3)} | ${(item.quantity.toString() + " " + item.productPrice.unit).padEnd(15)} | $trimmedItemName | ${
                    String.format(Locale.GERMANY, "%,d", item.priceAdjustment.toLong()).padStart(15)
                } | ${
                    String.format(Locale.GERMANY, "%,d", (item.priceAdjustment * item.quantity).toLong()).padStart(17)
                } |"
                result.append(itemLine)
                result.append("\n")

                // Add description line if needed
                if (description.isNotEmpty()) {
                    val descriptionLine = "|     | ${description.padEnd(15)} |                          |                 |                   |"
                    result.append(descriptionLine)
                    result.append("\n")
                }
            }

            // Add footer to each page
            val footer = """
            |-----|-----------------|--------------------------|-----------------|-------------------|
            |                                                   Total: ${String.format(Locale.GERMANY, "Rp%,d", orderData.total.toLong()).padStart(17)} |
            +----------------------------------------------------------------------------------------+
        """.trimIndent()
            result.append(footer)
            result.append("\n")
            result.append("Perhatian : Barang yang sudah dibeli tidak dapat dikembalikan lagi.")

            // Only add signature section to the last page
            if (page == pages - 1) {
                result.append("\n")
                result.append("""
                Penerima                 Pengirim                 Dengan Hormat
                (            )           (            )           (            )
            """.trimIndent())
            }
        }

        return result.toString()
    }

    private fun paginateReceiptText(text: String, rowsPerPage: Int, spaceBetweenPages: Int): String {
        val lines = text.lines()
        val totalLines = lines.size

        if (totalLines <= rowsPerPage) {
            return text
        }

        val paginatedText = StringBuilder()
        var currentLine = 0

        while (currentLine < totalLines) {
            val endLine = minOf(currentLine + rowsPerPage, totalLines)
            for (i in currentLine until endLine) {
                paginatedText.append(lines[i])
                paginatedText.append("\n")
            }

            currentLine = endLine

            if (currentLine < totalLines) {
                repeat(spaceBetweenPages) {
                    paginatedText.append("\n")
                }
            }
        }

        return paginatedText.toString()
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