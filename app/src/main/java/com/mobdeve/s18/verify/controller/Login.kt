package com.mobdeve.s18.verify.controller

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
import android.util.Log
import com.mobdeve.s18.verify.R
import com.mobdeve.s18.verify.app.VerifiApp
import com.mobdeve.s18.verify.model.Company
import com.mobdeve.s18.verify.model.User

import android.widget.CheckBox 
import kotlinx.serialization.json.Json
import android.widget.AutoCompleteTextView
import android.widget.ArrayAdapter
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import kotlinx.serialization.encodeToString
import kotlinx.serialization.Serializable

@Serializable
data class RememberedAccount(val email: String, val password: String)

import kotlinx.serialization.json.Json

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
        rememberMeCheckBox.isChecked = false // Always reset

        // Load saved accounts
        val sharedPrefs = getSharedPreferences("loginPrefs", MODE_PRIVATE)
        val savedJson = sharedPrefs.getString("rememberedAccounts", "[]")
        val rememberedAccounts = Json.decodeFromString<List<RememberedAccount>>(savedJson ?: "[]")

        val savedEmails = rememberedAccounts.map { it.email }

        // Set dropdown suggestions
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, savedEmails)
        (emailEditText as AutoCompleteTextView).setAdapter(adapter)

        // Autofill password on email selection
        (emailEditText as AutoCompleteTextView).setOnItemClickListener { _, _, position, _ ->
            val selectedEmail = adapter.getItem(position)
            val matched = rememberedAccounts.find { it.email == selectedEmail }
            passwordEditText.setText(matched?.password ?: "")
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

        Log.d("LOGIN_DEBUG", "Starting login query...")

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Try logging in as company (owner)
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
                    app.companyID = company.id.toString()
                    app.authorizedRole = "owner"

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@Login, "Logged in as company owner", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@Login, AdminDashboardActivity::class.java))
                        finish()
                    }
                    return@launch
                }

                // Try logging in as user (admin or worker)
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

                    if (BCrypt.checkpw(password, user.password)) {
                        val app = applicationContext as VerifiApp

                        app.companyID = user.companyID.toString()
                        app.employeeID = user.id.toString()

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
                    }
                }


                // If neither login was successful
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

        val sharedPreferences = getSharedPreferences("loginPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        if (rememberMeCheckBox.isChecked) {
            val sharedPrefs = getSharedPreferences("loginPrefs", MODE_PRIVATE)
            val savedJson = sharedPrefs.getString("rememberedAccounts", "[]")
            val accounts = Json.decodeFromString<MutableList<RememberedAccount>>(savedJson ?: "[]")

            if (accounts.none { it.email == email }) {
                accounts.add(RememberedAccount(email, password))
                sharedPrefs.edit().putString("rememberedAccounts", Json.encodeToString(accounts)).apply()
            }
        }
    }
}
