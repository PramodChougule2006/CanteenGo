package com.example.canteengo.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ListView
import android.widget.SimpleAdapter
import android.widget.Toast
import com.example.canteengo.R
import com.google.firebase.firestore.FirebaseFirestore

class UserActivity : Activity() {

    lateinit var listView: ListView

    lateinit var db: FirebaseFirestore

    private val canteenIds = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_user)

        listView = findViewById(R.id.canteenListView)

        db = FirebaseFirestore.getInstance()

        val ordersBtn =
            findViewById<Button>(R.id.viewOrdersBtn)

        loadCanteens()

        ordersBtn.setOnClickListener {

            val intent = Intent(
                this,
                OrderHistoryActivity::class.java
            )

            startActivity(intent)
        }

        listView.setOnItemClickListener { _, _, position, _ ->

            if (position >= canteenIds.size) {
                return@setOnItemClickListener
            }

            val canteenId = canteenIds[position]

            val intent = Intent(
                this,
                MenuActivity::class.java
            )

            intent.putExtra(
                "canteenId",
                canteenId
            )

            startActivity(intent)
        }
    }

    private fun loadCanteens() {

        val list =
            ArrayList<HashMap<String, String>>()

        db.collection("canteens")
            .get()
            .addOnSuccessListener { result ->

                list.clear()

                canteenIds.clear()

                if (result.isEmpty) {

                    val emptyMap =
                        HashMap<String, String>()

                    emptyMap["name"] =
                        "No canteens available"

                    emptyMap["college"] =
                        "Please check later"

                    emptyMap["location"] = ""

                    list.add(emptyMap)
                }

                for (doc in result) {

                    val map =
                        HashMap<String, String>()

                    map["name"] =
                        doc.getString("name") ?: ""

                    map["college"] =
                        doc.getString("college") ?: ""

                    map["location"] =
                        doc.getString("location") ?: ""

                    list.add(map)

                    canteenIds.add(doc.id)
                }

                val adapter = SimpleAdapter(
                    this,
                    list,
                    R.layout.canteen_item,
                    arrayOf(
                        "name",
                        "college",
                        "location"
                    ),
                    intArrayOf(
                        R.id.nameTv,
                        R.id.collegeTv,
                        R.id.locationTv
                    )
                )

                listView.adapter = adapter
            }
            .addOnFailureListener {

                Toast.makeText(
                    this,
                    "Failed to load canteens",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
}