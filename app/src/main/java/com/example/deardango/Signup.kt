package com.example.deardango


import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.deardango.databinding.ActivitySignupBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class Signup : AppCompatActivity() {
    private lateinit var binding: ActivitySignupBinding

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        binding.Signup1.setOnClickListener {
            val firstname = binding.firstname.text.toString()
            val lastname = binding.lastname.text.toString()
            val username = binding.username.text.toString()
            val password = binding.password.text.toString()
            val confirmpassword = binding.confirmpassword.text.toString()
            val email = "$username@gmail.com"

            if (firstname.isEmpty() || lastname.isEmpty() || username.isEmpty()
                || password.isEmpty() || confirmpassword.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if (password != confirmpassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            db.collection("users")
                .whereEqualTo("username", username)
                .get()
                .addOnSuccessListener { result ->
                    if (!result.isEmpty) {
                        Toast.makeText(this, "Username already taken", Toast.LENGTH_SHORT).show()
                    } else {
                        createUser(email, password, username, firstname, lastname)
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error checking username: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
        binding.BackButton.setOnClickListener {
            finish()
        }

    }
    private fun createUser(email: String, password: String, username: String, firstName: String, lastName: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { auth ->
                val user_id = auth.user?.uid ?: return@addOnSuccessListener
                val userData = mapOf(
                    "user_id" to user_id,
                    "username" to username,
                    "firstName" to firstName,
                    "lastName" to lastName
                )
                db.collection("users").document(user_id).set(userData)
                    .addOnSuccessListener {
                        // ---- THIS IS THE KEY CHANGE for going back with a result ----
                        val resultIntent = Intent()
                        resultIntent.putExtra("SIGN_UP_SUCCESS", true) // Send a flag
                        setResult(Activity.RESULT_OK, resultIntent)
                        finish() // This will now return to LoginActivity's onActivityResult
                        // Toast is now displayed in LoginActivity
                    }
                    .addOnFailureListener { e -> // Added e for exception
                        Toast.makeText(this, "Failed to save user info: ${e.message}", Toast.LENGTH_SHORT)
                            .show()
                    }
            }
            .addOnFailureListener { e -> // Added e for exception
                Toast.makeText(this, "Signup failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}