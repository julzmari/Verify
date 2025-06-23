package com.mobdeve.s18.verify

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.osmdroid.config.Configuration

class SubmissionHistory : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var userEntries: List<UserEntry>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))

        setContentView(R.layout.activity_submissionhistory)

        recyclerView = findViewById(R.id.submission_history_recyclerView)

        // Dummy entries (replace with real ones later)
        userEntries = listOf(
            UserEntry("user1", "DLSU Manila", "June 14, 2025 12:00PM", 14.5646, 120.9936, "Delivery"),
            UserEntry("user2", "BGC, Taguig", "June 14, 2025 1:20PM", 14.5515, 121.0490, "Delivery")
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = UserEntryAdapter(userEntries) {  }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, EmployeeDashboard::class.java))
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, Settings::class.java))
                    true
                }
                else -> false
            }
        }
    }
}
