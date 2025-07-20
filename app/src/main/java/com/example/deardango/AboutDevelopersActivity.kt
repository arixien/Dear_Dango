package com.example.deardango

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class AboutDevelopersActivity : AppCompatActivity() {

    private lateinit var dev1Image: ImageView
    private lateinit var dev2Image: ImageView
    private lateinit var dev3Image: ImageView
    private lateinit var dev4Image: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about_developers)

        // Initialize views
        initializeViews()

        // Set developer images
        setDeveloperImages( )
    }

    private fun initializeViews() {
        dev1Image = findViewById(R.id.dev1_image)
        dev2Image = findViewById(R.id.dev2_image)
        dev3Image = findViewById(R.id.dev3_image)
        dev4Image = findViewById(R.id.dev4_image)
    }

    private fun setDeveloperImages() {
        // Set your actual images here
        dev1Image.setImageResource(R.drawable.christabelle_photo)
        dev2Image.setImageResource(R.drawable.rantzel_photo)
        dev3Image.setImageResource(R.drawable.richielle_photo)
        dev4Image.setImageResource(R.drawable.cristine_photo)
    }
}