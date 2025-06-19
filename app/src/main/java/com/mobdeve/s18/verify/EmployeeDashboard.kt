package com.mobdeve.s18.verify

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class EmployeeDashboard : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_employeedashboard)

        val welcomeText = findViewById<TextView>(R.id.welcomeText)
        val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val username = sharedPref.getString("loggedInUser", "Guest")
        welcomeText.text = "Welcome, $username!"

        // Handle camera icon click
        val cameraIcon = findViewById<ImageView>(R.id.camIcon)
        cameraIcon.setOnClickListener {
            // TODO: Replace with actual activity
            // val intent = Intent(this, UserCamera::class.java)
            // startActivity(intent)
        }

        // Bottom Navigation setup
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    true
                }
                R.id.nav_history -> {
                    // TODO: Replace with actual activity
                    // startActivity(Intent(this, HistoryActivity::class.java))
                    true
                }
                R.id.nav_settings -> {
                    // TODO: Replace with actual activity
                    startActivity(Intent(this, Settings::class.java))
                    true
                }
                else -> false
            }
        }
    }
}

