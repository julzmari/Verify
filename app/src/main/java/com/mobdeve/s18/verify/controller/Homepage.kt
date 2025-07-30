package com.mobdeve.s18.verify.controller

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.mobdeve.s18.verify.R

class Homepage : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_homepage)

        val registerButton = findViewById<Button>(R.id.homepage_register_btn)
        val loginButton = findViewById<Button>(R.id.homepage_login_btn)

        registerButton.setOnClickListener {
            val intent = Intent(this, Register::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }

        loginButton.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        }
    }
}
