package com.example.canteengo.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
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

        payBtn.setOnClickListener {

            val selectedId = paymentGroup.checkedRadioButtonId

            if (selectedId == -1) {
                Toast.makeText(this, "Select payment method", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            startPayment()
        }
    }

    private fun startPayment() {

        val checkout = Checkout()

        // 🔥 REPLACE WITH YOUR KEY ID
        checkout.setKeyID("rzp_test_StbPAiGZpkvOvr")

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

    override fun onPaymentSuccess(razorpayPaymentID: String?) {

        val userId = auth.currentUser!!.uid

        val orderMap = hashMapOf(
            "userId" to userId,
            "items" to cartItems,
            "total" to totalAmount,
            "status" to "paid",
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