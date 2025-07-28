package com.mobdeve.s18.verify.controller

import android.content.Intent
import android.net.Uri
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

class ResetPasswordActivity : AppCompatActivity() {

    private val client = OkHttpClient()
    private lateinit var supabaseUrl: String
    private lateinit var anonKey: String
    private lateinit var token: String
    private lateinit var email: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)
        handleDeepLink(intent?.data)


        val app = application as VerifiApp
        supabaseUrl = app.supabase.supabaseUrl
        anonKey = app.supabase.supabaseKey

        // Extract deep link data
        val data: Uri? = intent?.data

        val uri = intent?.data
        Log.d("DEEPLINK", "Full URI: $uri")

        token = data?.getQueryParameter("token") ?: ""
        email = data?.getQueryParameter("email") ?: ""
        Log.d("DEEPLINK", "Token: $token")
        Log.d("DEEPLINK", "Email: $email")

        val passwordInput = findViewById<EditText>(R.id.reset_txt_password)
        val resetButton = findViewById<Button>(R.id.btn_submit_new_password)

        resetButton.setOnClickListener {
            val newPassword = passwordInput.text.toString().trim()
            if (newPassword.length >= 6) {
                submitNewPassword(newPassword)
            } else {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent?.data)
    }
    private fun handleDeepLink(data: Uri?) {
        token = data?.getQueryParameter("token") ?: ""
        email = data?.getQueryParameter("email") ?: ""
        Log.d("DEEPLINK", "Token: $token")
        Log.d("DEEPLINK", "Email: $email")
    }



    private fun submitNewPassword(password: String) {
        val json = """
            {
                "token": "$token",
                "email": "$email",
                "new_password": "$password"
            }
        """.trimIndent()

        val requestBody = json.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$supabaseUrl/functions/v1/reset-password")
            .addHeader("apikey", anonKey)
            .addHeader("Authorization", "Bearer $anonKey")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@ResetPasswordActivity, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this@ResetPasswordActivity, "Password reset successfully!", Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        Toast.makeText(this@ResetPasswordActivity, "Reset failed. Please try again.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
}
