package com.mobdeve.s18.verify.controller

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.mobdeve.s18.verify.R
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class EmployeeDashboard : BaseActivity() {

    private lateinit var mapView: MapView
    private lateinit var welcomeText: TextView
    private lateinit var submissionTv: TextView
    private lateinit var currentLocation: TextView

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))
        setContentView(R.layout.activity_employeedashboard)

        val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val username = sharedPref.getString("loggedInUser", "Guest")

        welcomeText = findViewById<TextView>(R.id.welcomeText)
        submissionTv = findViewById<TextView>(R.id.submissionTv)
        currentLocation = findViewById<TextView>(R.id.currentLocationTv)

        welcomeText.text = "Welcome, $username!"
        submissionTv.text = "Today's Submissions: 0"
        currentLocation.text = " Current Location: De La Salle University"

        mapView = findViewById(R.id.mapView)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)

        val startPoint = GeoPoint(14.5646, 120.9936)
        val mapController = mapView.controller
        mapController.setZoom(15.0)
        mapController.setCenter(startPoint)

        val marker = Marker(mapView)
        marker.position = startPoint
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.title = "Delivery Zone"
        mapView.overlays.add(marker)

        val cameraIcon = findViewById<FrameLayout>(R.id.captureBtn)
        cameraIcon.setOnClickListener {
            val intent = Intent(this, UserCamera::class.java)
            startActivity(intent)
        }

        // Bottom Navigation setup
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        setupBottomNavigation(bottomNav, R.id.nav_home)

    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDetach()
    }
}


