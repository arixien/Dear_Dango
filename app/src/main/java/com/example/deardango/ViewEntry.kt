package com.example.deardango

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ViewEntry : AppCompatActivity() {

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    // UI Components
    private lateinit var viewEntryCard: LinearLayout
    private lateinit var tvTitleDisplay: TextView
    private lateinit var tvContentDisplay: TextView
    private lateinit var dateDisplay: TextView
    private lateinit var btnBackHome: Button
    private lateinit var btnEdit: Button
    private lateinit var btnDelete: Button

    // Entry data
    private var entryId: String = ""
    private var entryTitle: String = ""
    private var entryContent: String = ""
    private var entryDate: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_view_entry)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Initialize views
        initializeViews()

        // Load entry data
        loadEntryData()

        // Setup click listeners
        setupClickListeners()

        // Handle edge-to-edge display
        setupEdgeToEdge()
    }

    private fun initializeViews() {
        viewEntryCard = findViewById(R.id.viewEntryCard)
        tvTitleDisplay = findViewById(R.id.tvTitleDisplay)
        tvContentDisplay = findViewById(R.id.tvContentDisplay)
        dateDisplay = findViewById(R.id.dateDisplay)
        btnBackHome = findViewById(R.id.btnBackHome)
        btnEdit = findViewById(R.id.btnEdit)
        btnDelete = findViewById(R.id.btnDelete)
    }

    private fun loadEntryData() {
        // Get data passed from previous activity
        entryId = intent.getStringExtra("ENTRY_ID") ?: ""
        entryTitle = intent.getStringExtra("ENTRY_TITLE") ?: "No Title"
        entryContent = intent.getStringExtra("ENTRY_CONTENT") ?: "No Content"
        entryDate = intent.getStringExtra("ENTRY_DATE") ?: "No Date"

        // Display the data
        displayEntryData()

        // If we have entry ID, load fresh data from Firebase
        if (entryId.isNotEmpty()) {
            loadEntryFromFirebase()
        }
    }

    private fun loadEntryFromFirebase() {
        firestore.collection("diary_entries")
            .document(entryId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    entryTitle = document.getString("title") ?: entryTitle
                    entryContent = document.getString("content") ?: entryContent
                    entryDate = document.getString("date") ?: entryDate

                    displayEntryData()
                } else {
                    Log.d("ViewEntry", "No such document")
                    Toast.makeText(this, "Entry not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Log.d("ViewEntry", "get failed with ", exception)
                Toast.makeText(this, "Failed to load entry", Toast.LENGTH_SHORT).show()
            }
    }

    private fun displayEntryData() {
        tvTitleDisplay.text = entryTitle
        tvContentDisplay.text = entryContent
        dateDisplay.text = "Date: $entryDate"
    }

    private fun setupClickListeners() {
        // View Entry Card click
        viewEntryCard.setOnClickListener {
            // Could navigate back to entry list or do nothing
        }

        // Back to Home button
        btnBackHome.setOnClickListener {
            navigateToHome()
        }

        // Edit button
        btnEdit.setOnClickListener {
            editEntry()
        }

        // Delete button
        btnDelete.setOnClickListener {
            showDeleteConfirmation()
        }
    }

    private fun navigateToHome() {
        val intent = Intent(this, Home::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }

    private fun editEntry() {
        // Navigate to EntryList with data for editing
        val intent = Intent(this, EntryList::class.java)
        intent.putExtra("EDIT_MODE", true)
        intent.putExtra("ENTRY_ID", entryId)
        intent.putExtra("ENTRY_TITLE", entryTitle)
        intent.putExtra("ENTRY_CONTENT", entryContent)
        intent.putExtra("ENTRY_DATE", entryDate)
        startActivity(intent)
        finish()
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Delete Entry")
            .setMessage("Are you sure you want to delete this diary entry? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteEntry()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteEntry() {
        if (entryId.isEmpty()) {
            Toast.makeText(this, "Cannot delete entry: ID not found", Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading state
        btnDelete.isEnabled = false
        btnDelete.text = "Deleting..."

        // Delete from Firestore
        firestore.collection("diary_entries")
            .document(entryId)
            .delete()
            .addOnSuccessListener {
                Log.d("ViewEntry", "DocumentSnapshot successfully deleted!")
                Toast.makeText(this, "Entry deleted successfully", Toast.LENGTH_SHORT).show()

                // Navigate back to home and refresh
                navigateToHome()
            }
            .addOnFailureListener { e ->
                Log.w("ViewEntry", "Error deleting document", e)
                Toast.makeText(this, "Failed to delete entry. Please try again.", Toast.LENGTH_SHORT).show()

                // Reset button state
                btnDelete.isEnabled = true
                btnDelete.text = "DELETE"
            }
    }

    private fun setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}