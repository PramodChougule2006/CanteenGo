package com.example.canteengo.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.*
import com.example.canteengo.R
import com.google.firebase.firestore.FirebaseFirestore

class MenuActivity : Activity() {

    private val cartItems = ArrayList<String>()

    private var totalAmount = 0

    lateinit var listView: ListView

    lateinit var db: FirebaseFirestore

    private val menuData =
        ArrayList<HashMap<String, String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_menu)

        listView = findViewById(R.id.menuListView)

        val viewCartBtn =
            findViewById<Button>(R.id.viewCartBtn)

        db = FirebaseFirestore.getInstance()

        val canteenId =
            intent.getStringExtra("canteenId")

        if (canteenId.isNullOrEmpty()) {

            Toast.makeText(
                this,
                "Canteen not found",
                Toast.LENGTH_SHORT
            ).show()

            finish()

            return
        }

        loadMenu(canteenId)

        viewCartBtn.setOnClickListener {

            val cartIntent =
                Intent(this, CartActivity::class.java)

            cartIntent.putStringArrayListExtra(
                "cartItems",
                cartItems
            )

            cartIntent.putExtra(
                "totalAmount",
                totalAmount
            )

            startActivity(cartIntent)
        }
    }

    private fun loadMenu(canteenId: String) {

        db.collection("canteens")
            .document(canteenId)
            .collection("menu")
            .get()
            .addOnSuccessListener { result ->

                menuData.clear()

                if (result.isEmpty) {

                    Toast.makeText(
                        this,
                        "No menu items available",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                for (doc in result) {

                    val map = HashMap<String, String>()

                    val name =
                        doc.getString("name") ?: ""

                    val price =
                        doc.getLong("price") ?: 0

                    val type =
                        doc.getString("type") ?: ""

                    map["name"] = name
                    map["price"] = "₹$price"
                    map["type"] = type

                    menuData.add(map)
                }

                val adapter = SimpleAdapter(
                    this,
                    menuData,
                    R.layout.menu_item,
                    arrayOf("name", "price", "type"),
                    intArrayOf(
                        R.id.itemNameTv,
                        R.id.itemPriceTv,
                        R.id.itemTypeTv
                    )
                )

                listView.adapter = adapter

                listView.setOnItemClickListener { _, _, position, _ ->

                    val item = menuData[position]

                    val name =
                        item["name"] ?: ""

                    val priceText =
                        item["price"]
                            ?.replace("₹", "")
                            ?.trim()

                    val price =
                        priceText?.toIntOrNull() ?: 0

                    cartItems.add("$name ₹$price")

                    totalAmount += price

                    Toast.makeText(
                        this,
                        "$name added | Total ₹$totalAmount",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .addOnFailureListener {

                Toast.makeText(
                    this,
                    "Failed to load menu",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
}