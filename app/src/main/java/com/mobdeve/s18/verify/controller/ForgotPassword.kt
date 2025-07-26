package com.mobdeve.s18.verify.controller

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mobdeve.s18.verify.R
import com.mobdeve.s18.verify.app.VerifiApp
import com.mobdeve.s18.verify.model.Company
import com.mobdeve.s18.verify.model.User
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import org.mindrot.jbcrypt.BCrypt

class ForgotPassword : AppCompatActivity() {

    private lateinit var emailField: EditText
    private lateinit var submitButton: Button
    private lateinit var app: VerifiApp
    private val json = Json { ignoreUnknownKeys = true }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        // View bindings
        emailField = findViewById(R.id.forgot_txt_email_input)
        submitButton = findViewById(R.id.btn_submit_reset)

        app = applicationContext as VerifiApp

        submitButton.setOnClickListener {

        }
    }
}
