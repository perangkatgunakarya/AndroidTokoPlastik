package com.example.tokoplastik.util

import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbManager
import android.os.Environment
import android.util.Log
import androidx.compose.ui.text.toUpperCase
import androidx.core.content.FileProvider
import com.example.tokoplastik.data.responses.CartItem
import com.example.tokoplastik.data.responses.ProductPrice
import com.example.tokoplastik.data.responses.Transaction
import com.itextpdf.text.*
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

class ReceiptGenerator(
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
            return "(1 ${selectedUnit.unit})"
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
//            result.append("x$ratio")
//        }
//
//        return result.toString()
    }

    fun generatedPdfReceipt(
        orderData: Transaction,
        cartItems: List<CartItem>,
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
                    "HistoryReceiptGenerator",
                    "Processing item: ${item.selectedPrice.productId} - ${allProductPrices}"
                )
                historyProductPrice =
                    allProductPrices.filter { it.productId == item.selectedPrice.productId }
                val description = generateTransactionDesc(
                    item.selectedPrice.unit,
                    item.quantity,
                    historyProductPrice
                )

                val cells = arrayOf(
                    "${++index}",
                    "${item.quantity} ${item.selectedPrice.unit} \n ${description}",
                    item.product?.data?.product?.name,
                    String.format(Locale.GERMANY, "%,d", item.customPrice),
                    String.format(Locale.GERMANY, "%,d", item.customPrice * item.quantity)
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
                "Total: ${String.format(Locale.GERMANY, "Rp %,d", orderData.total.toLong())}",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12f, blueHeader)
            )
            totalPara.alignment = Element.ALIGN_RIGHT
            document.add(totalPara)

            val terbilangTitle = Paragraph(
                "Terbilang :",
                FontFactory.getFont(FontFactory.HELVETICA, 10f)
            )
            document.add(terbilangTitle)

            // Buat table dengan 1 kolom saja untuk membatasi lebar
            val terbilangTable = PdfPTable(1)
            terbilangTable.widthPercentage = 50f // Batasi lebar hanya 50% dari halaman
            terbilangTable.horizontalAlignment = Element.ALIGN_LEFT

            // Tambahkan teks ke dalam cell
            val terbilangCell = PdfPCell(
                Phrase(
                    getAmountInWords(orderData.total.toLong()),
                    FontFactory.getFont(FontFactory.HELVETICA, 10f, Font.ITALIC)
                )
            ).apply {
                border = Rectangle.NO_BORDER
                setPadding(4f)
            }

            // Tambahkan cell ke tabel, lalu tambahkan tabel ke dokumen
            terbilangTable.addCell(terbilangCell)
            document.add(terbilangTable)

//            document.add(Paragraph("\n\n"))

            val footerTable = PdfPTable(3)
            footerTable.widthPercentage = 100f
            footerTable.setWidths(floatArrayOf(3f, 3f, 2f))

            val penerimaCell = PdfPCell().apply {
                border = Rectangle.NO_BORDER
                val penerimaFont = FontFactory.getFont(FontFactory.HELVETICA, 11f)
                addElement(Paragraph("Penerima\n\n\n\n", penerimaFont))
                addElement(
                    Paragraph(
                        """(                                  )""".trimIndent(),
                        penerimaFont
                    )
                )
            }

            val pengirimCell = PdfPCell().apply {
                border = Rectangle.NO_BORDER
                val pengirimFont = FontFactory.getFont(FontFactory.HELVETICA, 11f)
                addElement(Paragraph("Pengirim\n\n\n\n", pengirimFont))
                addElement(
                    Paragraph(
                        """(                                  )""".trimIndent(),
                        pengirimFont
                    )
                )
            }

            val regardCell = PdfPCell().apply {
                border = Rectangle.NO_BORDER
                val regardFont = FontFactory.getFont(FontFactory.HELVETICA, 11f)
                addElement(Paragraph("Dengan Hormat\n\n\n\n", regardFont))
                addElement(
                    Paragraph(
                        """(                                  )""".trimIndent(),
                        regardFont
                    )
                )
            }

