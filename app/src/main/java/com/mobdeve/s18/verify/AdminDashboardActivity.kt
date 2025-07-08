package com.mobdeve.s18.verify

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.overlay.Marker
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var recyclerView: RecyclerView
    private lateinit var userEntries: List<UserEntry>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Modern replacement for deprecated PreferenceManager
        Configuration.getInstance().load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))

        setContentView(R.layout.activity_admin_dashboard)

        mapView = findViewById(R.id.adminDashboard_mapView)
        recyclerView = findViewById(R.id.admin_dashboard_recyclerView)

        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)

        val startPoint = GeoPoint(14.5646, 120.9936)
        mapView.controller.setZoom(15.0)
        mapView.controller.setCenter(startPoint)

        // Dummy data
        userEntries = listOf(
            UserEntry("user1", "DLSU Manila", "June 14, 2025 12:00PM", 14.5646, 120.9936, "Delivery"),
            UserEntry("user2", "BGC, Taguig", "June 14, 2025 1:20PM", 14.5515, 121.0490, "Delivery")
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = UserEntryAdapter(userEntries) { user ->
            val userLocation = GeoPoint(user.latitude, user.longitude)
            mapView.controller.animateTo(userLocation)

            // Clear old markers
            mapView.overlays.clear()

            // Add new marker
            val marker = Marker(mapView).apply {
                position = userLocation
                title = user.username
                subDescription = user.locationName + " - " + user.status
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                icon = ContextCompat.getDrawable(this@AdminDashboardActivity, org.osmdroid.library.R.drawable.marker_default)
            }

            mapView.overlays.add(marker)
            mapView.invalidate()
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav2)

        bottomNav.setOnItemSelectedListener  { item ->
           when (item.itemId) {
           //     R.id.nav_home -> {
           //         startActivity(Intent(this, HomeActivity::class.java))
           //         true
           //     }
                R.id.nav_history -> {
                    startActivity(Intent(this, SubmissionHistory::class.java))
                    true
                }
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
