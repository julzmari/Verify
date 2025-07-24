package com.mobdeve.s18.verify.controller

import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.mobdeve.s18.verify.R
import com.mobdeve.s18.verify.app.VerifiApp
import com.mobdeve.s18.verify.model.User
import com.mobdeve.s18.verify.model.UserEntry
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import kotlinx.coroutines.withContext

class SubmissionHistory :BaseActivity() {

    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Configuration.getInstance().load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))

        setContentView(R.layout.activity_submissionhistory)
        val app = applicationContext as VerifiApp
        val supabase = app.supabase
        val role = app.authorizedRole
        var userEntries = listOf<UserEntry>()

        val bottomNavbar = findViewById<BottomNavigationView>(R.id.bottomNav)

        if (role == "worker") {
            bottomNavbar.inflateMenu(R.menu.bottom_navbar)
        } else {
            bottomNavbar.inflateMenu(R.menu.bottom_navbar2)
        }
        setupBottomNavigation(bottomNavbar, R.id.nav_history)


        // Optional: Set correct nav item as selected
        //bottomNavbar.selectedItemId = R.id.nav_history

        CoroutineScope(Dispatchers.IO).launch {

            try {

                if (role == "worker") {
                    val employeeID = app.employeeID
                    val userResponse = supabase.postgrest["users?id=eq.$employeeID"].select()
                    val user = userResponse.decodeList<User>().firstOrNull()
                    app.username = user?.name
                    val userEntriesResponse = supabase.postgrest["photos?user_id=eq.$employeeID&order=datetime.desc"]
                        .select()
                    userEntries = userEntriesResponse.decodeList<UserEntry>()
                } else {
                    val companyID = app.companyID
                    val userEntriesResponse = supabase.postgrest["photos?company_id=eq.$companyID&order=datetime.desc"]
                        .select()
                    userEntries = userEntriesResponse.decodeList<UserEntry>()
                }

                withContext(Dispatchers.Main) {
                    recyclerView.adapter = UserEntryAdapter(userEntries) {  }
                }

            } catch (e: Exception) {
                Log.e("Supabase", "Error fetching user/submissions: ${e.message}")
            }
        }

        recyclerView = findViewById(R.id.submission_history_recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = UserEntryAdapter(userEntries) {  }
    }
}
