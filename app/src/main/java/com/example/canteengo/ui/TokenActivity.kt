package com.example.canteengo.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.example.canteengo.R
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

class TokenActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_token)

        val orderId =
            intent.getStringExtra("orderId") ?: ""

        val amount =
            intent.getIntExtra("amount", 0)

        val orderTv =
            findViewById<TextView>(R.id.orderIdTv)

        val amountTv =
            findViewById<TextView>(R.id.amountTv)

        val qrImage =
            findViewById<ImageView>(R.id.qrImage)

        val backBtn =
            findViewById<Button>(R.id.backBtn)

        orderTv.text = "Order ID: $orderId"

        amountTv.text = "Amount: ₹$amount"

        val items =
            intent.getStringArrayListExtra("items")
                ?: arrayListOf()

        val itemsText =
            items.joinToString("\n- ")

        val qrData = """
Order:$orderId
Amount:₹$amount
Items:
- $itemsText
        """.trimIndent()

        val bitmap = generateQR(qrData)

        qrImage.setImageBitmap(bitmap)

        backBtn.setOnClickListener {

            val intent = Intent(
                this,
                UserActivity::class.java
            )

            intent.flags =
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_NEW_TASK

            startActivity(intent)

            finish()
        }
    }

    private fun generateQR(text: String): Bitmap {

        val size = 512

        val bits = QRCodeWriter().encode(
            text,
            BarcodeFormat.QR_CODE,
            size,
            size
        )

        val bitmap = Bitmap.createBitmap(
            size,
            size,
            Bitmap.Config.RGB_565
        )

        for (x in 0 until size) {

            for (y in 0 until size) {

                bitmap.setPixel(
                    x,
                    y,
                    if (bits[x, y])
                        android.graphics.Color.BLACK
                    else
                        android.graphics.Color.WHITE
                )
            }
        }

        return bitmap
    }
}