package com.example.canteengo.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import com.example.canteengo.R

class CartActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_cart)

        val listView = findViewById<ListView>(R.id.cartListView)
        val totalText = findViewById<TextView>(R.id.totalText)
        val checkoutBtn = findViewById<Button>(R.id.checkoutBtn)

        val cartItems =
            intent.getStringArrayListExtra("cartItems") ?: arrayListOf()

        val totalAmount =
            intent.getIntExtra("totalAmount", 0)

        if (cartItems.isEmpty()) {

            Toast.makeText(
                this,
                "Cart is empty",
                Toast.LENGTH_SHORT
            ).show()
        }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            cartItems
        )

        listView.adapter = adapter

        totalText.text = "Total: ₹$totalAmount"

        checkoutBtn.setOnClickListener {

            val paymentIntent =
                Intent(this, PaymentActivity::class.java)

            paymentIntent.putStringArrayListExtra(
                "cartItems",
                cartItems
            )

            paymentIntent.putExtra(
                "totalAmount",
                totalAmount
            )

            startActivity(paymentIntent)
        }
    }
}