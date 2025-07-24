package com.mobdeve.s18.verify.controller

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.mobdeve.s18.verify.R
import com.mobdeve.s18.verify.app.VerifiApp
import com.mobdeve.s18.verify.model.Company
import com.mobdeve.s18.verify.model.User
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.mindrot.jbcrypt.BCrypt

@Serializable
data class RememberedAccount(val email: String, val password: String)

class Login : AppCompatActivity() {

    private lateinit var emailEditText: MaterialAutoCompleteTextView
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var rememberMeCheckBox: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        emailEditText = findViewById(R.id.login_txt_email_input)
        passwordEditText = findViewById(R.id.login_txt_pw_input)
        loginButton = findViewById(R.id.btn_login)
        rememberMeCheckBox = findViewById(R.id.login_checkBox)
        rememberMeCheckBox.isChecked = false

        // Load remembered accounts
        val sharedPrefs = getSharedPreferences("loginPrefs", MODE_PRIVATE)
        val savedJson = sharedPrefs.getString("rememberedAccounts", "[]")
        val rememberedAccounts = Json.decodeFromString<List<RememberedAccount>>(savedJson ?: "[]")
        val savedEmails = rememberedAccounts.map { it.email }

        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, savedEmails)
        (emailEditText as AutoCompleteTextView).setAdapter(adapter)

        (emailEditText as AutoCompleteTextView).setOnItemClickListener { _, _, position, _ ->
            val selectedEmail = adapter.getItem(position)
            val matched = rememberedAccounts.find { it.email == selectedEmail }
            passwordEditText.setText(matched?.password ?: "")
        }

        findViewById<TextView>(R.id.login_txt_loginQuestion).setOnClickListener {
            startActivity(Intent(this, Register::class.java))
        }

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim().lowercase()
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
        val json = Json { ignoreUnknownKeys = true }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Try logging in as company
                val companyResult = supabase.postgrest
                    .from("companies")
                    .select {
                        eq("email", email)
                        limit(1)
                    }

                val companies = json.decodeFromString<List<Company>>(companyResult.body.toString())
                val company = companies.firstOrNull()

                if (company != null && BCrypt.checkpw(password, company.password)) {
                    val app = applicationContext as VerifiApp
                    app.companyID = company.id
                    app.authorizedRole = "owner"

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@Login, "Logged in as company owner", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@Login, AdminDashboardActivity::class.java))
                        finish()
                    }
                    return@launch
                }

                // Try logging in as user
                val userResult = supabase.postgrest
                    .from("users")
                    .select {
                        eq("email", email)
                        limit(1)
                    }

                val users = json.decodeFromString<List<User>>(userResult.body.toString())
                val user = users.firstOrNull()

                if (user != null) {
                    if (!user.isActive) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@Login, "Account is deactivated", Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }

                    // Track attempts locally using SharedPreferences
                    val prefs = getSharedPreferences("loginPrefs", MODE_PRIVATE)
                    val attemptKey = "attempts_${user.email}"
                    val currentAttempts = prefs.getInt(attemptKey, 0)

                    if (BCrypt.checkpw(password, user.password)) {
                        // Reset attempts on success
                        prefs.edit().remove(attemptKey).apply()

                        val app = applicationContext as VerifiApp
                        app.companyID = user.companyID
                        app.employeeID = user.id

                        when (user.role) {
                            "admin" -> {
                                app.authorizedRole = "admin"
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(this@Login, "Logged in as admin", Toast.LENGTH_SHORT).show()
                                    startActivity(Intent(this@Login, AdminDashboardActivity::class.java))
                                    finish()
                                }
                            }
                            "reg_employee" -> {
                                app.authorizedRole = "worker"
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(this@Login, "Logged in as worker", Toast.LENGTH_SHORT).show()
                                    startActivity(Intent(this@Login, EmployeeDashboard::class.java))
                                    finish()
                                }
                            }
                            else -> {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(this@Login, "Unauthorized role", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        return@launch

                    } else {
                        // Incorrect password, increment attempt
                        val newAttempts = currentAttempts + 1

                        if (newAttempts >= 5) {
                            // Deactivate user in DB
                            supabase.postgrest.from("users").update(
                                mapOf("isActive" to false)
                            ) {
                                eq("id", user.id)
                            }
                            prefs.edit().remove(attemptKey).apply()

                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@Login, "Account is deactivated", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            prefs.edit().putInt(attemptKey, newAttempts).apply()

                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    this@Login,
                                    "Invalid email or password. Attempts left: ${5 - newAttempts}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        return@launch
                    }
                }

                // No match found
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@Login, "Invalid email or password", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Log.e("LOGIN_DEBUG", "Login error: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@Login, "Login error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        // Remember me
        val sharedPreferences = getSharedPreferences("loginPrefs", MODE_PRIVATE)
        if (rememberMeCheckBox.isChecked) {
            val savedJson = sharedPreferences.getString("rememberedAccounts", "[]")
            val accounts = Json.decodeFromString<MutableList<RememberedAccount>>(savedJson ?: "[]")

            if (accounts.none { it.email == email }) {
                accounts.add(RememberedAccount(email, password))
                sharedPreferences.edit().putString("rememberedAccounts", Json.encodeToString(accounts)).apply()
            }
        }
    }
}
