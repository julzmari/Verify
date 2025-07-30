package com.mobdeve.s18.verify.repository

import android.util.Log
import com.mobdeve.s18.verify.model.PasswordHistory
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.mindrot.jbcrypt.BCrypt

class PasswordHistoryRepository(private val supabase: SupabaseClient) {

    private val tableName = "password_history"

    /**
     * Fetch last N password history entries for a given user/company.
     */
    suspend fun getLastNHistory(
        userId: String,
        userType: String,
    ): List<PasswordHistory> = withContext(Dispatchers.IO) {
        supabase.postgrest
            .from(tableName)
            .select {
                eq("user_id", userId)
                eq("user_type", userType)
                order("changed_at", Order.DESCENDING)
                limit(3)
            }
            .decodeList<PasswordHistory>()
    }

    /**
     * Check if the new password matches any of the last N hashed passwords.
     */
    suspend fun isPasswordReused(
        userId: String,
        userType: String,
        newPassword: String,
    ): Boolean {
        val recentPasswords = getLastNHistory(userId, userType)
        return recentPasswords.any { BCrypt.checkpw(newPassword, it.password_hash) }
    }

    /**
     * Keep only the latest `keep` history entries (prune old entries).
     */
    suspend fun pruneOldPasswords(userId: String, userType: String, keep: Int = 3) =
        withContext(Dispatchers.IO) {
            // Step 1: Get all IDs ordered by changed_at
            val allIds = supabase.postgrest
                .from(tableName)
                .select(Columns.list("id", "changed_at")) {
                    eq("user_id", userId)
                    eq("user_type", userType)
                    order("changed_at", Order.DESCENDING)
                }
                .decodeList<Map<String, String>>()
                .map { it["id"]!! }

            // Step 2: Compute IDs to delete
            if (allIds.size > keep) {
                val deleteIds = allIds.drop(keep) // keep newest, drop old ones

                // Step 3: Delete them one by one
                for (id in deleteIds) {
                    supabase.postgrest
                        .from(tableName)
                        .delete {
                            eq("id", id)
                        }
                }
            }
        }
}

/**
 * Insert password history with retry, without modifying your working logic.
 */
suspend fun insertPasswordHistoryWithRetry(
    supabase: SupabaseClient,
    userId: String,
    hashedPassword: String,
    userType: String,
    retries: Int = 3
): Boolean {
    repeat(retries) { attempt ->
        try {
            supabase.postgrest["password_history"].insert(
                buildJsonObject {
                    put("user_id", userId)
                    put("user_type", userType)
                    put("password_hash", hashedPassword)
                }
            )
            Log.i("PASSWORD_HISTORY", "Inserted on attempt ${attempt + 1}")
            return true
        } catch (e: Exception) {
            Log.w("PASSWORD_HISTORY", "Attempt ${attempt + 1} failed: ${e.message}")
            delay(500) // short wait before retry
        }
    }
    Log.e("PASSWORD_HISTORY", "All retry attempts failed.")
    return false
}
