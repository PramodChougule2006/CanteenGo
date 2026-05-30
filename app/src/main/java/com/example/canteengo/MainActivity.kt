package com.example.canteengo

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ THIS LINE FIXES CRASH
        setContentView(android.R.layout.simple_list_item_1)

        val db = FirebaseFirestore.getInstance()

        val testData = hashMapOf(
            "name" to "Pramod",
            "status" to "Connected"
        )

        db.collection("test")
            .add(testData)
            .addOnSuccessListener {
                Toast.makeText(this, "Firestore Connected ✅", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error ❌", Toast.LENGTH_LONG).show()
            }
    }
}