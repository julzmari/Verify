package com.mobdeve.s18.verify

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class AddUser : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_user)

        val usernameInput = findViewById<EditText>(R.id.Username)
        val emailInput = findViewById<EditText>(R.id.Email)
        val passwordInput = findViewById<EditText>(R.id.Pass)
        val confirmPasswordInput = findViewById<EditText>(R.id.confirmPass)
        val addUserButton = findViewById<Button>(R.id.addUser)
        val returnButton = findViewById<TextView>(R.id.returnText)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav2)

        returnButton.setOnClickListener {
            startActivity(Intent(this, ManageUser::class.java))
        }

        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_home -> {
                    //startActivity(Intent(this, AdminDashboard::class.java))
                    true
                }
                R.id.nav_history -> {
                   // startActivity(Intent(this, History::class.java))
                    true
                }
                R.id.nav_users -> {
                    startActivity(Intent(this, ManageUser::class.java))
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, AdminSettings::class.java))
                    true
                }
                else -> false
            }
        }

        addUserButton.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString()
            val confirmPassword = confirmPasswordInput.text.toString()

            if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val newUser = User(username, email, isActive = true)

            val resultIntent = Intent().apply {
                putExtra("newUser", newUser)
            }

            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }
}