//            footerTable.addCell(penerimaCell)
//            footerTable.addCell(pengirimCell)
//            footerTable.addCell(regardCell)
//
//            document.add(footerTable)

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
        orderData: Transaction,
        cartItems: List<CartItem>,
        orderId: String
    ): String {
        val MAX_LINES_PER_PAGE = 28
        val PAGE_SPACING = 6 // Number of blank lines between pages

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
        ${"Yth."}                                      
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
        // Table footer for last page
        val tableFooter = buildString {
            appendLine("|-----|-----------------|--------------------------|-----------------|-------------------|")
            appendLine("| Terbilang :                                      |            Total: ${String.format(Locale.GERMANY, "Rp%,d", orderData.total.toLong()).padStart(17)} |")

            // Wrap teks terbilang jika melebihi 48 karakter
            val terbilangText = getAmountInWords(orderData.total.toLong())
            val wrappedTerbilang = wrapTerbilang(terbilangText, 48)

            // Tambahkan baris pertama terbilang
            appendLine("| ${wrappedTerbilang[0].padEnd(48)} |                                     |")

            // Jika ada baris tambahan untuk terbilang, tambahkan sebagai baris baru
            for (i in 1 until wrappedTerbilang.size) {
                appendLine("| ${wrappedTerbilang[i].padEnd(48)} |                                     |")
            }

            appendLine("+----------------------------------------------------------------------------------------+")
        }.trimIndent().lines()

        // Simple table footer for intermediate pages
        val intermediateFooter = """
        +----------------------------------------------------------------------------------------+
    """.trimIndent().lines()

        // Calculate fixed content lines
        val fixedHeaderLinesCount = 1 + combinedHeader.size + tableHeader.size
        val fixedFooterLinesCount = tableFooter.size

        // Calculate maximum number of content lines per page
        val maxContentLinesPerPage = MAX_LINES_PER_PAGE - fixedHeaderLinesCount - 1

        // Process items into pages
        val pages = ArrayList<ArrayList<String>>()
        var currentPage = ArrayList<String>()
        var currentLineCount = 0

        for (i in cartItems.indices) {
            val item = cartItems[i]
            val itemNumber = i + 1

            historyProductPrice = allProductPrices.filter { it.productId == item.selectedPrice.productId }
            val description = generateTransactionDesc(item.selectedPrice.unit, item.quantity, historyProductPrice)

            // Process product name - split into multiple lines if needed
            val productName = item.product?.data?.product?.name ?: ""
            val productNameLines = wrapText(productName, 24)
            val limitedProductNameLines = productNameLines.take(3)

            // Calculate how many lines this item will take
            val itemLines = ArrayList<String>()

            // First line with item number, quantity, first line of product name, and prices
            val firstLine = "| ${itemNumber.toString().padEnd(3)} | ${(item.quantity.toString() + " " + item.selectedPrice.unit).padEnd(15)} | ${limitedProductNameLines.getOrElse(0) { "" }.padEnd(24)} | ${
                String.format(Locale.GERMANY, "%,d", item.customPrice).padStart(15)
            } | ${
                String.format(Locale.GERMANY, "%,d", (item.customPrice * item.quantity)).padStart(17)
            } |"
            itemLines.add(firstLine)

            // For the second line, combine the second line of product name (if exists) with description
            if (limitedProductNameLines.size > 1) {
                // If there's a second line of product name, show it along with description
                val secondLine = "|     | ${
                    if (description.isNotEmpty()) description.padEnd(15) else " ".repeat(15)
                } | ${limitedProductNameLines[1].padEnd(24)} |                 |                   |"
                itemLines.add(secondLine)
            } else if (description.isNotEmpty()) {
                // If there's no second product name line but there is a description, show just description
                val descriptionLine = "|     | ${description.padEnd(15)} |                          |                 |                   |"
                itemLines.add(descriptionLine)
            }

            // Add an empty line between items for better readability
            // But only add if this isn't the last item to avoid extra space before the footer
            if (i < cartItems.size - 1) {
                val emptyLine = "|     |                 |                          |                 |                   |"
                itemLines.add(emptyLine)
            }

            // Check if item fits on current page
            if (currentLineCount + itemLines.size > maxContentLinesPerPage && currentLineCount > 0) {
                pages.add(currentPage)
                currentPage = ArrayList()
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

            // Add footer (full footer for last page)
            if (pageIndex == pages.size - 1) {
                for (line in tableFooter) {
                    result.append(line).append("\n")
                }
            } else {
                for (line in intermediateFooter) {
                    result.append(line).append("\n")
                }
            }
        }

        return result.toString().trimEnd()
    }

    // Add this number-to-words converter from HistoryReceiptGenerator
    private fun getAmountInWords(number: Long): String {
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

        val billionsStr = if (billions > 0) "${convertLessThanOneThousand(billions)} milyar" else ""
        val millionsStr = if (millions > 0) "${convertLessThanOneThousand(millions)} juta" else ""
        val thousandsStr = if (thousands > 0) "${convertLessThanOneThousand(thousands)} ribu" else ""
        val remainderStr = if (remainder > 0) convertLessThanOneThousand(remainder) else ""
        val rupiah = "rupiah"

        return listOf(billionsStr, millionsStr, thousandsStr, remainderStr, rupiah)
            .filter { it.isNotEmpty() }
            .joinToString(" ")
            .capitalize()
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

    fun wrapTerbilang(text: String, maxLength: Int): List<String> {
        val result = ArrayList<String>()
        var remainingText = text

        while (remainingText.isNotEmpty()) {
            if (remainingText.length <= maxLength) {
                // Jika sisa teks sudah cukup pendek, tambahkan semuanya
                result.add(remainingText)
                break
            } else {
                // Cari posisi spasi terakhir sebelum batas panjang
                var cutIndex = remainingText.substring(0, maxLength).lastIndexOf(' ')

                // Jika tidak ada spasi dalam range, potong di maxLength
                if (cutIndex == -1) {
                    cutIndex = maxLength
                }

                // Tambahkan potongan teks ke hasil
                result.add(remainingText.substring(0, cutIndex))

                // Update sisa teks dengan menghilangkan spasi di awal jika ada
                remainingText = remainingText.substring(cutIndex).trimStart()
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