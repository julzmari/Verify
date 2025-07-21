package com.mobdeve.s18.verify.controller

import android.app.Activity
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
            when (it.itemId) {
                R.id.nav_home -> {
                    if (currentItemId != R.id.nav_home) {
                        val intent = if (role == "worker") {
                            Intent(this, EmployeeDashboard::class.java)
                        } else {
                            Intent(this, AdminDashboardActivity::class.java)
                        }
                        startActivity(intent)
                    }
                    true
                }

                R.id.nav_history -> {
                    if (currentItemId != R.id.nav_history) {
                        val intent = Intent(this, SubmissionHistory::class.java)
                        intent.putExtra("role", role)
                        startActivity(intent)
                    }
                    true
                }


                R.id.nav_users -> {
                    if (role != "worker" ) {
                        startActivity(Intent(this, ManageUser::class.java))
                    }
                    true
                }

                R.id.nav_settings -> {
                    if (currentItemId != R.id.nav_settings) {
                        val intent = if (role == "worker") {
                            Intent(this, Settings::class.java)
                        } else {
                            Intent(this, AdminSettings::class.java)
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
