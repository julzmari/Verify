package com.mobdeve.s18.verify.controller

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.mobdeve.s18.verify.R
import com.mobdeve.s18.verify.app.VerifiApp
import com.mobdeve.s18.verify.model.User
import java.util.*
import org.mindrot.jbcrypt.BCrypt
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import kotlinx.serialization.json.JsonObject


class AddUser : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_user)

        val nameInput = findViewById<EditText>(R.id.Username)
        val emailInput = findViewById<EditText>(R.id.Email)
        val passwordInput = findViewById<EditText>(R.id.Pass)
        val confirmPasswordInput = findViewById<EditText>(R.id.confirmPass)
        val roleInput = findViewById<Spinner>(R.id.userRoleSpinner)
        val addUserButton = findViewById<Button>(R.id.addUser)
        val returnButton = findViewById<TextView>(R.id.returnText)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav2)
        setupBottomNavigation(bottomNav, 0)

        val currentRole = (application as VerifiApp).authorizedRole
        val availableRoles = if (currentRole == "admin") {
            listOf("Regular Worker") // Only allow creating reg_employee
        } else {
            listOf("Admin", "Regular Worker")
        }

        val roleAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, availableRoles)

        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        roleInput.adapter = roleAdapter

        returnButton.setOnClickListener {
            startActivity(Intent(this, ManageUser::class.java))
        }


        addUserButton.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val email = emailInput.text.toString().trim().lowercase()
            val password = passwordInput.text.toString()
            val confirmPassword = confirmPasswordInput.text.toString()
            val selectedRole = roleInput.selectedItem.toString()
            val role = when (selectedRole) {
                "Admin" -> "admin"
                "Regular Worker" -> "reg_employee"
                else -> {
                    Toast.makeText(this, "Invalid role selected", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }


            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || role.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val app = applicationContext as VerifiApp
            val companyID = app.companyID

            if (companyID == null) {
                Toast.makeText(this, "Company ID is missing!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
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
                            Toast.makeText(this@AddUser, "Email is already registered.", Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }

                    val newUser = User(
                        id = UUID.randomUUID().toString(),
                        companyID = companyID,
                        role = role,
                        name = name,
                        email = email,
                        password = hashedPassword,
                        isActive = true,
                        createdAt = Clock.System.now()
                    )

                    val result = supabase.postgrest["users"].insert(newUser)


                    runOnUiThread {
                        Toast.makeText(this@AddUser, "User added successfully!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@AddUser, ManageUser::class.java))
                        finish()
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    runOnUiThread {
                        Toast.makeText(this@AddUser, "Failed to add user: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}