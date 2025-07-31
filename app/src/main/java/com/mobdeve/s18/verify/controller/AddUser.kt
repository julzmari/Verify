package com.mobdeve.s18.verify.controller

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Patterns
import android.widget.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.mobdeve.s18.verify.R
import com.mobdeve.s18.verify.app.VerifiApp
import com.mobdeve.s18.verify.model.User
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import kotlinx.serialization.json.JsonObject
import org.mindrot.jbcrypt.BCrypt
import java.util.*
import android.text.TextWatcher
import android.text.Editable
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.mobdeve.s18.verify.repository.insertPasswordHistoryWithRetry
import com.nulabinc.zxcvbn.Zxcvbn

class AddUser : BaseActivity() {

    private lateinit var passwordStrengthBar: ProgressBar
    private lateinit var passwordStrengthLabel: TextView

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_user)

        val nameInput = findViewById<EditText>(R.id.Username)
        val emailInput = findViewById<EditText>(R.id.Email)
        val passwordInput = findViewById<EditText>(R.id.Pass)
        val confirmPasswordInput = findViewById<EditText>(R.id.confirmPass)

        passwordStrengthBar = findViewById(R.id.passwordStrengthBar)
        passwordStrengthLabel = findViewById(R.id.passwordStrengthLabel)

        passwordInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                updatePasswordStrength(s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        val roleInput = findViewById<Spinner>(R.id.userRoleSpinner)
        val addUserButton = findViewById<Button>(R.id.addUser)
        val returnButton = findViewById<TextView>(R.id.returnText)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav2)
        setupBottomNavigation(bottomNav, 0)

        val currentRole = (application as VerifiApp).authorizedRole
        val availableRoles = if (currentRole == "admin") {
            listOf("Regular Worker")
        } else {
            listOf("Admin", "Regular Worker")
        }

        val roleAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, availableRoles)
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        roleInput.adapter = roleAdapter

        returnButton.setOnClickListener {
            startActivity(Intent(this, ManageUser::class.java))
        }

        addUserButton.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val email = emailInput.text.toString().trim().lowercase()
            val password = passwordInput.text.toString()
            val confirmPassword = confirmPasswordInput.text.toString()
            val selectedRole = roleInput.selectedItem.toString()

            val role = when (selectedRole) {
                "Admin" -> "admin"
                "Regular Worker" -> "reg_employee"
                else -> {
                    AppLogger.w("Validation", "AddUser failed: One or more fields were empty.")
                    Toast.makeText(this, "Invalid role selected", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            if (name.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
                AppLogger.w("Validation", "AddUser failed: Feilds incomplete. Name=$name")
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (name.length > 100) {
                AppLogger.w("Validation", "AddUser failed: Name exceeds 100 characters. Name=$name")
                Toast.makeText(this, "Name is too long", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches() || email.length > 100) {
                AppLogger.w("Validation", "AddUser failed: Invalid email $email")
                Toast.makeText(this, "Invalid email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val pwError = getPasswordStrengthError(password)
            if (pwError != null) {
                AppLogger.w("Validation", "AddUser failed: Password validation error -> $pwError")
                Toast.makeText(this, pwError, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                AppLogger.w("Validation", "AddUser failed: Passwords do not match")
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val app = applicationContext as VerifiApp
            val companyID = app.companyID

            if (companyID == null) {
                AppLogger.w("Validation", "AddUser failed: Company ID is missing!")
                Toast.makeText(this, "Company ID is missing!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt())

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val supabase = app.supabase
                    val newUserId = UUID.randomUUID().toString()

                    val companyEmailCheck = supabase.postgrest["companies"]
                        .select { eq("email", email) }
                        .decodeList<JsonObject>()

                    val userEmailCheck = supabase.postgrest["users"]
                        .select { eq("email", email) }
                        .decodeList<JsonObject>()

                    if (companyEmailCheck.isNotEmpty() || userEmailCheck.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            AppLogger.w("Validation", "AddUser failed: Email $email")
                            Toast.makeText(this@AddUser, "Unable to add user. Please try again.", Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }

                    val newUser = User(
                        id = newUserId,
                        companyID = companyID,
                        role = role,
                        name = name,
                        email = email,
                        password = hashedPassword,
                        isActive = true,
                        createdAt = Clock.System.now(),
                        profileURL = ""
                    )

                    supabase.postgrest["users"].insert(newUser)

                    val success = insertPasswordHistoryWithRetry(
                        supabase,
                        newUserId,
                        hashedPassword,
                        "user"
                    )

                    if (!success) {
                        supabase.postgrest["users"].delete { eq("id", newUserId) }

                        withContext(Dispatchers.Main) {
                            AppLogger.w("Validation", "Failed to add user. Rolled back.")
                            Toast.makeText(this@AddUser, "Failed to add user. Rolled back.", Toast.LENGTH_LONG).show()
                        }
                        return@launch
                    }

                    // 5. Success
                    withContext(Dispatchers.Main) {
                        AppLogger.i("Add User", "Successfully added user")
                        Toast.makeText(this@AddUser, "User added successfully!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@AddUser, ManageUser::class.java))
                        finish()
                    }

                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        AppLogger.w("Add User", "Failed to add user.")
                        Toast.makeText(this@AddUser, "Failed to add user. Try again later.", Toast.LENGTH_LONG).show()
                    }
                }
            }

        }
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
}