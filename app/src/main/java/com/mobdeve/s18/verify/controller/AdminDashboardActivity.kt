package com.mobdeve.s18.verify.controller

import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.mobdeve.s18.verify.model.UserEntry
import com.mobdeve.s18.verify.app.VerifiApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.mobdeve.s18.verify.R
import com.mobdeve.s18.verify.model.User
import io.github.jan.supabase.postgrest.postgrest

class AdminDashboardActivity : BaseActivity() {

    private lateinit var mapView: MapView
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))

        setContentView(R.layout.activity_admin_dashboard)

        mapView = findViewById(R.id.adminDashboard_mapView)
        recyclerView = findViewById(R.id.admin_dashboard_recyclerView)

        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)

        val startPoint = GeoPoint(14.5646, 120.9936)
        mapView.controller.setZoom(15.0)
        mapView.controller.setCenter(startPoint)

        fetchUserData()

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav2)
        setupBottomNavigation(bottomNav, R.id.nav_home)
    }

    private fun fetchUserData() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = applicationContext as VerifiApp
                val supabase = app.supabase

                // Fetch the 10 most recent entries for all users
                val recentEntriesResponse = supabase.postgrest["photos?order=datetime.desc&limit=10"]
                    .select()
                val recentEntries = recentEntriesResponse.decodeList<UserEntry>()

                // Fetch all users
                val allUsersLocationsResponse = supabase.postgrest["users"]
                    .select()
                val allUsers = allUsersLocationsResponse.decodeList<User>()

                val entries = recentEntries

                withContext(Dispatchers.Main) {

                    addUserMarkersToMap(allUsers, recentEntries)

                    recyclerView.layoutManager = LinearLayoutManager(this@AdminDashboardActivity)
                    recyclerView.adapter = UserEntryAdapter(entries) { user ->
                        // Handle user click, if needed
                        val userLocation = GeoPoint(user.latitude, user.longitude)
                        mapView.controller.animateTo(userLocation)

                        mapView.overlays.clear()

                        val marker = Marker(mapView).apply {
                            position = userLocation
                            title = user.username
                            subDescription = "${user.location_name} - ${user.status}"
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            icon = ContextCompat.getDrawable(this@AdminDashboardActivity, org.osmdroid.library.R.drawable.marker_default)
                        }

                        mapView.overlays.add(marker)
                        mapView.invalidate()
                    }
                }
            } catch (e: Exception) {
                Log.e("AdminDashboard", "Error fetching user data: ${e.message}")
            }
        }
    }

    private fun addUserMarkersToMap(allUsers: List<User>, recentEntries: List<UserEntry>) {
        mapView.overlays.clear() // Clear any previous markers

        allUsers.forEach { user ->
            // Find the last submission for the current user
            val lastSubmission = recentEntries.find { it.user_id == user.id }

            // If no submission, set to default GeoPoint or handle it accordingly
            val userPoint = if (lastSubmission != null) {
                GeoPoint(lastSubmission.latitude, lastSubmission.longitude)
            } else {
                GeoPoint(14.5646, 120.9936) // Default location
            }

            // Create the marker for the user
            val marker = Marker(mapView).apply {
                position = userPoint
                title = user.name
                subDescription = "${user.name} - ${lastSubmission?.status ?: "Unknown"}"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                icon = ContextCompat.getDrawable(this@AdminDashboardActivity, org.osmdroid.library.R.drawable.marker_default)
            }

            // Add the marker to the map
            mapView.overlays.add(marker)
        }

        // Invalidate the map to ensure new markers are rendered
        mapView.invalidate()
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