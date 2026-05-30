package com.example.canteengo.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import com.example.canteengo.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignupActivity : Activity() {

    lateinit var auth: FirebaseAuth

    lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_signup)

        auth = FirebaseAuth.getInstance()

        db = FirebaseFirestore.getInstance()

        val email =
            findViewById<EditText>(R.id.emailEt)

        val password =
            findViewById<EditText>(R.id.passwordEt)

        val signupBtn =
            findViewById<Button>(R.id.signupBtn)

        val spinner =
            findViewById<Spinner>(R.id.roleSpinner)

        val roles = arrayOf(
            "User",
            "Admin"
        )

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            roles
        )

        spinner.adapter = adapter

        signupBtn.setOnClickListener {

            val emailText =
                email.text.toString().trim()

            val passText =
                password.text.toString().trim()

            val role =
                spinner.selectedItem.toString()

            if (
                emailText.isEmpty() ||
                passText.isEmpty()
            ) {

                Toast.makeText(
                    this,
                    "Fill all fields",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            if (
                !Patterns.EMAIL_ADDRESS.matcher(emailText)
                    .matches()
            ) {

                Toast.makeText(
                    this,
                    "Enter valid email",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            if (passText.length < 6) {

                Toast.makeText(
                    this,
                    "Password must be at least 6 characters",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            signupBtn.isEnabled = false

            auth.createUserWithEmailAndPassword(
                emailText,
                passText
            )
                .addOnSuccessListener {

                    val currentUser =
                        auth.currentUser

                    if (currentUser == null) {

                        signupBtn.isEnabled = true

                        Toast.makeText(
                            this,
                            "User creation failed",
                            Toast.LENGTH_SHORT
                        ).show()

                        return@addOnSuccessListener
                    }

                    val userMap = hashMapOf(
                        "email" to emailText,
                        "role" to role
                    )

                    db.collection("users")
                        .document(currentUser.uid)
                        .set(userMap)
                        .addOnSuccessListener {

                            Toast.makeText(
                                this,
                                "Account Created ✅",
                                Toast.LENGTH_SHORT
                            ).show()

                            startActivity(
                                Intent(
                                    this,
                                    LoginActivity::class.java
                                )
                            )

                            finish()
                        }
                        .addOnFailureListener {

                            signupBtn.isEnabled = true

                            Toast.makeText(
                                this,
                                "Failed to save user data",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
                .addOnFailureListener { e ->

                    signupBtn.isEnabled = true

                    Toast.makeText(
                        this,
                        e.message ?: "Signup Failed ❌",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }
}