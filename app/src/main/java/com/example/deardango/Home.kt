package com.example.deardango

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class Home : AppCompatActivity() {

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    // UI Components
    private lateinit var emptyStateMessage: TextView
    private lateinit var diaryEntry1: LinearLayout
    private lateinit var diaryEntry2: LinearLayout
    private lateinit var diaryEntry3: LinearLayout
    private lateinit var tvTitle1: TextView
    private lateinit var tvDate1: TextView
    private lateinit var tvTitle2: TextView
    private lateinit var tvDate2: TextView
    private lateinit var tvTitle3: TextView
    private lateinit var tvDate3: TextView
    private lateinit var fabAddEntry: FloatingActionButton
    private lateinit var btnInfo: ImageButton
    private lateinit var btnExit: ImageButton

    // Data
    private val diaryEntries = mutableListOf<DiaryEntry>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Initialize views
        initializeViews()

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
        // Empty state and diary entries
        emptyStateMessage = findViewById(R.id.emptyStateMessage)
        diaryEntry1 = findViewById(R.id.diaryEntry1)
        diaryEntry2 = findViewById(R.id.diaryEntry2)
        diaryEntry3 = findViewById(R.id.diaryEntry3)

        // Text views for diary content
        tvTitle1 = findViewById(R.id.tvTitle1)
        tvDate1 = findViewById(R.id.tvDate1)
        tvTitle2 = findViewById(R.id.tvTitle2)
        tvDate2 = findViewById(R.id.tvDate2)
        tvTitle3 = findViewById(R.id.tvTitle3)
        tvDate3 = findViewById(R.id.tvDate3)

        // Buttons
        fabAddEntry = findViewById(R.id.fabAddEntry)
        btnInfo = findViewById(R.id.btnInfo)
        btnExit = findViewById(R.id.btnExit)
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

        // Diary entry click listeners
        diaryEntry1.setOnClickListener {
            if (diaryEntries.size > 0) {
                openDiaryEntry(diaryEntries[0])
            }
        }

        diaryEntry2.setOnClickListener {
            if (diaryEntries.size > 1) {
                openDiaryEntry(diaryEntries[1])
            }
        }

        diaryEntry3.setOnClickListener {
            if (diaryEntries.size > 2) {
                openDiaryEntry(diaryEntries[2])
            }
        }
    }

    private fun loadDiaryEntriesFromFirebase() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // User not logged in, redirect to login
            startActivity(Intent(this, Login::class.java))
            finish()
            return
        }

        Log.d("Home", "Loading diary entries for user: ${currentUser.uid}")

        firestore.collection("diary_entries")
            .whereEqualTo("userId", currentUser.uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(3)
            .get()
            .addOnSuccessListener { documents ->
                Log.d("Home", "Successfully loaded ${documents.size()} diary entries")
                diaryEntries.clear()

                for (document in documents) {
                    val entry = DiaryEntry(
                        id = document.id,
                        title = document.getString("title") ?: "",
                        content = document.getString("content") ?: "",
                        date = document.getString("date") ?: "",
                        timestamp = document.getTimestamp("timestamp")?.toDate() ?: Date(),
                        userId = document.getString("userId") ?: ""
                    )
                    diaryEntries.add(entry)
                }

                updateUI()
            }
            .addOnFailureListener { exception ->
                Log.w("Home", "Error getting documents: ", exception)
                showEmptyState()
            }
    }

    private fun updateUI() {
        Log.d("Home", "updateUI() called with ${diaryEntries.size} entries")
        if (diaryEntries.isEmpty()) {
            Log.d("Home", "Showing empty state")
            showEmptyState()
        } else {
            Log.d("Home", "Showing diary entries")
            showDiaryEntries()
        }
    }

    private fun showEmptyState() {
        emptyStateMessage.visibility = View.VISIBLE
        diaryEntry1.visibility = View.GONE
        diaryEntry2.visibility = View.GONE
        diaryEntry3.visibility = View.GONE
    }

    private fun showDiaryEntries() {
        emptyStateMessage.visibility = View.GONE

        // Show entry 1
        if (diaryEntries.size > 0) {
            diaryEntry1.visibility = View.VISIBLE
            tvTitle1.text = diaryEntries[0].title
            tvDate1.text = formatDate(diaryEntries[0].timestamp)
        } else {
            diaryEntry1.visibility = View.GONE
        }

        // Show entry 2
        if (diaryEntries.size > 1) {
            diaryEntry2.visibility = View.VISIBLE
            tvTitle2.text = diaryEntries[1].title
            tvDate2.text = formatDate(diaryEntries[1].timestamp)
        } else {
            diaryEntry2.visibility = View.GONE
        }

        // Show entry 3
        if (diaryEntries.size > 2) {
            diaryEntry3.visibility = View.VISIBLE
            tvTitle3.text = diaryEntries[2].title
            tvDate3.text = formatDate(diaryEntries[2].timestamp)
        } else {
            diaryEntry3.visibility = View.GONE
        }
    }

    private fun openDiaryEntry(entry: DiaryEntry) {
        val intent = Intent(this, ViewEntry::class.java)
        intent.putExtra("ENTRY_ID", entry.id)
        intent.putExtra("ENTRY_TITLE", entry.title)
        intent.putExtra("ENTRY_CONTENT", entry.content)
        intent.putExtra("ENTRY_DATE", entry.date)
        startActivity(intent)
    }

    private fun formatDate(date: Date): String {
        val calendar = Calendar.getInstance()
        val today = calendar.time
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = calendar.time

        return when {
            isSameDay(date, today) -> "Today"
            isSameDay(date, yesterday) -> "Yesterday"
            else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(date)
        }
    }

    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}