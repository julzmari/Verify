package com.mobdeve.s18.verify

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mindrot.jbcrypt.BCrypt
import kotlinx.serialization.Serializable
import android.util.Log
import kotlinx.coroutines.launch

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

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
        Log.d("LOGIN_DEBUG", "Starting login query...")

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val results = supabase.postgrest
                    .from("companies")
                    .select {
                        eq("email", email)
                        limit(1)
                    }
                val json = Json { ignoreUnknownKeys = true }
                val companies = json.decodeFromString<List<Company>>(results.body.toString())



                val company = companies.firstOrNull()

                if (company != null) {
                    Log.d("LOGIN_DEBUG", "Company found: ${company.email}")
                    Log.d("LOGIN_DEBUG", "Retrieved hash: ${company.password}")
                    Log.d("LOGIN_DEBUG", "Hash length: ${company.password.length}")

                    try {
                        val match = BCrypt.checkpw(password, company.password)
                        Log.d("LOGIN_DEBUG", "Password match result: $match")

                        if (match) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@Login, "Login successful", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this@Login, EmployeeDashboard::class.java))
                                finish()
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@Login, "Incorrect password", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("LOGIN_DEBUG", "BCrypt error: ${e.message}")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@Login, "Invalid password hash format.", Toast.LENGTH_SHORT).show()
                        }
                    }

                } else {
                    Log.d("LOGIN_DEBUG", "No company found for email: $email")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@Login, "No company found", Toast.LENGTH_SHORT).show()
                    }
                }

            } catch (e: Exception) {
                Log.e("LOGIN_DEBUG", "Login error: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@Login, "Login error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    @Serializable
    data class Company(
        val id: String,
        val name: String,
        val email: String,
        val password: String
    )
}
