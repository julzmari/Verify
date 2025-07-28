package com.mobdeve.s18.verify.controller

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
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


class AdminSettings : BaseActivity() {

    private lateinit var profileImageView: ImageView
    private lateinit var nameTextView: TextView
    private lateinit var emailTextView: TextView
    private lateinit var lastLoginTxt: TextView
    private lateinit var lastFailedLoginTxt: TextView

    private var currentUserId: String? = null
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val selectedImageUri: Uri? = result.data?.data
            selectedImageUri?.let { uri ->
                if (uri.scheme != "content" && uri.scheme != "file") {
                    Toast.makeText(this, "Invalid image source", Toast.LENGTH_SHORT).show()
                    Log.w("IMAGE_SECURITY", "Rejected URI with scheme: ${uri.scheme}")
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
        val app = applicationContext as VerifiApp
        val role = app.authorizedRole

        if (role != "admin" && role != "owner") {
            Log.w("ACCESS_CONTROL", "Unauthorized role tried to access AdminSettings: $role")
            Toast.makeText(this, "Access denied.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_settings)

        profileImageView = findViewById(R.id.profilePic)
        nameTextView = findViewById(R.id.tvName)
        emailTextView = findViewById(R.id.tvEmail)

        if (role == "owner") {
            fetchCompanyDetails(app.companyID ?: return)
        } else if (role == "admin") {
            currentUserId = app.employeeID
            currentUserId?.let { fetchUserDetails(it) }
        }

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

            val intent = Intent(this, Homepage::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
        lastLoginTxt = findViewById(R.id.latest_login_txt)
        lastFailedLoginTxt = findViewById(R.id.latest__failed_login_txt)

    }

    private fun fetchCompanyDetails(companyId: String) {
        val supabase = (application as VerifiApp).supabase
        val json = Json { ignoreUnknownKeys = true }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val result = supabase.postgrest
                    .from("companies")
                    .select {
                        eq("id", companyId)
                        limit(1)
                    }

                val companies = json.decodeFromString<List<Company>>(result.body.toString())
                val company = companies.firstOrNull()

                company?.let {
                    withContext(Dispatchers.Main) {
                        nameTextView.text = it.name
                        emailTextView.text = it.email

                        emailTextView.setOnClickListener {
                            AlertDialog.Builder(this@AdminSettings)
                                .setTitle("Company Email")
                                .setMessage(company.email)
                                .setPositiveButton("OK", null)
                                .show()
                        }

                        it.profileURL?.let { url ->
                            Glide.with(this@AdminSettings).load(url).circleCrop().into(profileImageView)
                        }

                        val lastLoginStr = it.last_login?.toString()
                        val lastFailedLoginStr = it.last_failed_login?.toString()

                        lastLoginTxt.text = "Last Login: ${formatTimestampRaw(lastLoginStr)}"
                        lastFailedLoginTxt.text = "Last Failed Login: ${formatTimestampRaw(lastFailedLoginStr)}"

                    }
                }

            } catch (e: Exception) {
                Log.e("FETCH_ERROR", "Company fetch error", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AdminSettings, "Something went wrong. Please try again later.", Toast.LENGTH_SHORT).show()
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
                        emailTextView.text = it.email

                        emailTextView.setOnClickListener {
                            AlertDialog.Builder(this@AdminSettings)
                                .setTitle("Admin Email")
                                .setMessage(user.email)
                                .setPositiveButton("OK", null)
                                .show()
                        }

                        it.profileURL?.let { url ->
                            Glide.with(this@AdminSettings).load(url).circleCrop().into(profileImageView)
                        }

                        val lastLoginStr = it.last_login?.toString()
                        val lastFailedLoginStr = it.last_failed_login?.toString()

                        lastLoginTxt.text = "Last Login: ${formatTimestampRaw(lastLoginStr)}"
                        lastFailedLoginTxt.text = "Last Failed Login: ${formatTimestampRaw(lastFailedLoginStr)}"

                    }
                }

            } catch (e: Exception) {
                Log.e("FETCH_ERROR", "User fetch error", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AdminSettings, "Something went wrong. Please try again later.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun storeProfileUrl(url: String) {
        val supabase = (application as VerifiApp).supabase
        val app = application as VerifiApp
        val role = app.authorizedRole
        val companyId = app.companyID
        val employeeId = app.employeeID

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                if (role == "owner" && companyId != null) {
                    supabase.postgrest.from("companies").update(
                        mapOf("profileURL" to url)
                    ) {
                        eq("id", companyId)
                    }
                } else if (role == "admin" && employeeId != null) {
                    supabase.postgrest.from("users").update(
                        mapOf("profileURL" to url)
                    ) {
                        eq("id", employeeId)
                    }
                }
            } catch (e: Exception) {
                Log.e("UPDATE_ERROR", "Profile URL update failed", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AdminSettings, "Failed to update profile picture. Try again later.", Toast.LENGTH_SHORT).show()
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
            Log.w("PERMISSION", "Storage permission denied by user.")
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }
}