package com.mobdeve.s18.verify.controller

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
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
    private lateinit var forgotPasswordText: TextView
    private lateinit var rememberedAccounts: MutableList<RememberedAccount>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        emailEditText = findViewById(R.id.login_txt_email_input)
        passwordEditText = findViewById(R.id.login_txt_pw_input)
        loginButton = findViewById(R.id.btn_login)
        rememberMeCheckBox = findViewById(R.id.login_checkBox)
        forgotPasswordText = findViewById(R.id.login_text_forgotpw)

        rememberMeCheckBox.isChecked = false

        val sharedPrefs = getSharedPreferences("loginPrefs", MODE_PRIVATE)
        val savedJson = sharedPrefs.getString("rememberedAccounts", "[]")
        rememberedAccounts = Json.decodeFromString<List<RememberedAccount>>(savedJson ?: "[]").toMutableList()
        val savedEmails = rememberedAccounts.map { it.email }

        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, savedEmails)
        emailEditText.setAdapter(adapter)

        emailEditText.setOnItemClickListener { _, _, position, _ ->
            val selectedEmail = adapter.getItem(position)
            val matched = rememberedAccounts.find { it.email == selectedEmail }
            passwordEditText.setText(matched?.password ?: "")
        }

        emailEditText.setOnLongClickListener {
            val selectedEmail = emailEditText.text.toString().trim()
            if (selectedEmail.isNotEmpty() && rememberedAccounts.any { it.email == selectedEmail }) {
                AlertDialog.Builder(this)
                    .setTitle("Forget Account?")
                    .setMessage("Do you want to remove remembered account: $selectedEmail?")
                    .setPositiveButton("Yes") { _, _ ->
                        removeRememberedAccount(selectedEmail)
                        emailEditText.text.clear()
                        passwordEditText.text.clear()
                    }
                    .setNegativeButton("No", null)
                    .show()
            }
            true
        }

        findViewById<TextView>(R.id.login_txt_loginQuestion).setOnClickListener {
            startActivity(Intent(this, Register::class.java))
        }

        forgotPasswordText.setOnClickListener {
            startActivity(Intent(this, ForgotPassword::class.java))
        }

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim().lowercase()
            val password = passwordEditText.text.toString()
            Log.d("LOGIN", "Trying email: $email")


            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches() || email.length > 100) {
                Log.w("Validation", "Login failed: Invalid email format or too long -> $email")
                Toast.makeText(this@Login, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 8 || password.length > 50) {
                Log.w("Validation", "Login failed: Password length invalid for $email")
                Toast.makeText(this@Login, "Password must be 8 to 50 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (rememberMeCheckBox.isChecked) {
                maybeUpdateRememberedAccount(email, password) {
                    loginUser(email, password)
                }
            } else {
                loginUser(email, password)
                Log.d("LOGIN", "Trying email: $email password: $password" )

            }
        }
    }

    private fun loginUser(email: String, password: String) {
        val supabase = (application as VerifiApp).supabase
        val json = Json { ignoreUnknownKeys = true }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val prefs = getSharedPreferences("loginPrefs", MODE_PRIVATE)
                val attemptKey = "attempts_$email"
                val currentAttempts = prefs.getInt(attemptKey, 0)

                // Try logging in as company
                val companyResult = supabase.postgrest
                    .from("companies")
                    .select {
                        eq("email", email)
                        limit(1)
                    }

                val companies = json.decodeFromString<List<Company>>(companyResult.body.toString())
                val company = companies.firstOrNull()

                if (company != null) {
                    if (!company.isActive) {
                        withContext(Dispatchers.Main) {
                            Log.w("Auth", "Login blocked: Company $email is deactivated")
                            Toast.makeText(this@Login, "Company account is deactivated", Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }

                    if (BCrypt.checkpw(password, company.password)) {
                        Log.i("Auth", "Login success: Company owner $email")
                        supabase.postgrest.from("companies").update(
                            mapOf("last_login" to kotlinx.datetime.Clock.System.now().toString())
                        ) {
                            eq("id", company.id)
                        }

                        prefs.edit().remove(attemptKey).apply()

                        val app = applicationContext as VerifiApp
                        app.companyID = company.id
                        app.authorizedRole = "owner"

                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@Login, "Logged in as company owner", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@Login, AdminDashboardActivity::class.java))
                            finish()
                        }

                        return@launch
                    } else {
                        Log.w("Auth", "Login failed: Incorrect password for company $email (Attempt ${currentAttempts + 1})")
                        supabase.postgrest.from("companies").update(
                            mapOf("last_failed_login" to kotlinx.datetime.Clock.System.now().toString())
                        ) {
                            eq("id", company.id)
                        }

                        val newAttempts = currentAttempts + 1

                        if (newAttempts >= 5) {
                            supabase.postgrest.from("companies").update(
                                mapOf("isActive" to false)
                            ) {
                                eq("id", company.id)
                            }
                            prefs.edit().remove(attemptKey).apply()

                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@Login, "Company account is deactivated", Toast.LENGTH_SHORT).show()
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
                            Log.w("Auth", "Login blocked: User $email is deactivated")
                            Toast.makeText(this@Login, "Account is deactivated", Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }

                    val userAttemptKey = "attempts_${user.email}"
                    val currentUserAttempts = prefs.getInt(userAttemptKey, 0)

                    if (BCrypt.checkpw(password, user.password)) {
                        Log.i("Auth", "Login success: ${user.role} $email")

                        supabase.postgrest.from("users").update(
                            mapOf("last_login" to kotlinx.datetime.Clock.System.now().toString())
                        ) {
                            eq("id", user.id)
                        }

                        prefs.edit().remove(userAttemptKey).apply()

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
                        Log.w("Auth", "Login failed: Incorrect password for user $email (Attempt ${currentUserAttempts + 1})")

                        val newAttempts = currentUserAttempts + 1

                        supabase.postgrest.from("users").update(
                            mapOf("last_failed_login" to kotlinx.datetime.Clock.System.now().toString())
                        ) {
                            eq("id", user.id)
                        }

                        if (newAttempts >= 5) {
                            supabase.postgrest.from("users").update(
                                mapOf("isActive" to false)
                            ) {
                                eq("id", user.id)
                            }
                            prefs.edit().remove(userAttemptKey).apply()

                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@Login, "Account is deactivated", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Log.w("Auth", "Login failed: No account found for $email")
                            prefs.edit().putInt(userAttemptKey, newAttempts).apply()
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
    }

    private fun maybeUpdateRememberedAccount(email: String, password: String, onContinue: () -> Unit) {
        val sharedPrefs = getSharedPreferences("loginPrefs", MODE_PRIVATE)
        val savedJson = sharedPrefs.getString("rememberedAccounts", "[]")
        val accounts = Json.decodeFromString<MutableList<RememberedAccount>>(savedJson ?: "[]")

        val existingIndex = accounts.indexOfFirst { it.email == email }

        if (existingIndex != -1) {
            val existingPassword = accounts[existingIndex].password
            if (existingPassword != password) {
                AlertDialog.Builder(this)
                    .setTitle("Update Saved Account")
                    .setMessage("Do you want to update the saved password?")
                    .setPositiveButton("Yes") { _, _ ->
                        accounts[existingIndex] = RememberedAccount(email, password)
                        sharedPrefs.edit {
                            putString("rememberedAccounts", Json.encodeToString(accounts))
                        }
                        Toast.makeText(this@Login, "Saved password updated.", Toast.LENGTH_SHORT).show()
                        onContinue()
                    }
                    .setNegativeButton("No") { _, _ -> onContinue() }
                    .show()
            } else {
                onContinue()
            }
        } else {
            accounts.add(RememberedAccount(email, password))
            sharedPrefs.edit {
                putString("rememberedAccounts", Json.encodeToString(accounts))
            }
            onContinue()
        }
    }

    private fun removeRememberedAccount(email: String) {
        val sharedPrefs = getSharedPreferences("loginPrefs", MODE_PRIVATE)
        val savedJson = sharedPrefs.getString("rememberedAccounts", "[]")
        val accounts = Json.decodeFromString<MutableList<RememberedAccount>>(savedJson ?: "[]")

        val updatedAccounts = accounts.filterNot { it.email == email }
        sharedPrefs.edit {
            putString("rememberedAccounts", Json.encodeToString(updatedAccounts))
        }

        Toast.makeText(this@Login, "Removed remembered account: $email", Toast.LENGTH_SHORT).show()

        val updatedEmails = updatedAccounts.map { it.email }
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, updatedEmails)
        emailEditText.setAdapter(adapter)

        rememberedAccounts = updatedAccounts.toMutableList()
    }
}
