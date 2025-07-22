package com.mobdeve.s18.verify.controller

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mobdeve.s18.verify.R
import com.mobdeve.s18.verify.app.VerifiApp
import kotlinx.serialization.json.Json
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mindrot.jbcrypt.BCrypt
import androidx.lifecycle.lifecycleScope
import com.mobdeve.s18.verify.model.Company
import com.mobdeve.s18.verify.model.User
import kotlinx.coroutines.*






class ChangePassword : AppCompatActivity() {

    protected lateinit var app: VerifiApp
    protected var role: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_changepassword)

        val currentPassword = findViewById<EditText>(R.id.currentPassword)
        val newPassword = findViewById<EditText>(R.id.newPassword)
        val confirmPassword = findViewById<EditText>(R.id.confirmPassword)
        val submitButton = findViewById<Button>(R.id.submitPassword)
        val discardButton = findViewById<Button>(R.id.discardPassword)
        val json = Json { ignoreUnknownKeys = true }




        submitButton.setOnClickListener {
            val current = currentPassword.text.toString()
            val new = newPassword.text.toString()
            val confirm = confirmPassword.text.toString()

            if (current.isEmpty() || new.isEmpty() || confirm.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (new != confirm) {
                Toast.makeText(this, "New passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }


            CoroutineScope(Dispatchers.IO).launch {
                try {
                    app = applicationContext as VerifiApp
                    role = app.authorizedRole

                    val supabase = (application as VerifiApp).supabase
                    val idToCheck: String

                    if (role == "owner") {
                        idToCheck = app.companyID ?: return@launch

                        val companyResult = supabase.postgrest
                            .from("companies")
                            .select {
                                eq("id", idToCheck)
                                limit(1)
                            }

                        val companies = json.decodeFromString<List<Company>>(companyResult.body.toString())
                        val company = companies.firstOrNull() ?: return@launch
                        val currentHashedPassword = company.password

                        // Check if current password does NOT match
                        if (!BCrypt.checkpw(current, currentHashedPassword)) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    this@ChangePassword,
                                    "Current password is incorrect.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            return@launch
                        }

                        // Check if new password is same as current
                        if (BCrypt.checkpw(new, currentHashedPassword)) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    this@ChangePassword,
                                    "New password must be different from current password.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            return@launch
                        }

                        // Hash and update password
                        val hashedNewPassword = BCrypt.hashpw(new, BCrypt.gensalt())
                        supabase.postgrest["companies"]
                            .update(mapOf("password" to hashedNewPassword)) {
                                eq("id", idToCheck)
                            }

                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@ChangePassword,
                                "Password updated successfully.",
                                Toast.LENGTH_SHORT
                            ).show()

                            finish()
                        }

                    } else {
                        idToCheck = app.employeeID ?: return@launch

                        val userResult = supabase.postgrest
                            .from("users")
                            .select {
                                eq("id", idToCheck)
                                limit(1)
                            }

                        val users = json.decodeFromString<List<User>>(userResult.body.toString())
                        val user = users.firstOrNull() ?: return@launch
                        val currentHashedPassword = user.password

                        if (!BCrypt.checkpw(current, currentHashedPassword)) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    this@ChangePassword,
                                    "Current password is incorrect.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            return@launch
                        }

                        if (BCrypt.checkpw(new, currentHashedPassword)) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    this@ChangePassword,
                                    "New password must be different from current password.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            return@launch
                        }

                        val hashedNewPassword = BCrypt.hashpw(new, BCrypt.gensalt())
                        supabase.postgrest["users"]
                            .update(mapOf("password" to hashedNewPassword)) {
                                eq("id", idToCheck)
                            }

                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@ChangePassword,
                                "Password updated successfully.",
                                Toast.LENGTH_SHORT
                            ).show()

                            finish()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@ChangePassword,
                            "Error: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            discardButton.setOnClickListener {
                if(role == "worker"){
                    startActivity(Intent(this, Settings::class.java))
                }
                else{
                    startActivity(Intent(this, AdminSettings::class.java))
                }

                finish()
            }
        }
    }
}

