package com.mobdeve.s18.verify

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

class Login: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val registerText = findViewById<TextView>(R.id.login_txt_loginQuestion)
        registerText.setOnClickListener {
            val intent = Intent(this, Register::class.java)
            startActivity(intent)
        }

        val forgotText = findViewById<TextView>(R.id.login_text_forgotpw)
        forgotText.setOnClickListener {
            val intent = Intent(this, ForgotPassword::class.java)
            startActivity(intent)
        }


        val emailInput = findViewById<EditText>(R.id.login_txt_email_input)
        val passwordInput = findViewById<EditText>(R.id.login_txt_pw_input)
        val loginButton = findViewById<Button>(R.id.btn_login)

        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            when {
                email == "admin@gmail.com" && password == "admin" -> {
                    val intent = Intent(this, AdminDashboardActivity::class.java)
                    startActivity(intent)
                }
                email == "user@gmail.com" && password == "user" -> {
                    val intent = Intent(this, EmployeeDashboard::class.java)
                    startActivity(intent)
                }
                else -> {
                    Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    @Suppress("MissingSuperCall")
    override fun onBackPressed() {
        val intent = Intent(this, Homepage::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }
}
