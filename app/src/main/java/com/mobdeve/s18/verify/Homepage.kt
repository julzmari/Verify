package com.mobdeve.s18.verify

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class Homepage : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_homepage)

        val registerButton = findViewById<Button>(R.id.homepage_register_btn)
        val loginButton = findViewById<Button>(R.id.homepage_login_btn)

        registerButton.setOnClickListener {
            startActivity(Intent(this, Register::class.java))
        }

        loginButton.setOnClickListener {
            startActivity(Intent(this, Login::class.java))
        }
    }
}
