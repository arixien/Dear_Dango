package com.example.deardango

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

class EntryList : AppCompatActivity() {

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    // UI Components
    private lateinit var createEntryCard: LinearLayout
    private lateinit var etTitle: EditText
    private lateinit var etContent: EditText
    private lateinit var dateField: EditText
    private lateinit var btnSave: Button
    private lateinit var btnUpdate: Button

    // Data
    private var selectedDate: Calendar = Calendar.getInstance()
    private var isEditMode = false
    private var editEntryId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_entry_list)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Initialize views
        initializeViews()

        // Check if in edit mode
        checkEditMode()

        // Setup click listeners
        setupClickListeners()

        // Handle edge-to-edge display
        setupEdgeToEdge()
    }

    private fun initializeViews() {
        createEntryCard = findViewById(R.id.createEntryCard)
        etTitle = findViewById(R.id.etTitle)
        etContent = findViewById(R.id.etContent)
        dateField = findViewById(R.id.dateField)
        btnSave = findViewById(R.id.btnSave)
        btnUpdate = findViewById(R.id.btnUpdate)

        // Set current date by default
        setCurrentDate()
    }

    private fun checkEditMode() {
        isEditMode = intent.getBooleanExtra("EDIT_MODE", false)

        if (isEditMode) {
            editEntryId = intent.getStringExtra("ENTRY_ID")
            etTitle.setText(intent.getStringExtra("ENTRY_TITLE") ?: "")
            etContent.setText(intent.getStringExtra("ENTRY_CONTENT") ?: "")
            dateField.setText(intent.getStringExtra("ENTRY_DATE") ?: "")

            // Show update button, hide cancel button and change save to update
            btnSave.text = "UPDATE"
            btnUpdate.visibility = android.view.View.GONE
        }
    }

    private fun setupClickListeners() {
        // Create Entry Card click
        createEntryCard.setOnClickListener {
            clearForm()
        }

        // Save button (or Update in edit mode)
        btnSave.setOnClickListener {
            if (isEditMode) {
                updateEntry()
            } else {
                saveEntry()
            }
        }

        // Cancel button (only visible in create mode)
        btnUpdate.setOnClickListener {
            cancelEntry()
        }

        // Date field click - open date picker
        dateField.setOnClickListener {
            showDatePicker()
        }
    }

    private fun cancelEntry() {
        // Show confirmation dialog if user has entered any data
        val title = etTitle.text.toString().trim()
        val content = etContent.text.toString().trim()

        if (title.isNotEmpty() || content.isNotEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Discard Changes?")
                .setMessage("You have unsaved changes. Are you sure you want to cancel?")
                .setPositiveButton("Discard") { _, _ ->
                    finish() // Go back to previous activity
                }
                .setNegativeButton("Keep Editing", null)
                .show()
        } else {
            // No data entered, just go back
            finish()
        }
    }

    private fun showDatePicker() {
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                selectedDate.set(year, month, dayOfMonth)
                updateDateField()
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun updateDateField() {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        dateField.setText(dateFormat.format(selectedDate.time))
    }

    private fun clearForm() {
        etTitle.setText("")
        etContent.setText("")
        setCurrentDate()
        etTitle.requestFocus()
    }

    private fun saveEntry() {
        val title = etTitle.text.toString().trim()
        val content = etContent.text.toString().trim()
        val date = dateField.text.toString().trim()

        if (!validateInput(title, content, date)) {
            return
        }

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please log in again", Toast.LENGTH_SHORT).show()
            return
        }

        // Create diary entry data
        val diaryEntry = hashMapOf(
            "title" to title,
            "content" to content,
            "date" to date,
            "userId" to currentUser.uid,
            "timestamp" to Timestamp.now()
        )

        // Save to Firestore
        firestore.collection("diary_entries")
            .add(diaryEntry)
            .addOnSuccessListener { documentReference ->
                Log.d("EntryList", "DocumentSnapshot added with ID: ${documentReference.id}")
                Toast.makeText(this, "Entry saved successfully!", Toast.LENGTH_SHORT).show()

                // Navigate to view the entry
                val intent = Intent(this, ViewEntry::class.java)
                intent.putExtra("ENTRY_ID", documentReference.id)
                intent.putExtra("ENTRY_TITLE", title)
                intent.putExtra("ENTRY_CONTENT", content)
                intent.putExtra("ENTRY_DATE", date)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                Log.w("EntryList", "Error adding document", e)
                Toast.makeText(this, "Failed to save entry. Please try again.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateEntry() {
        val title = etTitle.text.toString().trim()
        val content = etContent.text.toString().trim()
        val date = dateField.text.toString().trim()

        if (!validateInput(title, content, date)) {
            return
        }

        if (editEntryId == null) {
            Toast.makeText(this, "Entry ID not found", Toast.LENGTH_SHORT).show()
            return
        }

        // Update diary entry data
        val updates = hashMapOf<String, Any>(
            "title" to title,
            "content" to content,
            "date" to date,
            "timestamp" to Timestamp.now() // Update timestamp when modified
        )

        // Update in Firestore
        firestore.collection("diary_entries")
            .document(editEntryId!!)
            .update(updates)
            .addOnSuccessListener {
                Log.d("EntryList", "DocumentSnapshot successfully updated!")
                Toast.makeText(this, "Entry updated successfully!", Toast.LENGTH_SHORT).show()

                // Navigate back to view the entry
                val intent = Intent(this, ViewEntry::class.java)
                intent.putExtra("ENTRY_ID", editEntryId)
                intent.putExtra("ENTRY_TITLE", title)
                intent.putExtra("ENTRY_CONTENT", content)
                intent.putExtra("ENTRY_DATE", date)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                Log.w("EntryList", "Error updating document", e)
                Toast.makeText(this, "Failed to update entry. Please try again.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun validateInput(title: String, content: String, date: String): Boolean {
        if (title.isEmpty()) {
            etTitle.error = "Please enter a title"
            etTitle.requestFocus()
            return false
        }

        if (content.isEmpty()) {
            etContent.error = "Please enter some content"
            etContent.requestFocus()
            return false
        }

        if (date.isEmpty()) {
            dateField.error = "Please select a date"
            return false
        }

        return true
    }

    private fun setCurrentDate() {
        selectedDate = Calendar.getInstance()
        updateDateField()
    }

    private fun setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}