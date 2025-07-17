package com.mobdeve.s18.verify

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.github.jan.supabase.gotrue.GoTrue
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.launch

class Login : AppCompatActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        emailEditText = findViewById(R.id.login_txt_email_input)
        passwordEditText = findViewById(R.id.login_txt_pw_input)
        loginButton = findViewById(R.id.btn_login)

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (email.isBlank() || password.isBlank()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            } else {
                loginUser(email, password)
            }
        }
    }

    private fun loginUser(email: String, password: String) {
        val supabase = (application as VerifiApp).supabase
        val auth = supabase.pluginManager.getPlugin(GoTrue)

        lifecycleScope.launch {
            try {
                val session = auth.loginWith(Email) {
                    this.email = email
                    this.password = password
                }
                Toast.makeText(this@Login, "Login successful", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@Login, EmployeeDashboard::class.java))
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@Login, "Login failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
