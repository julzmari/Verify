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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.mobdeve.s18.verify.R
import com.mobdeve.s18.verify.app.VerifiApp

class AdminSettings : AppCompatActivity() {

    private lateinit var profileImageView: ImageView

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val selectedImageUri: Uri? = result.data?.data
            selectedImageUri?.let {
                Glide.with(this)
                    .load(it)
                    .circleCrop()
                    .into(profileImageView)
                Toast.makeText(this, "Profile picture updated!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_settings)

        val changePic = findViewById<TextView>(R.id.changeProfile)
        val changePass = findViewById<TextView>(R.id.changePassword)
        val logout = findViewById<TextView>(R.id.Logout)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav2)
        profileImageView = findViewById(R.id.profilePic)

        changePic.setOnClickListener {
            checkAndRequestPermission()
        }

        changePass.setOnClickListener {
            changePass.setOnClickListener {
                val intent = Intent(this, AdminChangePassword::class.java)
                startActivity(intent)
            }

        }

        logout.setOnClickListener {
            val app = applicationContext as VerifiApp
            app.companyID = null
            app.employeeID = null

            val intent = Intent(this, Homepage::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, AdminDashboardActivity::class.java))
                    true
                }
                R.id.nav_history -> true
                R.id.nav_users -> {
                    startActivity(Intent(this, ManageUser::class.java))
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, AdminSettings::class.java))
                    true
                }
                else -> false
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
