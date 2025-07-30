package com.mobdeve.s18.verify.controller

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.mobdeve.s18.verify.R
import com.mobdeve.s18.verify.app.VerifiApp
import com.mobdeve.s18.verify.model.Company
import com.mobdeve.s18.verify.model.User
import com.nulabinc.zxcvbn.Zxcvbn
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import org.mindrot.jbcrypt.BCrypt
import androidx.lifecycle.lifecycleScope
import com.mobdeve.s18.verify.repository.PasswordHistoryRepository
import com.mobdeve.s18.verify.repository.insertPasswordHistoryWithRetry

class ChangePassword : AppCompatActivity() {

    private lateinit var app: VerifiApp
    private var role: String? = null

    private lateinit var currentPassword: EditText
    private lateinit var newPassword: EditText
    private lateinit var confirmPassword: EditText
    private lateinit var strengthBar: ProgressBar
    private lateinit var strengthLabel: TextView
    private lateinit var submitButton: Button
    private lateinit var discardButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_changepassword)

        currentPassword = findViewById(R.id.currentPassword)
        newPassword = findViewById(R.id.newPassword)
        confirmPassword = findViewById(R.id.confirmPassword)
        submitButton = findViewById(R.id.submitPassword)
        discardButton = findViewById(R.id.discardPassword)



        // Dynamically insert strength bar and label
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
            val current = currentPassword.text.toString()
            val new = newPassword.text.toString()
            val confirm = confirmPassword.text.toString()

            if (current.isBlank() || new.isBlank() || confirm.isBlank()) {
                Toast.makeText(this, "Please complete all fields.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (new != confirm) {
                Toast.makeText(this, "New passwords do not match.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val pwError = getPasswordStrengthError(new)
            if (pwError != null) {
                Toast.makeText(this, pwError, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                app = applicationContext as VerifiApp
                role = app.authorizedRole
                val supabase = app.supabase
                val json = Json { ignoreUnknownKeys = true }
                val idToCheck: String

                try {
                    if (role == "owner") {
                        idToCheck = app.companyID ?: return@launch
                        val company = supabase.postgrest["companies"]
                            .select { eq("id", idToCheck); limit(1) }
                            .decodeList<Company>().firstOrNull()

                        if (company == null || !BCrypt.checkpw(current, company.password)) {
                            showFailToast()
                            return@launch
                        }

                        if (BCrypt.checkpw(new, company.password)) {
                            showReuseWarning()
                            return@launch
                        }

                        updatePassword("companies", idToCheck, new, company.password)

                    } else {
                        idToCheck = app.employeeID ?: return@launch
                        val user = supabase.postgrest["users"]
                            .select { eq("id", idToCheck); limit(1) }
                            .decodeList<User>().firstOrNull()

                        if (user == null || !BCrypt.checkpw(current, user.password)) {
                            showFailToast()
                            return@launch
                        }

                        if (BCrypt.checkpw(new, user.password)) {
                            showReuseWarning()
                            return@launch
                        }

                        updatePassword("users", idToCheck, new, user.password)
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@ChangePassword, "Something went wrong.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        discardButton.setOnClickListener {
            if ((application as VerifiApp).authorizedRole == "worker") {
                startActivity(Intent(this, Settings::class.java))
            } else {
                startActivity(Intent(this, AdminSettings::class.java))
            }
            finish()
        }
    }

    private fun getPasswordStrengthError(password: String): String? {
        if (password.length < 8) return "Password must be at least 8 characters long."
        if (password.length > 50) return "Password must not exceed 50 characters."
        if (!Regex("[A-Z]").containsMatchIn(password)) return "Must include an uppercase letter."
        if (!Regex("[a-z]").containsMatchIn(password)) return "Must include a lowercase letter."
        if (!Regex("[0-9]").containsMatchIn(password)) return "Must include a number."
        if (!Regex("[^A-Za-z0-9]").containsMatchIn(password)) return "Must include a special character."

        val score = Zxcvbn().measure(password).score
        if (score < 3) return "Password is too weak."

        return null
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

    private suspend fun updatePassword(table: String, id: String, newPassword: String, oldPassword: String) {
        val supabase = app.supabase
        val hashedNew = BCrypt.hashpw(newPassword, BCrypt.gensalt())
        val repo = PasswordHistoryRepository(supabase)


        val userType = if (table == "companies") "company" else "user"

        val canChange = try {
            repo.isPasswordChangeAllowed(id, userType)
        } catch (e: Exception) {
            Log.e("PASSWORD_HISTORY", "Check failed: ${e.message}", e)
            false
        }

        if (!canChange) {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@ChangePassword,
                    "Password cannot be changed yet. Please wait 24 hours since your last change.",
                    Toast.LENGTH_LONG
                ).show()
            }
            return
        }



        if (repo.isPasswordReused(id, userType, newPassword)) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@ChangePassword, "Cannot reuse any of your last 3 passwords.", Toast.LENGTH_LONG).show()
            }
            return
        }



        try {
            supabase.postgrest[table].update(mapOf("password" to hashedNew)) {
                eq("id", id)
            }

            val userType = if (table == "companies") "company" else "user"

            val historyInserted = insertPasswordHistoryWithRetry(
                supabase,
                userId = id,
                hashedPassword = hashedNew,
                userType = userType)

            if (!historyInserted) {
                supabase.postgrest[table].update(mapOf("password" to oldPassword)) {
                        eq("id", id)
                    }



                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ChangePassword,
                        "Password change failed. Rolled back to the previous password.",
                        Toast.LENGTH_LONG
                    ).show()
                }
                return
            }

            try {
                repo.pruneOldPasswords(id, userType)
            } catch (e: Exception) {
                Log.w("PASSWORD_HISTORY", "Failed to prune old passwords: ${e.message}")
            }


            withContext(Dispatchers.Main) {
                Toast.makeText(this@ChangePassword, "Password changed successfully.", Toast.LENGTH_SHORT).show()
                finish()
            }

        } catch (e: Exception) {
            supabase.postgrest[table].update(mapOf("password" to oldPassword)) {
                    eq("id", id)
                }

            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@ChangePassword,
                    "Password change failed. Rolled back to previous password.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }





    private suspend fun showFailToast() {
        withContext(Dispatchers.Main) {
            Toast.makeText(this@ChangePassword, "Password change failed. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun showReuseWarning() {
        withContext(Dispatchers.Main) {
            Toast.makeText(this@ChangePassword, "New password must be different.", Toast.LENGTH_SHORT).show()
        }
    }




}