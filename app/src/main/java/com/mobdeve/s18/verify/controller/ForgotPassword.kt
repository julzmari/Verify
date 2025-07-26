package com.mobdeve.s18.verify.controller

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mobdeve.s18.verify.R
import com.mobdeve.s18.verify.app.VerifiApp
import com.mobdeve.s18.verify.model.Company
import com.mobdeve.s18.verify.model.User
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import org.mindrot.jbcrypt.BCrypt

class ForgotPassword : AppCompatActivity() {

    private lateinit var emailField: EditText
    private lateinit var newPasswordField: EditText
    private lateinit var confirmPasswordField: EditText
    private lateinit var submitButton: Button
    private lateinit var app: VerifiApp
    private val json = Json { ignoreUnknownKeys = true }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        // View bindings
        emailField = findViewById(R.id.forgot_txt_email_input)
        newPasswordField = findViewById(R.id.et_new_password)
        confirmPasswordField = findViewById(R.id.et_confirm_password)
        submitButton = findViewById(R.id.btn_submit_reset)

        app = applicationContext as VerifiApp

        submitButton.setOnClickListener {
            val email = emailField.text.toString().trim()
            val newPassword = newPasswordField.text.toString().trim()
            val confirmPassword = confirmPasswordField.text.toString().trim()

            if (email.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val supabase = app.supabase

                    // Try to find the user by email
                    val userResult = supabase.postgrest["users"].select {
                        eq("email", email)
                        limit(1)
                    }

                    val users = json.decodeFromString<List<User>>(userResult.body.toString())

                    if (users.isNotEmpty()) {
                        val user = users.first()
                        val hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt())

                        supabase.postgrest["users"].update(mapOf("password" to hashedPassword)) {
                            eq("id", user.id)
                        }

                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@ForgotPassword, "Password updated for user.", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        return@launch
                    }

                    // Try to find the company by email
                    val companyResult = supabase.postgrest["companies"].select {
                        eq("email", email)
                        limit(1)
                    }

                    val companies = json.decodeFromString<List<Company>>(companyResult.body.toString())

                    if (companies.isNotEmpty()) {
                        val company = companies.first()
                        val hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt())

                        supabase.postgrest["companies"].update(mapOf("password" to hashedPassword)) {
                            eq("id", company.id)
                        }

                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@ForgotPassword, "Password updated for owner.", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        return@launch
                    }

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@ForgotPassword, "No account found with that email.", Toast.LENGTH_SHORT).show()
                    }

                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@ForgotPassword, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}
