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

    private val client = OkHttpClient()
    private lateinit var supabaseUrl: String
    private lateinit var anonKey: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_code)

        val app = application as VerifiApp
        supabaseUrl = app.supabase.supabaseUrl
        anonKey = app.supabase.supabaseKey

        val email = intent.getStringExtra("email") ?: ""
        val codeInput = findViewById<EditText>(R.id.editTextVerificationCode)
        val verifyButton = findViewById<Button>(R.id.buttonVerifyCode)

        verifyButton.setOnClickListener {
            val code = codeInput.text.toString().trim()
            if (code.matches(Regex("^\\d{6}$"))) {
                verifyResetCode(email, code)
            } else {
                Toast.makeText(this, "Please enter a valid 6-digit code.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun verifyResetCode(email: String, code: String) {
        val json = """
            {
                "email": "$email",
                "token": "$code"
            }
        """.trimIndent()

        val sanitizedUrl = if (supabaseUrl.startsWith("http")) supabaseUrl else "https://$supabaseUrl"
        val endpoint = "$sanitizedUrl/functions/v1/verify_code"
        val requestBody = json.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(endpoint)
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer $anonKey")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@VerifyCodeActivity, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                Log.d("VERIFY", "Status: ${response.code}, Body: $body")

                runOnUiThread {
                    if (response.isSuccessful) {
                        val intent = Intent(this@VerifyCodeActivity, ResetPasswordActivity::class.java)
                        intent.putExtra("email", email)
                        intent.putExtra("token", code) // optional
                        startActivity(intent)
                    } else {
                        Toast.makeText(this@VerifyCodeActivity, "Invalid or expired code.", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@VerifyCodeActivity, ForgotPassword::class.java)
                        startActivity(intent)
                        finish()
                    }
                }
            }
        })
    }
}
