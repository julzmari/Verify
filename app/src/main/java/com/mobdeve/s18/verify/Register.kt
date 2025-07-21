package com.mobdeve.s18.verify

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import org.mindrot.jbcrypt.BCrypt
import io.github.jan.supabase.postgrest.postgrest

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

    private fun registerCompany() {
        val name = nameInput.text.toString().trim()
        val email = emailInput.text.toString().trim()
        val password = pwInput.text.toString()
        val confirmPassword = pw2Input.text.toString()

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill all fields.", Toast.LENGTH_SHORT).show()
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
