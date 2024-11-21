package com.example.tokoplastik.util

import android.content.Context
import android.content.Intent
import android.graphics.pdf.PdfDocument
import android.hardware.usb.UsbManager
import android.os.Environment
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import androidx.core.content.FileProvider
import com.example.tokoplastik.R
import com.example.tokoplastik.data.responses.CartItem
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
                addElement(Paragraph("Tanggal      : ${SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date())}", detailsFont))
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
                    "${item.quantity} ${item.selectedPrice.unit}",
                    item.product?.data?.name,
                    String.format("Rp %,d", item.customPrice),
                    String.format("Rp %,d", item.customPrice * item.quantity)
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
                "Total: ${String.format("Rp %,d", orderData.total.toLong())}",
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

    fun printReceipt(
        orderData: Transaction,
        cartItems: List<CartItem>,
        orderId: String
    ) {
        val printerCommands = StringBuilder()

        printerCommands.append(byteArrayOf(0x1B, 0x40).toString()) // Initialize printer
        printerCommands.append(byteArrayOf(0x1B, 0x33, 0x18).toString()) // Set line spacing
        printerCommands.append(byteArrayOf(0x1B, 0x61, 0x01).toString()) // Center alignment

        printerCommands.append("PT PERANGKAT GUNA KARYA\n")
        printerCommands.append(byteArrayOf(0x1B, 0x45, 0x01).toString()) // Bold ON
        printerCommands.append("INVOICE\n")
        printerCommands.append(byteArrayOf(0x1B, 0x45, 0x00).toString()) // Bold OFF
        printerCommands.append(byteArrayOf(0x1B, 0x61, 0x00).toString()) // Left alignment

        printerCommands.append("\n")
        printerCommands.append("${orderData.customer.name}\n")
        printerCommands.append("${orderData.customerId}\n")

        printerCommands.append("\n")
        printerCommands.append("Referensi   : INV-${orderId}\n")
        printerCommands.append("Tanggal     : ${SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date())}\n")
        printerCommands.append("STATUS      : ${orderData.paymentStatus}\n")
        printerCommands.append("\n")

        printerCommands.append("----------------------------------------\n")
        printerCommands.append(
            String.format(
                "%-15s %-8s %-5s %-8s %8s\n",
                "Produk", "Harga", "Qty", "Unit", "Jumlah"
            )
        )
        printerCommands.append("----------------------------------------\n")

        cartItems.forEach { item ->
            printerCommands.append(String.format("%-15s\n", item.product?.data?.name))
            printerCommands.append(
                String.format(
                    "%8.0f x %-5d %8.0f%% %11.0f\n",
                    item.customPrice,
                    item.quantity,
                    item.selectedPrice.unit,
                    item.customPrice * item.quantity
                )
            )
        }

        printerCommands.append(String.format("Total %33.0f\n", orderData.total))
        printerCommands.append("----------------------------------------\n")

        printerCommands.append("\n")
        printerCommands.append(byteArrayOf(0x1B, 0x61, 0x01).toString()) // Center alignment
        printerCommands.append("Dengan Hormat,\n\n\n")
        printerCommands.append("Toko Plastik H. Ali\n")
        printerCommands.append("Penjualan\n")

        printerCommands.append("\n\n\n\n")
        printerCommands.append(byteArrayOf(0x1D, 0x56, 0x41, 0x10).toString()) // Paper cut

        Thread {
            try {
                val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
                val deviceList = usbManager.deviceList

                val epsonDevice = deviceList.values.find { device ->
                    device.vendorId == 0x04b8 // Epson vendor ID
                }

                epsonDevice?.let { device ->
                    if (usbManager.hasPermission(device)) {
                        val connection = usbManager.openDevice(device)
                        val endpoint = device.getInterface(0).getEndpoint(0)

                        connection.bulkTransfer(
                            endpoint,
                            printerCommands.toString().toByteArray(),
                            printerCommands.toString().length,
                            5000
                        )

                        connection.close()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }
}
