package com.mobdeve.s18.verify.controller

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.mobdeve.s18.verify.R
import com.mobdeve.s18.verify.app.VerifiApp
import kotlinx.coroutines.*
import org.mindrot.jbcrypt.BCrypt
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.JsonObject


class Register : AppCompatActivity() {

    private lateinit var nameInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var pwInput: EditText
    private lateinit var pw2Input: EditText
    private lateinit var registerBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        nameInput = findViewById(R.id.register_txt_name_input)
        emailInput = findViewById(R.id.register_txt_email_input)
        pwInput = findViewById(R.id.register_txt_pw_input)
        pw2Input = findViewById(R.id.register_txt_pw2_input)
        registerBtn = findViewById(R.id.button)

        val registerText = findViewById<TextView>(R.id.register_txt_loginQuestion)
        registerText.setOnClickListener {
            startActivity(Intent(this, Login::class.java))
        }

        registerBtn.setOnClickListener {
            registerCompany()
        }
    }

    private fun getPasswordStrengthError(password: String): String? {
        return when {
            password.length < 8 -> "Password must be at least 8 characters long."
            !Regex("[A-Z]").containsMatchIn(password) -> "Password must contain at least one uppercase letter."
            !Regex("[a-z]").containsMatchIn(password) -> "Password must contain at least one lowercase letter."
            !Regex("[0-9]").containsMatchIn(password) -> "Password must contain at least one number."
            !Regex("[^A-Za-z0-9]").containsMatchIn(password) -> "Password must contain at least one special character."
            else -> null
        }
    }

    private fun registerCompany() {
        val name = nameInput.text.toString().trim()
        val email = emailInput.text.toString().trim().lowercase()
        val password = pwInput.text.toString()
        val confirmPassword = pw2Input.text.toString()

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill all fields.", Toast.LENGTH_SHORT).show()
            return
        }

        val pwError = getPasswordStrengthError(password)
        if (pwError != null) {
            Toast.makeText(this, pwError, Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_SHORT).show()
            return
        }

        val hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt())

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val supabase = (application as VerifiApp).supabase

                // 1. Check if email exists in companies
                val companyEmailCheck = supabase.postgrest["companies"]
                    .select {
                        eq("email", email)
                    }
                    .decodeList<JsonObject>()


                // 2. Check if email exists in users
                val userEmailCheck = supabase.postgrest["users"]
                    .select {
                        eq("email", email)
                    }
                    .decodeList<JsonObject>()


                if (companyEmailCheck.isNotEmpty() || userEmailCheck.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@Register, "Email is already registered.", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                supabase.postgrest["companies"].insert(
                    mapOf(
                        "name" to name,
                        "email" to email,
                        "password" to hashedPassword
                    )
                )

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@Register, "Company registered!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@Register, Login::class.java))
                    finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@Register, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    @Suppress("MissingSuperCall")
    override fun onBackPressed() {
        val intent = Intent(this, Homepage::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }
}
