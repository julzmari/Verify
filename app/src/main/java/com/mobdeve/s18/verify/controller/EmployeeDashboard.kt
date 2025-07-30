package com.mobdeve.s18.verify.controller

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.mobdeve.s18.verify.R
import com.mobdeve.s18.verify.app.VerifiApp
import com.mobdeve.s18.verify.model.User
import com.mobdeve.s18.verify.model.UserEntry
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneOffset

class EmployeeDashboard : BaseActivity() {

    private lateinit var welcomeText: TextView
    private lateinit var submissionCount: TextView
    private lateinit var currentLocation: TextView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var captureButton: Button

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            getCurrentLocation()
        } else {
            Log.e("Location", "Permission denied")
        }
    }

    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>


    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_employeedashboard)
        Log.i("EmployeeDashboard", "onCreate started")


        welcomeText = findViewById(R.id.welcomeText)
        submissionCount = findViewById(R.id.submissionCount)
        currentLocation = findViewById(R.id.currentLocationTv)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        Log.d("EmployeeDashboard", "Requested location permission")

        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                Log.i("EmployeeDashboard", "Camera activity returned RESULT_OK, refreshing dashboard")
                fetchUserData() // refresh dashboard if submission succeeded

            }else{
                Log.w("EmployeeDashboard", "Camera activity canceled or failed")
            }
        }

        fetchUserData()

        captureButton = findViewById(R.id.captureBtnDashboard)
        captureButton.setOnClickListener {
            Log.i("EmployeeDashboard", "Capture button clicked, launching camera")
            val intent = Intent(this, UserCamera::class.java)
            cameraLauncher.launch(intent)
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        setupBottomNavigation(bottomNav, R.id.nav_home)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchUserData() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = applicationContext as VerifiApp
                val supabase = app.supabase
                val employeeID = app.employeeID

                if (employeeID.isNullOrEmpty()) {
                    Log.e("EmployeeDashboard", "EmployeeID missing! Redirecting to login.")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@EmployeeDashboard, "Unauthorized access. Please login again.", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@EmployeeDashboard, Login::class.java))
                        finish()
                    }
                    return@launch
                }
                Log.d("EmployeeDashboard", "Fetching user info for employeeID=$employeeID")
                val userResponse = supabase.postgrest["users?id=eq.$employeeID"].select()
                val user = userResponse.decodeList<User>().firstOrNull()
                if (user != null) {
                    Log.i("Dashboard", "Successfully fetched user ${user.email}")
                    app.username = user.name
                } else {
                    Log.w("Dashboard", "User not found for employeeID $employeeID")
                }


                val today = LocalDate.now(ZoneOffset.UTC)
                val startOfDay = today.atStartOfDay().toString()
                val endOfDay = today.plusDays(1).atStartOfDay().toString()

                Log.d("EmployeeDashboard", "Querying submissions for today ($startOfDay to $endOfDay)")

                val response = supabase.postgrest["photos?user_id=eq.$employeeID&datetime=gte.$startOfDay&datetime=lt.$endOfDay"]
                    .select()

                val submissionTodayCount = response.decodeList<UserEntry>().size
                Log.i("Dashboard", "Fetched $submissionTodayCount submissions for today.")


                withContext(Dispatchers.Main) {
                    welcomeText.text = "Welcome, ${user?.name ?: "Unknown"}!"
                    submissionCount.text = submissionTodayCount.toString()
                }

            } catch (e: Exception) {
                Log.e("Supabase", "Error fetching user/submissions: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EmployeeDashboard, "An error occurred while fetching your data.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        val app = applicationContext as VerifiApp

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    if (location.latitude !in -90.0..90.0 || location.longitude !in -180.0..180.0) {
                        Log.e("Location", "Invalid coordinates")
                        return@addOnSuccessListener
                    }
                    Log.i("Location", "Successfully retrieved location: ${location.latitude}, ${location.longitude}")


                    val geocoder = Geocoder(this)
                    try {
                        val addressList = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        val locationName = addressList?.firstOrNull()?.getAddressLine(0) ?: "Unknown location"

                        val safeLocationName = locationName.take(150)
                        currentLocation.text = safeLocationName
                        app.location = safeLocationName
                    } catch (e: Exception) {
                        Log.e("Location", "Geocoder error: ${e.message}")
                        currentLocation.text = "Unable to fetch location"
                    }

                    app.longitude = location.longitude
                    app.latitude = location.latitude
                } else {
                    Log.e("Location", "Location is null")
                }
            }
            .addOnFailureListener {
                Log.e("Location", "Error getting location: ${it.message}")
            }
    }
}