package com.example.canteengo.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import com.example.canteengo.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import org.json.JSONObject

class PaymentActivity : Activity(), PaymentResultListener {

    lateinit var paymentGroup: RadioGroup
    lateinit var payBtn: Button

    lateinit var db: FirebaseFirestore
    lateinit var auth: FirebaseAuth

    var cartItems: ArrayList<String>? = null
    var totalAmount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_payment)

        paymentGroup = findViewById(R.id.paymentGroup)
        payBtn = findViewById(R.id.payBtn)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        cartItems = intent.getStringArrayListExtra("cartItems") ?: arrayListOf()
        totalAmount = intent.getIntExtra("totalAmount", 0)

        if (cartItems!!.isEmpty() || totalAmount <= 0) {

            Toast.makeText(
                this,
                "Invalid cart data",
                Toast.LENGTH_LONG
            ).show()

            finish()
            return
        }
        Checkout.preload(applicationContext)

        val upiOption = findViewById<RadioButton>(R.id.upiOption)
        val cashOption = findViewById<RadioButton>(R.id.cashOption)

        val upiOptionContainer = findViewById<LinearLayout>(R.id.upiOptionContainer)
        val cashOptionContainer = findViewById<LinearLayout>(R.id.cashOptionContainer)

        upiOption.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                cashOption.isChecked = false
            }
        }

        cashOption.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                upiOption.isChecked = false
            }
        }

        upiOptionContainer.setOnClickListener {
            upiOption.isChecked = true
        }

        cashOptionContainer.setOnClickListener {
            cashOption.isChecked = true
        }

        payBtn.setOnClickListener {

            if (!upiOption.isChecked && !cashOption.isChecked) {
                Toast.makeText(this, "Select payment method", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (cashOption.isChecked) {
                processCashPayment()
            } else {
                startPayment()
            }
        }
    }

    private fun startPayment() {

        val checkout = Checkout()

        // 🔥 REPLACE WITH YOUR KEY ID
        checkout.setKeyID("rzp_test_SwoluZW48diqZm")

        try {

            val options = JSONObject()

            options.put("name", "CanteenGo")
            options.put("description", "Food Order Payment")
            options.put("theme.color", "#CDB4DB")

            // Amount in paise
            options.put("amount", totalAmount * 100)

            options.put("currency", "INR")

            val prefill = JSONObject()
            prefill.put("email", auth.currentUser?.email ?: "")

            options.put("prefill", prefill)

            checkout.open(this, options)

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Error: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun processCashPayment() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User session expired", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val userId = currentUser.uid

        val orderMap = hashMapOf(
            "userId" to userId,
            "items" to cartItems,
            "total" to totalAmount,
            "status" to "pending",
            "paymentStatus" to "NOT PAID",
            "paymentId" to "CASH",
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("orders")
            .add(orderMap)
            .addOnSuccessListener { documentRef ->

                Toast.makeText(
                    this,
                    "Order Placed (Cash on Delivery) ✅",
                    Toast.LENGTH_LONG
                ).show()

                val intent = Intent(this, TokenActivity::class.java)

                intent.putExtra("orderId", documentRef.id)
                intent.putExtra("amount", totalAmount)
                intent.putStringArrayListExtra("items", cartItems)
                intent.putExtra("paymentStatus", "NOT PAID")

                startActivity(intent)

                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Failed to place order: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    override fun onPaymentSuccess(razorpayPaymentID: String?) {

        val userId = auth.currentUser!!.uid

        val orderMap = hashMapOf(
            "userId" to userId,
            "items" to cartItems,
            "total" to totalAmount,
            "status" to "paid",
            "paymentStatus" to "PAID",
            "paymentId" to (razorpayPaymentID ?: ""),
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("orders")
            .add(orderMap)
            .addOnSuccessListener { documentRef ->

                Toast.makeText(
                    this,
                    "Payment Successful ✅",
                    Toast.LENGTH_LONG
                ).show()

                val intent = Intent(this, TokenActivity::class.java)

                intent.putExtra("orderId", documentRef.id)
                intent.putExtra("amount", totalAmount)
                intent.putStringArrayListExtra("items", cartItems)
                intent.putExtra("paymentStatus", "PAID")

                startActivity(intent)

                finish()
            }
    }

    override fun onPaymentError(code: Int, response: String?) {

        Toast.makeText(
            this,
            "Payment Failed ❌",
            Toast.LENGTH_LONG
        ).show()
    }
}