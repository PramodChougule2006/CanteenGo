
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
        val mobileEt = findViewById<EditText>(R.id.ownerMobileEt)
        val upiIdEt = findViewById<EditText>(R.id.upiIdEt)

        val saveBtn = findViewById<Button>(R.id.saveBtn)
        val adminTitleTv = findViewById<TextView>(R.id.adminTitleTv)
        val adminControlsLayout = findViewById<LinearLayout>(R.id.adminControlsLayout)

        val itemNameEt = findViewById<EditText>(R.id.itemNameEt)
        val itemPriceEt = findViewById<EditText>(R.id.itemPriceEt)

        val itemTypeSpinner = findViewById<Spinner>(R.id.itemTypeSpinner)

        val addItemBtn = findViewById<Button>(R.id.addItemBtn)

        checkCanteenExists(currentUser.uid)

        val types = arrayOf("Veg", "Non-Veg")

        val spinnerAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            types
        )

        itemTypeSpinner.adapter = spinnerAdapter

        val scanBtn = findViewById<Button>(R.id.scanBtn)

        // ── Accordion views ──────────────────────────────────
        val canteenInfoHeader   = findViewById<LinearLayout>(R.id.canteenInfoHeader)
        val canteenInfoContent  = findViewById<LinearLayout>(R.id.canteenInfoContent)
        val canteenInfoArrow    = findViewById<TextView>(R.id.canteenInfoArrow)

        val menuManagementHeader  = findViewById<LinearLayout>(R.id.menuManagementHeader)
        val menuManagementContent = findViewById<LinearLayout>(R.id.menuManagementContent)
        val menuManagementArrow   = findViewById<TextView>(R.id.menuManagementArrow)

        val verifyOrdersHeader  = findViewById<LinearLayout>(R.id.verifyOrdersHeader)
        val verifyOrdersContent = findViewById<LinearLayout>(R.id.verifyOrdersContent)
        val verifyOrdersArrow   = findViewById<TextView>(R.id.verifyOrdersArrow)

        val viewItemsBtn = findViewById<Button>(R.id.viewItemsBtn)

        // ── Accordion toggle click listeners ─────────────────
        canteenInfoHeader.setOnClickListener {
            toggleSection(canteenInfoContent, canteenInfoArrow)
        }

        menuManagementHeader.setOnClickListener {
            toggleSection(menuManagementContent, menuManagementArrow)
        }

        verifyOrdersHeader.setOnClickListener {
            toggleSection(verifyOrdersContent, verifyOrdersArrow)
        }

        // ── View current items list ───────────────────────────
        viewItemsBtn.setOnClickListener {
            showMenuItemsDialog()
        }

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
            val mobile = mobileEt.text.toString().trim()
            val upiId = upiIdEt.text.toString().trim()

            if (name.isEmpty() || college.isEmpty() || location.isEmpty() || mobile.isEmpty() || upiId.isEmpty()) {

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
                "mobile" to mobile,
                "upiId" to upiId,
                "ownerId" to currentUser.uid
            )

            val canteenId = currentCanteenId
            if (canteenId != null) {
                db.collection("canteens")
                    .document(canteenId)
                    .set(canteenMap)
                    .addOnSuccessListener {
                        Toast.makeText(
                            this,
                            "Canteen Details Updated ✅",
                            Toast.LENGTH_SHORT
                        ).show()
                        adminTitleTv.text = name
                    }
                    .addOnFailureListener {
                        Toast.makeText(
                            this,
                            "Failed to update details",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            } else {
                db.collection("canteens")
                    .add(canteenMap)
                    .addOnSuccessListener { documentRef ->

                        currentCanteenId = documentRef.id

                        Toast.makeText(
                            this,
                            "Canteen Added ✅",
                            Toast.LENGTH_SHORT
                        ).show()

                        saveBtn.text = "Update Canteen Details"
                        adminTitleTv.text = name
                        adminControlsLayout.visibility = LinearLayout.VISIBLE

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
    }

    // ─── Toggle accordion section ────────────────────────────────────────────
    private fun toggleSection(contentView: LinearLayout, arrowTv: TextView) {
        if (contentView.visibility == android.view.View.VISIBLE) {
            contentView.visibility = android.view.View.GONE
            arrowTv.text = "▶"
        } else {
            contentView.visibility = android.view.View.VISIBLE
            arrowTv.text = "▼"
        }
    }

    // ─── Show menu items in scrollable dialog ────────────────────────────────
    private fun showMenuItemsDialog() {
        if (menuList.isEmpty()) {
            android.widget.Toast.makeText(this, "No menu items yet", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        val items = menuList.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Current Menu Items")
            .setItems(items) { _, position ->
                val menuId  = menuIds[position]
                val itemText = menuList[position]
                val parts   = itemText.split(" - ₹")
                val oldName  = parts.getOrNull(0) ?: ""
                val oldPrice = parts.getOrNull(1) ?: "0"
                val options = arrayOf("Edit Item", "Delete Item")
                AlertDialog.Builder(this)
                    .setTitle("Manage: $oldName")
                    .setItems(options) { _, which ->
                        if (which == 0) showEditDialog(menuId, oldName, oldPrice)
                        else            showDeleteConfirmationDialog(menuId)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showDeleteConfirmationDialog(menuId: String) {
        val canteenId = currentCanteenId ?: return
        AlertDialog.Builder(this)
            .setTitle("Delete Item")
            .setMessage("Are you sure you want to delete this menu item?")
            .setPositiveButton("Delete") { _, _ ->
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
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun checkCanteenExists(userId: String) {

        val adminControlsLayout =
            findViewById<LinearLayout>(R.id.adminControlsLayout)

        val nameEt = findViewById<EditText>(R.id.canteenNameEt)
        val collegeEt = findViewById<EditText>(R.id.collegeNameEt)
        val locationEt = findViewById<EditText>(R.id.locationEt)
        val mobileEt = findViewById<EditText>(R.id.ownerMobileEt)
        val upiIdEt = findViewById<EditText>(R.id.upiIdEt)
        val saveBtn = findViewById<Button>(R.id.saveBtn)
        val adminTitleTv = findViewById<TextView>(R.id.adminTitleTv)

        db.collection("canteens")
            .whereEqualTo("ownerId", userId)
            .get()
            .addOnSuccessListener { result ->

                if (!result.isEmpty) {

                    val doc = result.documents[0]

                    currentCanteenId = doc.id

                    val name = doc.getString("name") ?: ""
                    nameEt.setText(name)
                    collegeEt.setText(doc.getString("college") ?: "")
                    locationEt.setText(doc.getString("location") ?: "")
                    mobileEt.setText(doc.getString("mobile") ?: "")
                    upiIdEt.setText(doc.getString("upiId") ?: "")

                    adminTitleTv.text = name
                    saveBtn.text = "Update Canteen Details"

                    adminControlsLayout.visibility = LinearLayout.VISIBLE

                    loadMenu()

                } else {
                    adminControlsLayout.visibility = LinearLayout.GONE
                    saveBtn.text = "Save Canteen Details"
                    adminTitleTv.text = "Canteen Admin"
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
