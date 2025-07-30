package com.mobdeve.s18.verify.controller

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.mobdeve.s18.verify.R
import com.mobdeve.s18.verify.app.VerifiApp

open class BaseActivity : AppCompatActivity() {

    protected lateinit var app: VerifiApp
    protected var role: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app = applicationContext as VerifiApp
        role = app.authorizedRole

        if (role == null) {
            startActivity(Intent(this, Login::class.java))
            finish()
            return
        }

    }

    protected fun setupBottomNavigation(bottomNav: BottomNavigationView, currentItemId: Int) {
        bottomNav.selectedItemId = currentItemId
        bottomNav.setOnItemSelectedListener {

            val intent = when (it.itemId) {
                R.id.nav_home -> {
                    if (currentItemId == R.id.nav_home) return@setOnItemSelectedListener true
                    when (role) {
                        "worker" -> Intent(this, EmployeeDashboard::class.java)
                        "admin", "owner" -> Intent(this, AdminDashboardActivity::class.java)
                        else -> return@setOnItemSelectedListener false
                    }
                }

                R.id.nav_history -> {
                    if (currentItemId == R.id.nav_history) return@setOnItemSelectedListener true
                    if (role in listOf("worker", "admin", "owner")) {
                        Intent(this, SubmissionHistory::class.java).putExtra("role", role)
                    } else return@setOnItemSelectedListener false
                }

                R.id.nav_users -> {
                    if (role != "worker") {
                        Intent(this, ManageUser::class.java)
                    } else return@setOnItemSelectedListener false
                }

                R.id.nav_settings -> {
                    if (currentItemId == R.id.nav_settings) return@setOnItemSelectedListener true
                    when (role) {
                        "worker" -> Intent(this, Settings::class.java)
                        "admin", "owner" -> Intent(this, AdminSettings::class.java)
                        else -> return@setOnItemSelectedListener false
                    }
                }

                else -> return@setOnItemSelectedListener false
            }

            // Prevent animation + clear top stack
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)

            true
        }
    }
}