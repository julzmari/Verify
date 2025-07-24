package com.mobdeve.s18.verify.controller

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.mobdeve.s18.verify.R
import com.mobdeve.s18.verify.app.VerifiApp
import com.mobdeve.s18.verify.model.Company
import com.mobdeve.s18.verify.model.User
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class AdminSettings : BaseActivity() {

    private lateinit var profileImageView: ImageView
    private lateinit var nameTextView: TextView
    private lateinit var companyNameTextView: TextView
    private var currentUserId: String? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val selectedImageUri: Uri? = result.data?.data
            selectedImageUri?.let { uri ->
                // Display
                Glide.with(this)
                    .load(uri)
                    .circleCrop()
                    .into(profileImageView)

                // Store in DB
                storeProfileUrl(uri.toString())
                Toast.makeText(this, "Profile picture updated!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_settings)

        profileImageView = findViewById(R.id.profilePic)
        nameTextView = findViewById(R.id.adminName)
        companyNameTextView = findViewById(R.id.companyID)

        val app = applicationContext as VerifiApp
        currentUserId = app.employeeID

        currentUserId?.let {
            fetchUserDetails(it)
        }

        val changePic = findViewById<TextView>(R.id.changeProfile)
        val changePass = findViewById<TextView>(R.id.changePassword)
        val logout = findViewById<TextView>(R.id.Logout)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav2)
        setupBottomNavigation(bottomNav, R.id.nav_settings)

        changePic.setOnClickListener {
            checkAndRequestPermission()
        }

        changePass.setOnClickListener {
            startActivity(Intent(this, ChangePassword::class.java))
        }

        logout.setOnClickListener {
            app.companyID = null
            app.employeeID = null
            app.authorizedRole = null

            val intent = Intent(this, Homepage::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    private fun fetchUserDetails(userId: String) {
        val supabase = (application as VerifiApp).supabase
        val json = Json { ignoreUnknownKeys = true }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val result = supabase.postgrest
                    .from("users")
                    .select {
                        eq("id", userId)
                        limit(1)
                    }

                val users = json.decodeFromString<List<User>>(result.body.toString())
                val user = users.firstOrNull()

                user?.let {
                    // Update name
                    withContext(Dispatchers.Main) {
                        nameTextView.text = it.name
                        // Load profile photo if exists
                        it.profileURL?.let { url ->
                            Glide.with(this@AdminSettings).load(url).circleCrop().into(profileImageView)
                        }
                    }

                    // Now fetch company name using companyID
                    val companyResult = supabase.postgrest
                        .from("companies")
                        .select {
                            eq("id", it.companyID)
                            limit(1)
                        }

                    val companies = json.decodeFromString<List<Company>>(companyResult.body.toString())
                    val company = companies.firstOrNull()

                    company?.let { comp ->
                        withContext(Dispatchers.Main) {
                            companyNameTextView.text = comp.name
                        }
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AdminSettings, "Failed to load user: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun storeProfileUrl(url: String) {
        val supabase = (application as VerifiApp).supabase
        val userId = currentUserId ?: return

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                supabase.postgrest.from("users").update(
                    mapOf("profileURL" to url)
                ) {
                    eq("id", userId)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AdminSettings, "Failed to store profile picture: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun checkAndRequestPermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_IMAGES
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            openImagePicker()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(permission), 100)
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        pickImageLauncher.launch(intent)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openImagePicker()
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }
}
