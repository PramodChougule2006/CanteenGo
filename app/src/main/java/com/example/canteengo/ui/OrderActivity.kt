package com.example.canteengo.ui

import android.app.Activity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import com.example.canteengo.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class OrderActivity : Activity() {

    lateinit var listView: ListView

    lateinit var db: FirebaseFirestore

    lateinit var auth: FirebaseAuth

    private val orderList = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_order)

        listView = findViewById(R.id.orderListView)

        db = FirebaseFirestore.getInstance()

        auth = FirebaseAuth.getInstance()

        val currentUser = auth.currentUser

        if (currentUser == null) {

            Toast.makeText(
                this,
                "User session expired",
                Toast.LENGTH_SHORT
            ).show()

            finish()

            return
        }

        loadOrders(currentUser.uid)
    }

    private fun loadOrders(userId: String) {

        db.collection("orders")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { result ->

                orderList.clear()

                if (result.isEmpty) {

                    orderList.add("No orders found 📦")
                }

                for (doc in result) {

                    val items =
                        doc.get("items")?.toString()
                            ?: "No items"

                    val total =
                        doc.getLong("total") ?: 0

                    val status =
                        doc.getString("status")
                            ?: "pending"

                    orderList.add(
                        "Items: $items\n" +
                                "Total: ₹$total\n" +
                                "Status: $status"
                    )
                }

                val adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_list_item_1,
                    orderList
                )

                listView.adapter = adapter
            }
            .addOnFailureListener {

                Toast.makeText(
                    this,
                    "Failed to load orders",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
}