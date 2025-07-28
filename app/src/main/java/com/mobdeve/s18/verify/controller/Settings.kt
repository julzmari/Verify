package com.mobdeve.s18.verify.controller

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
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
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class Settings : BaseActivity() {

    private lateinit var profileImageView: ImageView
    private lateinit var nameTextView: TextView
    private lateinit var companyNameTextView: TextView
    private var currentUserId: String? = null
    private lateinit var lastLoginTxt: TextView
    private lateinit var lastFailedLoginTxt: TextView

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val selectedImageUri: Uri? = result.data?.data
            selectedImageUri?.let { uri ->
                if (uri.scheme != "content" && uri.scheme != "file") {
                    Toast.makeText(this, "Invalid image source", Toast.LENGTH_SHORT).show()
                    Log.w("SECURITY_IMAGE", "Rejected image URI with unsupported scheme: ${uri.scheme}")
                    return@let
                }

                Glide.with(this)
                    .load(uri)
                    .circleCrop()
                    .into(profileImageView)

                storeProfileUrl(uri.toString())
                Toast.makeText(this, "Profile picture updated!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        profileImageView = findViewById(R.id.profilePic)
        nameTextView = findViewById(R.id.userName)
        companyNameTextView = findViewById(R.id.companyID)

        val app = applicationContext as VerifiApp
        currentUserId = app.employeeID

        if (currentUserId.isNullOrEmpty()) {
            Log.w("SECURITY_ACCESS", "Access attempt with null employee ID")
            Toast.makeText(this, "Access denied. Please log in again.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        fetchUserDetails(currentUserId!!)

        val changePic = findViewById<TextView>(R.id.changeProfile)
        val changePass = findViewById<TextView>(R.id.changePassword)
        val logout = findViewById<TextView>(R.id.Logout)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav2)
        setupBottomNavigation(bottomNav, R.id.nav_settings)

        changePic.setOnClickListener { checkAndRequestPermission() }
        changePass.setOnClickListener { startActivity(Intent(this, ChangePassword::class.java)) }

        logout.setOnClickListener {
            app.companyID = null
            app.employeeID = null
            app.authorizedRole = null
            app.location = null
            app.latitude = null
            app.longitude = null
            app.username = null

            val intent = Intent(this, Homepage::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
        lastLoginTxt = findViewById(R.id.latest_login_txt)
        lastFailedLoginTxt = findViewById(R.id.latest__failed_login_txt)
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
                    withContext(Dispatchers.Main) {
                        nameTextView.text = it.name
                        it.profileURL?.let { url ->
                            Glide.with(this@Settings).load(url).circleCrop().into(profileImageView)
                        }
                    }

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
                    val lastLoginStr = it.last_login?.toString()
                    val lastFailedLoginStr = it.last_failed_login?.toString()

                    lastLoginTxt.text = "Last Login: ${formatTimestampRaw(lastLoginStr)}"
                    lastFailedLoginTxt.text = "Last Failed Login: ${formatTimestampRaw(lastFailedLoginStr)}"


                } ?: run {
                    Log.w("FETCH_USER", "No user found for ID: $userId")
                }

            } catch (e: Exception) {
                Log.e("FETCH_USER_ERROR", "Exception during user/company fetch", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@Settings, "Something went wrong. Please try again later.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun formatTimestampRaw(raw: String?): String {
        if (raw == null) return "None"

        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC") // adjust if needed

            val date = inputFormat.parse(raw)

            val outputFormat = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
            outputFormat.timeZone = TimeZone.getTimeZone("Asia/Manila")

            outputFormat.format(date ?: return "Invalid")
        } catch (e: Exception) {
            "Invalid"
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
                Log.e("UPDATE_PROFILE_URL", "Failed to update profile URL", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@Settings, "Failed to update profile picture. Try again later.", Toast.LENGTH_SHORT).show()
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
            Log.w("PERMISSION_DENIED", "User denied image read permission")
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }
}