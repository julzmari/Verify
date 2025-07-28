package com.mobdeve.s18.verify.controller

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.mobdeve.s18.verify.R
import com.mobdeve.s18.verify.app.VerifiApp
import com.mobdeve.s18.verify.model.Company
import com.mobdeve.s18.verify.model.CompanyUpdatePayload
import com.mobdeve.s18.verify.model.User
import com.nulabinc.zxcvbn.Zxcvbn
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import org.mindrot.jbcrypt.BCrypt

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var newPassword: EditText
    private lateinit var confirmPassword: EditText
    private lateinit var strengthBar: ProgressBar
    private lateinit var strengthLabel: TextView
    private lateinit var submitButton: Button

    private lateinit var app: VerifiApp


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
            val newPass = newPassword.text.toString()
            val confirmPass = confirmPassword.text.toString()

            if (newPass.isBlank() || confirmPass.isBlank()) {
                Toast.makeText(this, "Please complete all fields.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPass != confirmPass) {
                Toast.makeText(
                    this@ResetPasswordActivity,
                    "Passwords do not match.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val error = getPasswordStrengthError(newPass)
            if (error != null) {
                Toast.makeText(this@ResetPasswordActivity, error, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val supabase = app.supabase
                val json = Json { ignoreUnknownKeys = true }
                val email = intent.getStringExtra("email") ?: return@launch

                try {

                    val companyResult = supabase.postgrest
                        .from("companies")
                        .select { eq("email", email); limit(1) }
                    val companies =
                        json.decodeFromString<List<Company>>(companyResult.body.toString())
                    val company = companies.firstOrNull()

                    if (company != null) {


                        if (BCrypt.checkpw(newPass, company.password)) {
                            showReuseWarning()
                            return@launch
                        }

                        updateCompanyPasswordAndActivate(company.id, newPass)
                        val intent = Intent(this@ResetPasswordActivity, Login::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        return@launch



                    } else {
                        val userResult = supabase.postgrest
                            .from("users")
                            .select { eq("email", email); limit(1) }

                        val users = json.decodeFromString<List<User>>(userResult.body.toString())
                        val user = users.firstOrNull()

                        if (user != null) {

                            if (BCrypt.checkpw(newPass, user.password)) {
                                showReuseWarning()
                                return@launch
                            }

                            updatePassword("users", user.id, newPass)
                            val intent = Intent(this@ResetPasswordActivity, Login::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            return@launch

                        }
                    }

                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@ResetPasswordActivity,
                            "Email not found.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@ResetPasswordActivity,
                            "Something went wrong.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }


    }


    private suspend fun updatePassword(table: String, id: String, newPassword: String) {
        val supabase = app.supabase
        val hashed = BCrypt.hashpw(newPassword, BCrypt.gensalt())
        supabase.postgrest[table].update(mapOf("password" to hashed)) {
            eq("id", id)
        }
        withContext(Dispatchers.Main) {
            Toast.makeText(
                this@ResetPasswordActivity,
                "Password changed successfully.",
                Toast.LENGTH_SHORT
            ).show()
            finish()
        }


    }

    private suspend fun updateCompanyPasswordAndActivate(id: String, newPassword: String) {
        val supabase = app.supabase
        val hashed = BCrypt.hashpw(newPassword, BCrypt.gensalt())

        try {

            val updatePayload = CompanyUpdatePayload(
                password = hashed,
                isActive = true
            )
            val response = supabase.postgrest["companies"]
                .update(updatePayload) {
                    eq("id", id)
                }

            Log.d("ResetPassword", "Response: $response")

            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@ResetPasswordActivity,
                    "Password changed successfully.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        } catch (e: Exception) {
            Log.e("ResetPassword", "Update failed", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@ResetPasswordActivity,
                    "Error updating password.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
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

