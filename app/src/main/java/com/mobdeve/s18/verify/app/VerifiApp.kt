package com.mobdeve.s18.verify.app

import android.app.Application
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.GoTrue
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

class VerifiApp : Application() {
    lateinit var supabase: SupabaseClient
    var companyID: String? = null
    var employeeID: String? = null
    var username: String? = null
    var location: String? = null
    var latitude: Double? = null
    var longitude: Double? = null
    var authorizedRole: String? = null //owner, admin, worker

    companion object {
        lateinit var instance: VerifiApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        supabase = createSupabaseClient(
            supabaseUrl = "https://lkjkgesqyrjodxzujkgg.supabase.co",
            supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImxramtnZXNxeXJqb2R4enVqa2dnIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTI0OTA4NzgsImV4cCI6MjA2ODA2Njg3OH0.adifn-tLWzDD8moDdjHaXHJGkMEpePgfeGVv9bFVcEI"
        ) {
            install(GoTrue){
                alwaysAutoRefresh = true
                autoLoadFromStorage = true
            }
            install(Postgrest)
            install(Storage)
        }
    }
}
