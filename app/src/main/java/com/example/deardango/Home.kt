package com.example.deardango

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.*

class Home : AppCompatActivity() {

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    // UI Components
    private lateinit var emptyStateMessage: TextView
    private lateinit var recyclerViewDiaryEntries: RecyclerView
    private lateinit var fabAddEntry: FloatingActionButton
    private lateinit var btnInfo: ImageButton
    private lateinit var btnExit: ImageButton

    // RecyclerView
    private lateinit var diaryAdapter: DiaryEntryAdapter
    private val diaryEntries = mutableListOf<DiaryEntry>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Initialize views
        initializeViews()

        // Setup RecyclerView
        setupRecyclerView()

        // Setup click listeners
        setupClickListeners()

        // Load diary entries from Firebase
        loadDiaryEntriesFromFirebase()

        // Handle edge-to-edge display
        setupEdgeToEdge()
    }

    override fun onResume() {
        super.onResume()
        // Refresh diary entries when returning to this activity
        Log.d("Home", "onResume() - Refreshing diary entries")
        loadDiaryEntriesFromFirebase()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // Handle when activity is brought to front with SINGLE_TOP flag
        Log.d("Home", "onNewIntent() - Refreshing diary entries")
        loadDiaryEntriesFromFirebase()
    }

    private fun initializeViews() {
        // Empty state and RecyclerView
        emptyStateMessage = findViewById(R.id.emptyStateMessage)
        recyclerViewDiaryEntries = findViewById(R.id.recyclerViewDiaryEntries)

        // Buttons
        fabAddEntry = findViewById(R.id.fabAddEntry)
        btnInfo = findViewById(R.id.btnInfo)
        btnExit = findViewById(R.id.btnExit)
    }

    private fun setupRecyclerView() {
        diaryAdapter = DiaryEntryAdapter(diaryEntries) { entry ->
            openDiaryEntry(entry)
        }

        recyclerViewDiaryEntries.apply {
            layoutManager = LinearLayoutManager(this@Home)
            adapter = diaryAdapter
        }
    }

    private fun setupClickListeners() {
        // Add new diary entry
        fabAddEntry.setOnClickListener {
            val intent = Intent(this, EntryList::class.java)
            startActivity(intent)
        }

        // Info button - navigate to About Developers
        btnInfo.setOnClickListener {
            val intent = Intent(this, AboutDevelopersActivity::class.java)
            startActivity(intent)

            // Temporary: Manual refresh for debugging
            Log.d("Home", "DEBUG: Manual refresh triggered via Info button")
            loadDiaryEntriesFromFirebase()
        }

        // Exit button
        btnExit.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, Login::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun loadDiaryEntriesFromFirebase() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.d("Home", "No authenticated user found")
            // User not logged in, redirect to login
            startActivity(Intent(this, Login::class.java))
            finish()
            return
        }

        Log.d("Home", "Loading diary entries for user: ${currentUser.uid}")

        // Show loading state (optional - you can add a progress indicator)
        Log.d("Home", "Starting Firestore query...")

        firestore.collection("diary_entries")
            .whereEqualTo("userId", currentUser.uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            // Remove the limit(3) to load ALL entries
            .get()
            .addOnSuccessListener { documents ->
                Log.d("Home", "Firestore query successful - Found ${documents.size()} documents")

                Toast.makeText(this, "Loaded ${documents.size()} entries", Toast.LENGTH_SHORT)
                    .show()

                val newEntries = mutableListOf<DiaryEntry>()

                if (documents.isEmpty) {
                    Log.d("Home", "No diary entries found for user ${currentUser.uid}")
                    updateUI(newEntries)
                    return@addOnSuccessListener
                }

                for (document in documents) {
                    Log.d("Home", "Processing document: ${document.id} data: ${document.data}")

                    val entry = DiaryEntry(
                        id = document.id,
                        title = document.getString("title") ?: "Untitled",
                        content = document.getString("content") ?: "No content",
                        date = document.getString("date") ?: "No date",
                        timestamp = document.getTimestamp("timestamp")?.toDate() ?: Date(),
                        userId = document.getString("userId") ?: ""
                    )

                    Log.d("Home", "Created entry: ${entry.title} - ${entry.date}")
                    newEntries.add(entry)
                }

                Log.d("Home", "Total entries loaded: ${newEntries.size}")
                updateUI(newEntries)
            }
            .addOnFailureListener { exception ->
                Log.w("Home", "Error getting documents: ", exception)
                Toast.makeText(this, "Failed to load diary entries", Toast.LENGTH_SHORT).show()
                showEmptyState()
            }
    }

    private fun updateUI(entries: List<DiaryEntry>) {
        Log.d("Home", "updateUI() called with ${entries.size} entries")
        if (entries.isEmpty()) {
            Log.d("Home", "Showing empty state")
            showEmptyState()
        } else {
            Log.d("Home", "Showing diary entries")
            showDiaryEntries(entries)
        }
    }

    private fun showEmptyState() {
        emptyStateMessage.setText(R.string.start_diary)
        emptyStateMessage.visibility = View.VISIBLE
        recyclerViewDiaryEntries.visibility = View.GONE
    }

    private fun showDiaryEntries(entries: List<DiaryEntry>) {
        emptyStateMessage.visibility = View.GONE
        recyclerViewDiaryEntries.visibility = View.VISIBLE
        diaryAdapter.updateEntries(entries)
    }

    private fun openDiaryEntry(entry: DiaryEntry) {
        val intent = Intent(this, ViewEntry::class.java)
        intent.putExtra("ENTRY_ID", entry.id)
        intent.putExtra("ENTRY_TITLE", entry.title)
        intent.putExtra("ENTRY_CONTENT", entry.content)
        intent.putExtra("ENTRY_DATE", entry.date)
        startActivity(intent)
    }

    private fun setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}