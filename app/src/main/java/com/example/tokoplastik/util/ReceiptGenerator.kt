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

class ReceiptGenerator (
    private val context: Context
){
    fun generatedPdfReceipt (
        orderData: Transaction,
        cartItems: List<CartItem>,
        orderId: String
    ) : File {
        val filename = "Invoice_${orderId}.pdf"
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), filename)

        val document = Document()
        try {
            PdfWriter.getInstance(document, FileOutputStream(file))
            document.open()

            // header
            document.add(Paragraph("TOKO PLASTIK HAJI ALI", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18f)))
            document.add(Paragraph("INVOICE", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18f)))
            document.add(Paragraph("Tanggal: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())}", FontFactory.getFont(FontFactory.HELVETICA, 12f)))
            document.add(Paragraph("No Invoice: TPHA-$orderId", FontFactory.getFont(FontFactory.COURIER, 12f)))
            document.add(Paragraph("Customer: ${orderData.customerId}", FontFactory.getFont(FontFactory.COURIER, 12f)))

            // table
            val table = PdfPTable(5)
            table.widthPercentage = 100f
            table.setWidths(floatArrayOf(3f, 1f, 1f, 1f, 1f))

            // table headers
            arrayOf("Product", "Price", "Quantity", "Unit", "Total").forEach { header ->
                val cell = PdfPCell(Phrase(header))
                cell.horizontalAlignment = Element.ALIGN_CENTER
                table.addCell(cell)
            }

            cartItems.forEach { cartItem ->
                table.addCell(cartItem.productPrice.first().product.name)
                table.addCell(String.format("Rp. %,d", cartItem.customPrice))
                table.addCell(cartItem.quantity.toString())
                table.addCell(cartItem.selectedPrice.unit)
                table.addCell(String.format("Rp. %,d", cartItem.customPrice * cartItem.quantity))
            }

            document.add(table)
            document.add(Paragraph("\n"))

            // footer
            document.add(Paragraph("Total: Rp. ${String.format("Rp. %,d", orderData.total)}", FontFactory.getFont(FontFactory.COURIER_BOLD, 12f)))
            document.add(Paragraph("Status Pembayaran: ${orderData.paymentStatus}", FontFactory.getFont(FontFactory.COURIER_BOLD, 12f)))

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
        // initialize printer
        printerCommands.append(byteArrayOf(0x1B, 0x40).toString())
        // set line spacing
        printerCommands.append(byteArrayOf(0x1B, 0x33, 0x18).toString())

        //header
        printerCommands.append("TOKO PLASTIK HAJI ALI\n")
        printerCommands.append("INVOICE\n")
        printerCommands.append("Tanggal: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())}\n")
        printerCommands.append("No Invoice: TPHA-${orderId}\n")
        printerCommands.append("Customer: ${orderData.customerId}")
        printerCommands.append("\n")

        // table
        printerCommands.append(String.format("%-20s %8s %12s %12s %12s\n", "Product", "Price", "Quantity", "Unit", "Total"))
        printerCommands.append("-".repeat(64) + "\n")

        Thread {
            try {
                val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
                val deviceList = usbManager.deviceList

                // Find Epson printer
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
