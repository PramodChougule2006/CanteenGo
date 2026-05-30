package com.example.canteengo.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import com.example.canteengo.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OrderHistoryActivity : Activity() {

    lateinit var listView: ListView

    private val orderList = ArrayList<String>()

    private val orderIds = ArrayList<String>()

    private val amounts = ArrayList<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_order_history)

        listView = findViewById(R.id.orderListView)

        loadOrders()

        listView.setOnItemClickListener { _, _, position, _ ->

            if (position >= orderIds.size ||
                position >= amounts.size
            ) {
                return@setOnItemClickListener
            }

            val intent =
                Intent(this, TokenActivity::class.java)

            intent.putExtra(
                "orderId",
                orderIds[position]
            )

            intent.putExtra(
                "amount",
                amounts[position]
            )

            startActivity(intent)
        }
    }

    private fun loadOrders() {

        val currentUser =
            FirebaseAuth.getInstance().currentUser

        if (currentUser == null) {

            Toast.makeText(
                this,
                "User session expired",
                Toast.LENGTH_SHORT
            ).show()

            finish()

            return
        }

        val db = FirebaseFirestore.getInstance()

        val formatter = SimpleDateFormat(
            "dd MMM yyyy, hh:mm a",
            Locale.getDefault()
        )

        db.collection("orders")
            .whereEqualTo("userId", currentUser.uid)
            .orderBy(
                "timestamp",
                com.google.firebase.firestore.Query.Direction.DESCENDING
            )
            .get()
            .addOnSuccessListener { result ->

                orderList.clear()
                orderIds.clear()
                amounts.clear()

                if (result.isEmpty) {

                    orderList.add("No orders found 📦")
                }

                for (doc in result) {

                    val id = doc.id

                    val amount =
                        (doc.getLong("total") ?: 0).toInt()

                    val status =
                        doc.getString("status")
                            ?: "pending"

                    val time =
                        doc.getLong("timestamp")
                            ?: System.currentTimeMillis()

                    val date =
                        formatter.format(Date(time))

                    orderList.add(
                        "Rs.$amount | ${status.uppercase()}\n$date"
                    )

                    orderIds.add(id)

                    amounts.add(amount)
                }

                val adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_list_item_1,
                    orderList
                )

                listView.adapter = adapter
            }
            .addOnFailureListener { e ->

                Toast.makeText(
                    this,
                    "Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}