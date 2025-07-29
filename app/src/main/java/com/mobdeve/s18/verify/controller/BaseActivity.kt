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
    }

    protected fun setupBottomNavigation(bottomNav: BottomNavigationView, currentItemId: Int) {
        bottomNav.selectedItemId = currentItemId
        bottomNav.setOnItemSelectedListener {
            if (role == null) {
                // Fail securely: do nothing if role is undefined
                return@setOnItemSelectedListener false
            }

            when (it.itemId) {
                R.id.nav_home -> {
                    if (currentItemId != R.id.nav_home) {
                        val intent = when (role) {
                            "worker" -> Intent(this, EmployeeDashboard::class.java)
                            "admin", "owner" -> Intent(this, AdminDashboardActivity::class.java)
                            else -> return@setOnItemSelectedListener false // fail secure
                        }
                        startActivity(intent)
                    }
                    true
                }

                R.id.nav_history -> {
                    if (currentItemId != R.id.nav_history) {
                        if (role in listOf("worker", "admin", "owner")) {
                            val intent = Intent(this, SubmissionHistory::class.java)
                            intent.putExtra("role", role)
                            startActivity(intent)
                        } else {
                            return@setOnItemSelectedListener false
                        }
                    }
                    true
                }

                R.id.nav_users -> {
                    if (role != "worker") {
                        startActivity(Intent(this, ManageUser::class.java))
                    } else {
                        return@setOnItemSelectedListener false
                    }
                    true
                }

                R.id.nav_settings -> {
                    if (currentItemId != R.id.nav_settings) {
                        val intent = when (role) {
                            "worker" -> Intent(this, Settings::class.java)
                            "admin", "owner" -> Intent(this, AdminSettings::class.java)
                            else -> return@setOnItemSelectedListener false
                        }
                        startActivity(intent)
                    }
                    true
                }

                else -> false
            }
        }
    }
}