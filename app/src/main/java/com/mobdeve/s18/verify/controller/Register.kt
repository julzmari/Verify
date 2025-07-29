package com.mobdeve.s18.verify.controller

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.util.Patterns
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.mobdeve.s18.verify.R
import com.mobdeve.s18.verify.app.VerifiApp
import kotlinx.coroutines.*
import org.mindrot.jbcrypt.BCrypt
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import com.nulabinc.zxcvbn.Zxcvbn
import android.text.TextWatcher
import com.mobdeve.s18.verify.model.Company
import com.mobdeve.s18.verify.repository.insertPasswordHistoryWithRetry
import io.github.jan.supabase.SupabaseClient
import kotlinx.serialization.json.Json



class Register : AppCompatActivity() {

    private lateinit var nameInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var pwInput: EditText
    private lateinit var pw2Input: EditText
    private lateinit var registerBtn: Button

    private lateinit var passwordStrengthBar: ProgressBar
    private lateinit var passwordStrengthLabel: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        nameInput = findViewById(R.id.register_txt_name_input)
        emailInput = findViewById(R.id.register_txt_email_input)
        pwInput = findViewById(R.id.register_txt_pw_input)
        pw2Input = findViewById(R.id.register_txt_pw2_input)
        registerBtn = findViewById(R.id.button)

        findViewById<TextView>(R.id.register_txt_loginQuestion).setOnClickListener {
            startActivity(Intent(this, Login::class.java))
        }

        registerBtn.setOnClickListener {
            registerCompany()
        }

        passwordStrengthBar = findViewById(R.id.passwordStrengthBar)
        passwordStrengthLabel = findViewById(R.id.passwordStrengthLabel)

        pwInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                updatePasswordStrength(s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    @SuppressLint("SetTextI18n")
    private fun updatePasswordStrength(password: String) {
        val zxcvbn = Zxcvbn()
        val strength = zxcvbn.measure(password)

        val score = strength.score  // 0 (very weak) to 4 (very strong)
        val feedback = strength.feedback.suggestions.joinToString(", ")

        val (progress, label, color) = when (score) {
            4 -> Triple(100, "Very Strong", R.color.green_primary)
            3 -> Triple(75, "Strong", android.R.color.holo_green_dark)
            2 -> Triple(50, "Moderate", android.R.color.holo_orange_dark)
            1 -> Triple(25, "Weak", android.R.color.holo_red_dark)
            else -> Triple(10, "Very Weak", android.R.color.holo_red_dark)
        }

        passwordStrengthBar.progress = progress
        passwordStrengthLabel.text = "$label ${if (feedback.isNotEmpty()) "- $feedback" else ""}"

        passwordStrengthBar.progressDrawable.setColorFilter(
            ContextCompat.getColor(this, color),
            android.graphics.PorterDuff.Mode.SRC_IN
        )
    }

    private fun getPasswordStrengthError(password: String): String? {
        if (password.length < 8) return "Password must be at least 8 characters long."
        if (password.length > 50) return "Password must not exceed 50 characters."
        if (!Regex("[A-Z]").containsMatchIn(password)) return "Password must contain at least one uppercase letter."
        if (!Regex("[a-z]").containsMatchIn(password)) return "Password must contain at least one lowercase letter."
        if (!Regex("[0-9]").containsMatchIn(password)) return "Password must contain at least one number."
        if (!Regex("[^A-Za-z0-9]").containsMatchIn(password)) return "Password must contain at least one special character."

        val zxcvbn = Zxcvbn()
        val score = zxcvbn.measure(password).score
        if (score < 3) return "Password is too weak. Try making it stronger."

        return null
    }

    private fun registerCompany() {
        val name = nameInput.text.toString().trim()
        val email = emailInput.text.toString().trim().lowercase()
        val password = pwInput.text.toString()
        val confirmPassword = pw2Input.text.toString()

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill all fields.", Toast.LENGTH_SHORT).show()
            Log.w("REGISTER_VALIDATION", "Empty input field.")
            return
        }

        if (name.length > 100) {
            Toast.makeText(this, "Name is too long.", Toast.LENGTH_SHORT).show()
            Log.w("REGISTER_VALIDATION", "Name exceeds length.")
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches() || email.length > 100) {
            Toast.makeText(this, "Please enter a valid email address.", Toast.LENGTH_SHORT).show()
            Log.w("REGISTER_VALIDATION", "Invalid email format or length.")
            return
        }

        val pwError = getPasswordStrengthError(password)
        if (pwError != null) {
            Toast.makeText(this, pwError, Toast.LENGTH_SHORT).show()
            Log.w("REGISTER_VALIDATION", "Password failed strength check.")
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_SHORT).show()
            Log.w("REGISTER_VALIDATION", "Password mismatch.")
            return
        }

        val hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt())

        CoroutineScope(Dispatchers.IO).launch {
            val supabase = (application as VerifiApp).supabase
            val json = Json { ignoreUnknownKeys = true }

            try {
                // --- Check email uniqueness ---
                val companyEmailCheck = supabase.postgrest["companies"]
                    .select { eq("email", email) }
                    .decodeList<JsonObject>()

                val userEmailCheck = supabase.postgrest["users"]
                    .select { eq("email", email) }
                    .decodeList<JsonObject>()

                if (companyEmailCheck.isNotEmpty() || userEmailCheck.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@Register, "Registration failed. Email already used.", Toast.LENGTH_SHORT).show()
                    }
                    Log.w("REGISTER_ATTEMPT", "Email already used: $email")
                    return@launch
                }

                // --- Insert company ---
                val insertedCompany = supabase.postgrest["companies"]
                    .insert(
                        buildJsonObject {
                            put("name", name)
                            put("email", email)
                            put("password", hashedPassword)
                            put("isActive", true)
                        }
                    )
                    .decodeSingle<Company>()

                // --- Try inserting password_history with retry ---
                val success = insertPasswordHistoryWithRetry(supabase, insertedCompany.id, hashedPassword, "company")

                if (!success) {
                    // Rollback: delete the company if history insert failed
                    Log.e("REGISTER_ROLLBACK", "Deleting company ${insertedCompany.id} due to password_history failure.")
                    supabase.postgrest["companies"].delete { eq("id", insertedCompany.id) }

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@Register, "Registration failed. Please try again.", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }

                // --- Registration successful ---
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@Register, "Company registered!", Toast.LENGTH_SHORT).show()
                    Log.i("REGISTER_ATTEMPT", "New company registered: $email")
                    startActivity(Intent(this@Register, Login::class.java))
                    finish()
                }

            } catch (e: Exception) {
                Log.e("REGISTER_ERROR", "Error during registration", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@Register, "Registration failed. Please try again later.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }





    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, Homepage::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }
}