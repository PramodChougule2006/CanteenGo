package com.example.canteengo.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.example.canteengo.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : Activity() {

    lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        val emailEt = findViewById<EditText>(R.id.emailEt)
        val passwordEt = findViewById<EditText>(R.id.passwordEt)

        val loginBtn = findViewById<Button>(R.id.loginBtn)
        val signupBtn = findViewById<Button>(R.id.signupBtn)

        loginBtn.setOnClickListener {

            val emailText =
                emailEt.text.toString().trim()

            val passText =
                passwordEt.text.toString().trim()

            if (emailText.isEmpty() || passText.isEmpty()) {

                Toast.makeText(
                    this,
                    "Fill all fields",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            loginBtn.isEnabled = false

            auth.signInWithEmailAndPassword(
                emailText,
                passText
            )
                .addOnSuccessListener {

                    val currentUser = auth.currentUser

                    if (currentUser == null) {

                        loginBtn.isEnabled = true

                        Toast.makeText(
                            this,
                            "User session error",
                            Toast.LENGTH_SHORT
                        ).show()

                        return@addOnSuccessListener
                    }

                    val userId = currentUser.uid

                    val db =
                        FirebaseFirestore.getInstance()

                    db.collection("users")
                        .document(userId)
                        .get()
                        .addOnSuccessListener { document ->

                            loginBtn.isEnabled = true

                            if (!document.exists()) {

                                Toast.makeText(
                                    this,
                                    "User data not found",
                                    Toast.LENGTH_SHORT
                                ).show()

                                return@addOnSuccessListener
                            }

                            val role =
                                document.getString("role")

                            if (role == "Admin") {

                                startActivity(
                                    Intent(
                                        this,
                                        AdminActivity::class.java
                                    )
                                )

                            } else {

                                startActivity(
                                    Intent(
                                        this,
                                        UserActivity::class.java
                                    )
                                )
                            }

                            finish()
                        }
                        .addOnFailureListener {

                            loginBtn.isEnabled = true

                            Toast.makeText(
                                this,
                                "Failed to fetch user data",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
                .addOnFailureListener {

                    loginBtn.isEnabled = true

                    Toast.makeText(
                        this,
                        "Login Failed ❌",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }

        signupBtn.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    SignupActivity::class.java
                )
            )
        }
    }
}