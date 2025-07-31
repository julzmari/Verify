package com.mobdeve.s18.verify.controller

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.mobdeve.s18.verify.R
import com.mobdeve.s18.verify.app.VerifiApp
import com.mobdeve.s18.verify.model.Company
import com.mobdeve.s18.verify.model.CompanyUpdatePayload
import com.mobdeve.s18.verify.model.User
import com.mobdeve.s18.verify.repository.insertPasswordHistoryWithRetry
import com.nulabinc.zxcvbn.Zxcvbn
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import org.mindrot.jbcrypt.BCrypt
import com.mobdeve.s18.verify.repository.PasswordHistoryRepository


class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var newPassword: EditText
    private lateinit var confirmPassword: EditText
    private lateinit var strengthBar: ProgressBar
    private lateinit var strengthLabel: TextView
    private lateinit var submitButton: Button

    private lateinit var app: VerifiApp


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)
        app = applicationContext as VerifiApp



        newPassword = findViewById(R.id.reset_txt_password)
        confirmPassword = findViewById(R.id.reset_txt_confirmPassword)
        submitButton = findViewById(R.id.btn_submit_new_password)

        strengthBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal)
        strengthLabel = TextView(this)
        val layout = newPassword.parent as LinearLayout
        strengthBar.max = 100
        layout.addView(strengthBar, layout.indexOfChild(newPassword) + 1)
        layout.addView(strengthLabel, layout.indexOfChild(strengthBar) + 1)


        newPassword.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                updatePasswordStrength(s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        submitButton.setOnClickListener {
            val new = newPassword.text.toString()
            val confirm = confirmPassword.text.toString()

            if (new.isBlank() || confirm.isBlank()) {
                Toast.makeText(this, "Please complete all fields.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (new != confirm) {
                Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val pwError = getPasswordStrengthError(new)
            if (pwError != null) {
                Toast.makeText(this, pwError, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val email = intent.getStringExtra("email") ?: return@launch
                val supabase = app.supabase
                val json = Json { ignoreUnknownKeys = true }

                try {
                    // 1️⃣ Check company first
                    val company = supabase.postgrest["companies"]
                        .select { eq("email", email); limit(1) }
                        .decodeList<Company>()
                        .firstOrNull()

                    if (company != null) {
                        if (BCrypt.checkpw(new, company.password)) {
                            showReuseWarning()
                            return@launch
                        }
                        updateCompanyPasswordAndActivate(company.id, new, company.password, company.isActive)
                        return@launch
                    }

                    // 2️⃣ Otherwise check users
                    val user = supabase.postgrest["users"]
                        .select { eq("email", email); limit(1) }
                        .decodeList<User>()
                        .firstOrNull()

                    if (user != null) {
                        if (BCrypt.checkpw(new, user.password)) {
                            showReuseWarning()
                            return@launch
                        }
                        updatePassword("users", user.id, new, user.password)
                        return@launch
                    }

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@ResetPasswordActivity, "Email not found.", Toast.LENGTH_SHORT).show()
                    }

                } catch (e: Exception) {
                    Log.e("ResetPassword", "Error", e)
                    showFailToast()
                }
            }
        }
    }





    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun updatePassword(table: String, id: String, newPassword: String, oldPassword: String) {
        val supabase = app.supabase
        val hashedNew = BCrypt.hashpw(newPassword, BCrypt.gensalt())
        val repo = PasswordHistoryRepository(supabase)

        val userType = if (table == "companies") "company" else "user"


        if (repo.isPasswordReused(id, userType, newPassword)) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@ResetPasswordActivity, "Cannot reuse any of your last 3 passwords.", Toast.LENGTH_LONG).show()
            }
            return
        }


        try {
            supabase.postgrest[table].update(mapOf("password" to hashedNew)) { eq("id", id) }

            val historyInserted = insertPasswordHistoryWithRetry(supabase, id, hashedNew, "user")
            if (!historyInserted) {
                supabase.postgrest[table].update(mapOf("password" to oldPassword)) { eq("id", id) }
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ResetPasswordActivity, "Password change failed. Rolled back.", Toast.LENGTH_LONG).show()
                }
                return
            }

            try {
                repo.pruneOldPasswords(id, "user")
            } catch (e: Exception) {
                AppLogger.w("PASSWORD_HISTORY", "Pruning failed: ${e.message}")
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(this@ResetPasswordActivity, "Password changed successfully.", Toast.LENGTH_SHORT).show()
                goToLogin()
            }

        } catch (e: Exception) {
            supabase.postgrest[table].update(mapOf("password" to oldPassword)) { eq("id", id) }
            withContext(Dispatchers.Main) {
                Toast.makeText(this@ResetPasswordActivity, "Password change failed. Rolled back.", Toast.LENGTH_LONG).show()
            }
        }
    }




    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun updateCompanyPasswordAndActivate(id: String, newPassword: String, oldPassword: String, isActive: Boolean) {
        val supabase = app.supabase
        val hashedNew = BCrypt.hashpw(newPassword, BCrypt.gensalt())
        val repo = PasswordHistoryRepository(supabase)

        val userType = "company"


        if (repo.isPasswordReused(id, userType, newPassword)) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@ResetPasswordActivity, "Cannot reuse any of your last 3 passwords.", Toast.LENGTH_LONG).show()
            }
            return
        }

        try {
            val updatePayload = CompanyUpdatePayload(
                password = hashedNew,
                isActive = true
            )

            supabase.postgrest["companies"].update(updatePayload) {
                eq("id", id)
            }


            val historyInserted = insertPasswordHistoryWithRetry(supabase, id, hashedNew, "company")
            if (!historyInserted) {

                val rollback = CompanyUpdatePayload(
                    password = oldPassword,
                    isActive = isActive
                )

                supabase.postgrest["companies"].update(rollback) {
                    eq("id", id)
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ResetPasswordActivity, "Password change failed. Rolled back.", Toast.LENGTH_LONG).show()
                }
                return
            }

            try {
                repo.pruneOldPasswords(id, "company")
            } catch (e: Exception) {
                AppLogger.w("PASSWORD_HISTORY", "Pruning failed: ${e.message}")
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(this@ResetPasswordActivity, "Password changed successfully.", Toast.LENGTH_SHORT).show()
                goToLogin()
            }

        } catch (e: Exception) {
            supabase.postgrest["companies"].update(mapOf("password" to oldPassword, "is_active" to isActive)) { eq("id", id) }
            withContext(Dispatchers.Main) {
                Toast.makeText(this@ResetPasswordActivity, "Password change failed. Rolled back.", Toast.LENGTH_LONG).show()
            }
        }
    }



    private fun goToLogin() {
        val intent = Intent(this@ResetPasswordActivity, Login::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun updatePasswordStrength(password: String) {
        val zxcvbn = Zxcvbn()
        val result = zxcvbn.measure(password)
        val score = result.score
        val feedback = result.feedback.suggestions.joinToString(", ")

        val (progress, label, colorRes) = when (score) {
            4 -> Triple(100, "Very Strong", R.color.green_primary)
            3 -> Triple(75, "Strong", android.R.color.holo_green_dark)
            2 -> Triple(50, "Moderate", android.R.color.holo_orange_dark)
            1 -> Triple(25, "Weak", android.R.color.holo_red_light)
            else -> Triple(10, "Very Weak", android.R.color.holo_red_dark)
        }

        strengthBar.progress = progress
        strengthLabel.text = "$label${if (feedback.isNotEmpty()) " - $feedback" else ""}"
        strengthBar.progressDrawable.setColorFilter(
            ContextCompat.getColor(this, colorRes),
            android.graphics.PorterDuff.Mode.SRC_IN
        )
    }

    private fun getPasswordStrengthError(password: String): String? {
        if (password.length < 8) return "Password must be at least 8 characters long."
        if (password.length > 50) return "Password must not exceed 50 characters."
        if (!Regex("[A-Z]").containsMatchIn(password)) return "Must include an uppercase letter."
        if (!Regex("[a-z]").containsMatchIn(password)) return "Must include a lowercase letter."
        if (!Regex("[0-9]").containsMatchIn(password)) return "Must include a number."
        if (!Regex("[^A-Za-z0-9]").containsMatchIn(password)) return "Must include a special character."

        val score = Zxcvbn().measure(password).score
        return if (score < 3) "Password is too weak." else null
    }

    private suspend fun showFailToast() {
        withContext(Dispatchers.Main) {
            Toast.makeText(
                this@ResetPasswordActivity,
                "Password change failed. Please try again.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private suspend fun showReuseWarning() {
        withContext(Dispatchers.Main) {
            Toast.makeText(
                this@ResetPasswordActivity,
                "New password must be different.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

}

