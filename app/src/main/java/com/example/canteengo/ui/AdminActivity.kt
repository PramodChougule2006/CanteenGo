
package com.example.canteengo.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import com.example.canteengo.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.integration.android.IntentIntegrator

class AdminActivity : Activity() {

    lateinit var db: FirebaseFirestore
    lateinit var auth: FirebaseAuth

    lateinit var menuListView: ListView

    val menuList = ArrayList<String>()
    val menuIds = ArrayList<String>()

    var currentCanteenId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_admin)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val currentUser = auth.currentUser

        if (currentUser == null) {
            Toast.makeText(this, "User session expired", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val orderIdEt = findViewById<EditText>(R.id.orderIdEt)
        val verifyBtn = findViewById<Button>(R.id.verifyBtn)

        val nameEt = findViewById<EditText>(R.id.canteenNameEt)
        val collegeEt = findViewById<EditText>(R.id.collegeNameEt)
        val locationEt = findViewById<EditText>(R.id.locationEt)

        val saveBtn = findViewById<Button>(R.id.saveBtn)

        val itemNameEt = findViewById<EditText>(R.id.itemNameEt)
        val itemPriceEt = findViewById<EditText>(R.id.itemPriceEt)

        val itemTypeSpinner = findViewById<Spinner>(R.id.itemTypeSpinner)

        val addItemBtn = findViewById<Button>(R.id.addItemBtn)

        menuListView = findViewById(R.id.menuListView)

        checkCanteenExists(currentUser.uid)

        val types = arrayOf("Veg", "Non-Veg")

        val spinnerAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            types
        )

        itemTypeSpinner.adapter = spinnerAdapter

        val scanBtn = findViewById<Button>(R.id.scanBtn)

        scanBtn.setOnClickListener {

            val integrator = IntentIntegrator(this)

            integrator.setPrompt("Scan Order QR")
            integrator.setBeepEnabled(true)
            integrator.setOrientationLocked(true)

            integrator.initiateScan()
        }

        verifyBtn.setOnClickListener {

            val orderId = orderIdEt.text.toString()
                .replace("Order ID:", "")
                .trim()

            if (orderId.isEmpty()) {
                Toast.makeText(this, "Enter Order ID", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            db.collection("orders")
                .document(orderId)
                .get()
                .addOnSuccessListener { doc ->

                    if (doc.exists()) {

                        db.collection("orders")
                            .document(orderId)
                            .update("status", "completed")
                            .addOnSuccessListener {

                                Toast.makeText(
                                    this,
                                    "Order Completed ✅",
                                    Toast.LENGTH_SHORT
                                ).show()

                                orderIdEt.text.clear()
                            }
                            .addOnFailureListener {

                                Toast.makeText(
                                    this,
                                    "Failed to update order",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                    } else {

                        Toast.makeText(
                            this,
                            "Order not found ❌",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                .addOnFailureListener {

                    Toast.makeText(
                        this,
                        "Network error",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }

        saveBtn.setOnClickListener {

            val name = nameEt.text.toString().trim()
            val college = collegeEt.text.toString().trim()
            val location = locationEt.text.toString().trim()

            if (currentCanteenId != null) {

                Toast.makeText(
                    this,
                    "Canteen already exists ❌",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            if (name.isEmpty() || college.isEmpty() || location.isEmpty()) {

                Toast.makeText(
                    this,
                    "Fill all fields",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            val canteenMap = hashMapOf(
                "name" to name,
                "college" to college,
                "location" to location,
                "ownerId" to currentUser.uid
            )

            db.collection("canteens")
                .add(canteenMap)
                .addOnSuccessListener { documentRef ->

                    currentCanteenId = documentRef.id

                    Toast.makeText(
                        this,
                        "Canteen Added ✅",
                        Toast.LENGTH_SHORT
                    ).show()

                    loadMenu()
                }
                .addOnFailureListener {

                    Toast.makeText(
                        this,
                        "Failed to save canteen",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }

        addItemBtn.setOnClickListener {

            val itemName = itemNameEt.text.toString().trim()

            val itemPrice = itemPriceEt.text.toString().trim()

            val itemType = itemTypeSpinner.selectedItem.toString()

            val priceInt = itemPrice.toIntOrNull()

            if (itemName.isEmpty() || priceInt == null) {

                Toast.makeText(
                    this,
                    "Enter valid details",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            val canteenId = currentCanteenId

            if (canteenId == null) {

                Toast.makeText(
                    this,
                    "Create canteen first ❌",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            val menuMap = hashMapOf(
                "name" to itemName,
                "price" to priceInt,
                "type" to itemType
            )

            db.collection("canteens")
                .document(canteenId)
                .collection("menu")
                .add(menuMap)
                .addOnSuccessListener {

                    Toast.makeText(
                        this,
                        "Item Added ✅",
                        Toast.LENGTH_SHORT
                    ).show()

                    itemNameEt.text.clear()
                    itemPriceEt.text.clear()

                    loadMenu()
                }
                .addOnFailureListener {

                    Toast.makeText(
                        this,
                        "Failed to add item",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }

        menuListView.setOnItemLongClickListener { _, _, position, _ ->

            val canteenId = currentCanteenId ?: return@setOnItemLongClickListener true

            val menuId = menuIds[position]

            db.collection("canteens")
                .document(canteenId)
                .collection("menu")
                .document(menuId)
                .delete()
                .addOnSuccessListener {

                    Toast.makeText(
                        this,
                        "Item Deleted ❌",
                        Toast.LENGTH_SHORT
                    ).show()

                    loadMenu()
                }

            true
        }

        menuListView.setOnItemClickListener { _, _, position, _ ->

            val menuId = menuIds[position]

            val itemText = menuList[position]

            val parts = itemText.split(" - ₹")

            val oldName = parts.getOrNull(0) ?: ""

            val oldPrice = parts.getOrNull(1) ?: "0"

            showEditDialog(menuId, oldName, oldPrice)
        }
    }

    private fun checkCanteenExists(userId: String) {

        val canteenForm =
            findViewById<LinearLayout>(R.id.canteenFormLayout)

        val menuLayout =
            findViewById<LinearLayout>(R.id.menuLayout)

        db.collection("canteens")
            .whereEqualTo("ownerId", userId)
            .get()
            .addOnSuccessListener { result ->

                if (!result.isEmpty) {

                    val doc = result.documents[0]

                    currentCanteenId = doc.id

                    canteenForm.visibility = LinearLayout.GONE
                    menuLayout.visibility = LinearLayout.VISIBLE

                    loadMenu()

                } else {

                    canteenForm.visibility = LinearLayout.VISIBLE
                    menuLayout.visibility = LinearLayout.GONE
                }
            }
    }

    private fun loadMenu() {

        val canteenId = currentCanteenId ?: return

        menuList.clear()
        menuIds.clear()

        db.collection("canteens")
            .document(canteenId)
            .collection("menu")
            .get()
            .addOnSuccessListener { result ->

                for (doc in result) {

                    val name = doc.getString("name") ?: ""

                    val price = doc.getLong("price") ?: 0

                    menuList.add("$name - ₹$price")

                    menuIds.add(doc.id)
                }

                val adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_list_item_1,
                    menuList
                )

                menuListView.adapter = adapter
            }
    }

    private fun showEditDialog(
        menuId: String,
        oldName: String,
        oldPrice: String
    ) {

        val dialogView =
            layoutInflater.inflate(R.layout.dialog_edit_item, null)

        val nameEt =
            dialogView.findViewById<EditText>(R.id.editItemNameEt)

        val priceEt =
            dialogView.findViewById<EditText>(R.id.editItemPriceEt)

        val updateBtn =
            dialogView.findViewById<Button>(R.id.updateBtn)

        nameEt.setText(oldName)

        priceEt.setText(oldPrice)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setNegativeButton("Cancel") { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .create()

        updateBtn.setOnClickListener {

            val newName = nameEt.text.toString().trim()

            val newPrice =
                priceEt.text.toString().trim().toIntOrNull()

            if (newName.isEmpty() || newPrice == null) {

                Toast.makeText(
                    this,
                    "Enter valid details ❌",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            val canteenId = currentCanteenId ?: return@setOnClickListener

            val updatedMap = hashMapOf<String, Any>(
                "name" to newName,
                "price" to newPrice
            )

            db.collection("canteens")
                .document(canteenId)
                .collection("menu")
                .document(menuId)
                .update(updatedMap)
                .addOnSuccessListener {

                    Toast.makeText(
                        this,
                        "Item Updated ✏️",
                        Toast.LENGTH_SHORT
                    ).show()

                    dialog.dismiss()

                    loadMenu()
                }
        }

        dialog.show()
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {

        super.onActivityResult(requestCode, resultCode, data)

        val result = IntentIntegrator.parseActivityResult(
            requestCode,
            resultCode,
            data
        )

        if (result != null) {

            if (result.contents != null) {

                val scannedData = result.contents.trim()

                val orderId = scannedData
                    .replace("Order:", "")
                    .trim()

                if (orderId.isEmpty()) {

                    Toast.makeText(
                        this,
                        "Invalid QR ❌",
                        Toast.LENGTH_SHORT
                    ).show()

                    return
                }

                db.collection("orders")
                    .document(orderId)
                    .get()
                    .addOnSuccessListener { doc ->

                        if (doc.exists()) {

                            val status =
                                doc.getString("status")

                            if (status == "completed") {

                                Toast.makeText(
                                    this,
                                    "Already used ❌",
                                    Toast.LENGTH_SHORT
                                ).show()

                                return@addOnSuccessListener
                            }

                            db.collection("orders")
                                .document(orderId)
                                .update("status", "completed")
                                .addOnSuccessListener {

                                    Toast.makeText(
                                        this,
                                        "Order Completed ✅",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }

                        } else {

                            Toast.makeText(
                                this,
                                "Order not found ❌",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

            } else {

                Toast.makeText(
                    this,
                    "Scan Cancelled ❌",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
