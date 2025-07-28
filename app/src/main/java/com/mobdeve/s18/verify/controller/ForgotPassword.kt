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

class ForgotPassword : AppCompatActivity() {

    private val client = OkHttpClient()
    private lateinit var supabaseUrl: String
    private lateinit var anonKey: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        val app = application as VerifiApp
        supabaseUrl = app.supabase.supabaseUrl
        anonKey = app.supabase.supabaseKey

        Log.d("FORGOT", "Supabase URL: $supabaseUrl")
        Log.d("FORGOT", "Supabase Key: $anonKey")



        val emailInput = findViewById<EditText>(R.id.forgot_txt_email_input)
        val sendButton = findViewById<Button>(R.id.btn_submit_reset)

        sendButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            if (email.isNotEmpty()) {
                sendResetCode(email)
            } else {
                Toast.makeText(this, "Please enter your email.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendResetCode(email: String) {
        val json = """
            {
                "email": "$email"
            }
        """.trimIndent()
        val sanitizedUrl = if (supabaseUrl.startsWith("http")) supabaseUrl else "https://$supabaseUrl"
        val endpoint = "$sanitizedUrl/functions/v1/request-password-reset"
        Log.d("FORGOT", "Supabase URL: $endpoint")


        val requestBody = json.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(endpoint)
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer $anonKey")
            .post(requestBody)
            .build()

        Log.d("FORGOT Final", "Supabase URL: $request")


        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@ForgotPassword, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                Log.d("FORGOT", "Status: ${response.code}, Body: $body")

                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this@ForgotPassword, "Code sent if email exists.", Toast.LENGTH_LONG).show()

                        val intent = Intent(this@ForgotPassword, VerifyCodeActivity::class.java)
                        intent.putExtra("email", email)
                        startActivity(intent)
                    } else {
                        Toast.makeText(this@ForgotPassword, "Failed to send code.", Toast.LENGTH_SHORT).show()
                    }
                }
            }

        })
    }
}
