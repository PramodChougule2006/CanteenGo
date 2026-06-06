package com.example.canteengo.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import com.example.canteengo.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OrderHistoryActivity : Activity() {

    lateinit var orderListView: ListView
    lateinit var sectionHeaderTv: TextView
    lateinit var pendingOrdersCard: CardView
    lateinit var completedOrdersCard: CardView

    private var showCompleted = false

    private val allOrders = ArrayList<OrderData>()

    data class OrderData(
        val id: String,
        val amount: Int,
        val status: String,
        val text: String,
        val paymentStatus: String
    )

    private val displayedList = ArrayList<String>()
    private val displayedIds = ArrayList<String>()
    private val displayedAmounts = ArrayList<Int>()
    private val displayedPaymentStatuses = ArrayList<String>()

    private var ordersListener: com.google.firebase.firestore.ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_order_history)

        orderListView = findViewById(R.id.orderListView)
        sectionHeaderTv = findViewById(R.id.sectionHeaderTv)
        pendingOrdersCard = findViewById(R.id.pendingOrdersCard)
        completedOrdersCard = findViewById(R.id.completedOrdersCard)

        loadOrders()

        pendingOrdersCard.setOnClickListener {
            showCompleted = false
            updateUI()
        }

        completedOrdersCard.setOnClickListener {
            showCompleted = true
            updateUI()
        }

        orderListView.setOnItemClickListener { _, _, position, _ ->

            if (position >= displayedIds.size ||
                position >= displayedAmounts.size
            ) {
                return@setOnItemClickListener
            }

            val intent =
                Intent(this, TokenActivity::class.java)

            intent.putExtra(
                "orderId",
                displayedIds[position]
            )

            intent.putExtra(
                "amount",
                displayedAmounts[position]
            )

            intent.putExtra(
                "paymentStatus",
                displayedPaymentStatuses[position]
            )

            startActivity(intent)
        }
    }

    private fun updateUI() {
        displayedList.clear()
        displayedIds.clear()
        displayedAmounts.clear()
        displayedPaymentStatuses.clear()

        if (showCompleted) {
            sectionHeaderTv.text = "Completed Orders"
            for (order in allOrders) {
                if (order.status.equals("completed", ignoreCase = true)) {
                    displayedList.add(order.text)
                    displayedIds.add(order.id)
                    displayedAmounts.add(order.amount)
                    displayedPaymentStatuses.add(order.paymentStatus)
                }
            }
            if (displayedList.isEmpty()) {
                displayedList.add("No completed orders 📦")
            }
        } else {
            sectionHeaderTv.text = "Pending Orders"
            for (order in allOrders) {
                if (!order.status.equals("completed", ignoreCase = true)) {
                    displayedList.add(order.text)
                    displayedIds.add(order.id)
                    displayedAmounts.add(order.amount)
                    displayedPaymentStatuses.add(order.paymentStatus)
                }
            }
            if (displayedList.isEmpty()) {
                displayedList.add("No pending orders 📦")
            }
        }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            displayedList
        )
        orderListView.adapter = adapter
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

        ordersListener = db.collection("orders")
            .whereEqualTo("userId", currentUser.uid)
            .orderBy(
                "timestamp",
                com.google.firebase.firestore.Query.Direction.DESCENDING
            )
            .addSnapshotListener { result, e ->

                if (e != null) {
                    Toast.makeText(
                        this,
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addSnapshotListener
                }

                allOrders.clear()

                if (result != null) {
                    for (doc in result) {

                        val id = doc.id

                        val amount =
                            (doc.getLong("total") ?: 0).toInt()

                        val status =
                            doc.getString("status")
                                ?: "pending"

                        val paymentStatus =
                            doc.getString("paymentStatus")
                                ?: if (status.equals("paid", ignoreCase = true)) "PAID" else "NOT PAID"

                        val time =
                            doc.getLong("timestamp")
                                ?: System.currentTimeMillis()

                        val date =
                            formatter.format(Date(time))

                        val itemText = "Rs.$amount | ${status.uppercase()}\n$date"

                        allOrders.add(OrderData(id, amount, status, itemText, paymentStatus))
                    }
                }

                updateUI()
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        ordersListener?.remove()
    }
}