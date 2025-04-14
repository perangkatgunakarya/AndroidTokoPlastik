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
        val MAX_LINES_PER_PAGE = 28
        val PAGE_SPACING = 5 // Number of blank lines between pages

        // Common header elements
        val shopName = "TOKO PLASTIK H. ALI"
        val centeredShopName = shopName.padStart((84 + shopName.length) / 2, ' ').padEnd(84, ' ')

        // Wrap the address if it exceeds 30 characters
        val address = if (orderData.customer.address.length > 30) {
            orderData.customer.address.take(30)
        } else {
            orderData.customer.address
        }

        // Recipient info
        val formattedRecipientInfo = """
        ${"Yth.".padEnd(59)}                                      
        ${orderData.customer.name.padEnd(59)}
        ${address}
    """.trimIndent()

        // Invoice details
        val invoiceDetails = """
        INVOICE
        Referensi  : TPHA-${orderId}
        Tanggal    : ${formatDate(orderData.createdAt)}
        Status     : ${orderData.paymentStatus.toUpperCase()}
        Jatuh Tempo: ${formatDate(orderData.dueDate)}
    """.trimIndent()

        // Combine recipient info and invoice details side by side
        val recipientLines = formattedRecipientInfo.lines()
        val detailLines = invoiceDetails.lines()
        val headerHeight = maxOf(recipientLines.size, detailLines.size)

        val combinedHeader = ArrayList<String>()
        for (i in 0 until headerHeight) {
            val recipientLine = if (i < recipientLines.size) recipientLines[i].padEnd(59) else " ".repeat(59)
            val detailLine = if (i < detailLines.size) detailLines[i] else ""
            combinedHeader.add(recipientLine + detailLine)
        }

        // Table header
        val tableHeader = """
        +----------------------------------------------------------------------------------------+
        | NO  | BANYAKNYA       | NAMA PRODUK              |           HARGA |            JUMLAH |
        |-----|-----------------|--------------------------|-----------------|-------------------|
    """.trimIndent().lines()

        // Table footer for last page
        val tableFooter = """
        |-----|-----------------|--------------------------|-----------------|-------------------|
        | Terbilang :                                      |           Total: ${String.format(Locale.GERMANY, "Rp%,d", orderData.total.toLong()).padStart(17)} |
        | ${getAmountInWords(orderData.total.toLong())}${" ".repeat(48 - getAmountInWords(orderData.total.toLong()).length)}|                                     |
        +----------------------------------------------------------------------------------------+
    """.trimIndent().lines()

        // Simple table footer for intermediate pages
        val intermediateFooter = """
        +----------------------------------------------------------------------------------------+
    """.trimIndent().lines()

        // Calculate fixed content lines
        val fixedHeaderLinesCount = 1 + combinedHeader.size + tableHeader.size // shop name + combined header + table header
        val fixedFooterLinesCount = tableFooter.size

        // Calculate maximum number of content lines per page
        val maxContentLinesPerPage = MAX_LINES_PER_PAGE - fixedHeaderLinesCount - 1 // -1 for safety

        // Process items into pages
        val pages = ArrayList<ArrayList<String>>()
        var currentPage = ArrayList<String>()
        var currentLineCount = 0

        for (i in cartItems.indices) {
            val item = cartItems[i]
            val itemNumber = i + 1

            historyProductPrice = allProductPrices.filter { it.productId == item.productPrice.product.id }
            val description = generateTransactionDesc(item.productPrice.unit, item.quantity, historyProductPrice)

            // Process product name - split into multiple lines if needed (limit to 3 lines)
            val productName = item.productPrice.product.name
            val productNameLines = wrapText(productName, 24)
            val limitedProductNameLines = productNameLines.take(3) // Limit to 3 lines max

            // Calculate how many lines this item will take
            val itemLines = ArrayList<String>()

            // First line with item number, quantity, first line of product name, and prices
            val firstLine = "| ${itemNumber.toString().padEnd(3)} | ${(item.quantity.toString() + " " + item.productPrice.unit).padEnd(15)} | ${limitedProductNameLines[0].padEnd(24)} | ${
                String.format(Locale.GERMANY, "%,d", item.priceAdjustment.toLong()).padStart(15)
            } | ${
                String.format(Locale.GERMANY, "%,d", (item.priceAdjustment * item.quantity).toLong()).padStart(17)
            } |"
            itemLines.add(firstLine)

            // Add description line if needed
            if (description.isNotEmpty()) {
                val descriptionLine = "|     | ${description.padEnd(15)} |                          |                 |                   |"
                itemLines.add(descriptionLine)
            }

            // Additional product name lines if any
            for (j in 1 until limitedProductNameLines.size) {
                val productNameLine = "|     |                 | ${limitedProductNameLines[j].padEnd(24)}|                 |                   |"
                itemLines.add(productNameLine)
            }

            // Empty line after each item for better readability (except last item on page)
            if (i < cartItems.size - 1) {
                itemLines.add("|     |                 |                          |                 |                   |")
            }

            // Check if this item will fit on current page, if not start a new page
            if (currentLineCount + itemLines.size > maxContentLinesPerPage && currentLineCount > 0) {
                pages.add(currentPage)
                currentPage = ArrayList<String>()
                currentLineCount = 0
            }

            // Add item lines to current page
            currentPage.addAll(itemLines)
            currentLineCount += itemLines.size
        }

        // Add last page if not empty
        if (currentPage.isNotEmpty()) {
            pages.add(currentPage)
        }

        // Combine all pages into final result
        val result = StringBuilder()

        for (pageIndex in pages.indices) {
            if (pageIndex > 0) {
                // Add spacing between pages
                result.append("\n".repeat(PAGE_SPACING))
            }

            // Add header
            result.append(centeredShopName).append("\n")
            for (line in combinedHeader) {
                result.append(line).append("\n")
            }
            for (line in tableHeader) {
                result.append(line).append("\n")
            }

            // Add page content
            for (line in pages[pageIndex]) {
                result.append(line).append("\n")
            }

            // Add footer (full footer for last page, simple footer for intermediate pages)
            if (pageIndex == pages.size - 1) {
                // Last page gets full footer with total
                for (line in tableFooter) {
                    result.append(line).append("\n")
                }
            } else {
                // Intermediate pages get simple footer
                for (line in intermediateFooter) {
                    result.append(line).append("\n")
                }
            }
        }

        return result.toString().trimEnd()
    }

    // Helper function to convert number to words in Indonesian
    private fun getAmountInWords(number: Long): String {
        // This is a simplified version - you might want to implement a more complete number-to-words converter
        val units = arrayOf("", "satu", "dua", "tiga", "empat", "lima", "enam", "tujuh", "delapan", "sembilan", "sepuluh",
            "sebelas", "dua belas", "tiga belas", "empat belas", "lima belas", "enam belas", "tujuh belas",
            "delapan belas", "sembilan belas")
        val tens = arrayOf("", "", "dua puluh", "tiga puluh", "empat puluh", "lima puluh", "enam puluh", "tujuh puluh",
            "delapan puluh", "sembilan puluh")

        fun convertLessThanOneThousand(n: Int): String {
            return when {
                n < 20 -> units[n]
                n < 100 -> "${tens[n / 10]} ${units[n % 10]}".trim()
                n < 1000 -> {
                    val hundreds = n / 100
                    val remainder = n % 100
                    val hundredsStr = if (hundreds == 1) "seratus" else "${units[hundreds]} ratus"
                    val remainderStr = if (remainder > 0) " ${convertLessThanOneThousand(remainder)}" else ""
                    "$hundredsStr$remainderStr"
                }
                else -> ""
            }
        }

        if (number == 0L) return "nol"

        val billions = (number / 1_000_000_000).toInt()
        val millions = ((number % 1_000_000_000) / 1_000_000).toInt()
        val thousands = ((number % 1_000_000) / 1_000).toInt()
        val remainder = (number % 1_000).toInt()

        val billionsStr = if (billions > 0) {
            val billionsWord = if (billions == 1) "satu milyar" else "${convertLessThanOneThousand(billions)} milyar"
            billionsWord
        } else ""

        val millionsStr = if (millions > 0) {
            val millionsWord = if (millions == 1) "satu juta" else "${convertLessThanOneThousand(millions)} juta"
            if (billionsStr.isNotEmpty()) " $millionsWord" else millionsWord
        } else ""

        val thousandsStr = if (thousands > 0) {
            val thousandsWord = if (thousands == 1) "seribu" else "${convertLessThanOneThousand(thousands)} ribu"
            if (billionsStr.isNotEmpty() || millionsStr.isNotEmpty()) " $thousandsWord" else thousandsWord
        } else ""

        val remainderStr = if (remainder > 0) {
            val remainderWord = convertLessThanOneThousand(remainder)
            if (billionsStr.isNotEmpty() || millionsStr.isNotEmpty() || thousandsStr.isNotEmpty()) " $remainderWord" else remainderWord
        } else ""

        return "$billionsStr$millionsStr$thousandsStr$remainderStr".trim().capitalize()
    }

    // Helper function to wrap text
    private fun wrapText(text: String, width: Int): List<String> {
        if (text.length <= width) {
            return listOf(text)
        }

        val result = mutableListOf<String>()
        var remaining = text

        while (remaining.isNotEmpty()) {
            val cutIndex = if (remaining.length > width) {
                val spaceIndex = remaining.substring(0, width).lastIndexOf(' ')
                if (spaceIndex > 0) spaceIndex else width
            } else {
                remaining.length
            }

            result.add(remaining.substring(0, cutIndex))
            remaining = if (cutIndex < remaining.length) {
                if (remaining[cutIndex] == ' ') {
                    remaining.substring(cutIndex + 1)
                } else {
                    remaining.substring(cutIndex)
                }
            } else {
                ""
            }
        }

        return result
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