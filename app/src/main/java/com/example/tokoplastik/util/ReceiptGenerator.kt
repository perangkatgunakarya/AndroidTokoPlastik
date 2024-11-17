package com.example.tokoplastik.util

import com.example.tokoplastik.data.responses.CartItem
import com.example.tokoplastik.data.responses.Transaction
import com.example.tokoplastik.data.responses.TransactionProduct
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReceiptGenerator {
    fun generateHtmlReceipt(
        orderData: Transaction,
        cartItems: List<CartItem>,
        orderId: String
    ): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        val date = dateFormat.format(Date(orderData.createdAt))

        val itemsHtml = buildString {
            cartItems.forEach { item ->
                append(
                    """
                    <tr>
                        <td>${item.product?.data?.name}</td>
                        <td>${item.quantity}</td>
                        <td>${item.selectedPrice.unit}</td>
                        <td>Rp ${item.customPrice.toRupiah()}</td>
                        <td>Rp ${(item.quantity * item.customPrice).toRupiah()}</td>
                    </tr>
                """.trimIndent()
                )
            }
        }

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; }
                    .receipt { max-width: 400px; margin: 0 auto; padding: 20px; }
                    .header { text-align: center; margin-bottom: 20px; }
                    .table { width: 100%; border-collapse: collapse; margin: 20px 0; }
                    .table th, .table td { border: 1px solid #ddd; padding: 8px; text-align: left; }
                    .footer { text-align: center; margin-top: 20px; }
                    .total { font-weight: bold; text-align: right; margin-top: 20px; }
                </style>
            </head>
            <body>
                <div class="receipt">
                    <div class="header">
                        <h2>TOKO PLASTIK</h2>
                        <p>Order ID: $orderId</p>
                        <p>Date: $date</p>
                        <p>Payment Method: ${orderData.paymentStatus}</p>
                    </div>

                    <table class="table">
                        <thead>
                            <tr>
                                <th>Item</th>
                                <th>Qty</th>
                                <th>Unit</th>
                                <th>Price</th>
                                <th>Subtotal</th>
                            </tr>
                        </thead>
                        <tbody>
                            $itemsHtml
                        </tbody>
                    </table>

                    <div class="total">
                        <p>Total: Rp ${orderData.total.toRupiah()}</p>
                    </div>

                    <div class="footer">
                        <p>Thank you for shopping!</p>
                        <p>Please come again</p>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    private fun Int.toRupiah(): String = String.format("%,d", this).replace(",", ".")
}
