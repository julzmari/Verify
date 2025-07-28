package com.mobdeve.s18.verify.controller

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mobdeve.s18.verify.R
import com.mobdeve.s18.verify.app.VerifiApp
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class VerifyCodeActivity : AppCompatActivity() {

    private lateinit var codeInput: EditText
    private lateinit var verifyButton: Button
    private val client = OkHttpClient()

    private lateinit var supabaseUrl: String
    private lateinit var anonKey: String
    private lateinit var email: String  // coming from intent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_code)

        val app = application as VerifiApp
        supabaseUrl = app.supabase.supabaseUrl
        anonKey = app.supabase.supabaseKey

        codeInput = findViewById(R.id.editTextVerificationCode)
        verifyButton = findViewById(R.id.buttonVerifyCode)

        // Get email from intent
        email = intent.getStringExtra("email") ?: ""

        verifyButton.setOnClickListener {
            val code = codeInput.text.toString().trim()
            if (email.isNotEmpty() && code.isNotEmpty()) {
                verifyCode(email, code)
            } else {
                Toast.makeText(this, "Please enter the verification code.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun verifyCode(email: String, code: String) {
        val json = """
            {
                "email": "$email",
                "token": "$code"
            }
        """.trimIndent()

        val request = Request.Builder()
            .url("$supabaseUrl/functions/v1/verify-reset-token")
            .addHeader("apikey", anonKey)
            .addHeader("Authorization", "Bearer $anonKey")
            .post(json.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@VerifyCodeActivity, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                runOnUiThread {
                    if (response.isSuccessful) {
                        // Move to password reset screen
                        val intent = Intent(this@VerifyCodeActivity, ResetPasswordActivity::class.java)
                        intent.putExtra("email", email)
                        intent.putExtra("token", code) // Pass token for backend verification
                        startActivity(intent)
                        finish()
                    } else {
                        Log.e("VerifyCode", "Verification failed: $body")
                        Toast.makeText(this@VerifyCodeActivity, "Invalid or expired code", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
}
