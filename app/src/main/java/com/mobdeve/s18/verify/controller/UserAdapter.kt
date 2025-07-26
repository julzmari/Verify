package com.mobdeve.s18.verify.controller

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.mobdeve.s18.verify.R
import com.mobdeve.s18.verify.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.datetime.toJavaInstant


import com.mobdeve.s18.verify.app.VerifiApp
import java.util.*
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.*


class UserAdapter(
    private val displayList: MutableList<User>,
    private val onUserClick: (User) -> Unit,
    private val currentUserRole: String
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    private var allUsers: MutableList<User> = mutableListOf()
    private var filteredStatus: Boolean? = null
    private var currentSearchQuery: String = ""

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val username: TextView = itemView.findViewById(R.id.tvUsername)
        val email: TextView = itemView.findViewById(R.id.tvEmail)
        val toggleButton: Button = itemView.findViewById(R.id.btnStatus)
        val createdAt: TextView = itemView.findViewById(R.id.tvCreatedAt)
        val role: TextView = itemView.findViewById(R.id.tvRole)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun getItemCount(): Int = displayList.size

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = displayList[position]

        holder.username.text = user.name
        holder.email.text = user.email

        val displayRole = when (user.role) {
            "admin" -> "Admin"
            "reg_employee" -> "Regular Worker"
            else -> user.role
        }
        holder.role.text = "Role: $displayRole"

        val millis = user.createdAt.epochSeconds * 1000 + user.createdAt.nanosecondsOfSecond / 1_000_000
        val date = Date(millis)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        holder.createdAt.text = "Joined on: ${dateFormat.format(date)}"

        holder.toggleButton.text = if (user.isActive) "Active" else "Inactive"
        holder.toggleButton.setBackgroundTintList(
            ColorStateList.valueOf(
                if (user.isActive) Color.parseColor("#4CAF50") else Color.parseColor("#F44336")
            )
        )

        val canToggle = when (currentUserRole) {
            "owner" -> true
            "admin" -> user.role == "reg_employee"
            else -> false
        }

        holder.toggleButton.setOnClickListener {
            if (!canToggle) {
                Toast.makeText(holder.itemView.context, "You don't have permission to update this user", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val updatedUser = user.copy(isActive = !user.isActive)
            allUsers[allUsers.indexOfFirst { it.id == updatedUser.id }] = updatedUser
            applyFilters()
            updateUserStatusInSupabase(updatedUser, holder.itemView.context)
        }

        if (currentUserRole == "owner") {
            holder.itemView.setOnClickListener {
                onUserClick(user)
            }
        } else {
            holder.itemView.setOnClickListener(null)
        }
    }

    private fun updateUserStatusInSupabase(user: User, context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val supabase = (context.applicationContext as VerifiApp).supabase
                supabase.postgrest["users?id=eq.${user.id}"]
                    .update(mapOf("isActive" to user.isActive))
            } catch (e: Exception) {
                Log.e("Supabase", "Exception during update: ${e.message}")
            }
        }
    }

    fun setUsers(newUsers: List<User>) {
        allUsers = newUsers.toMutableList()
        filteredStatus = null
        currentSearchQuery = ""
        applyFilters()
    }

    fun addUser(user: User) {
        allUsers.add(user)
        applyFilters()
    }

    fun updateUser(updatedUser: User) {
        val index = allUsers.indexOfFirst { it.id == updatedUser.id }
        if (index != -1) {
            allUsers[index] = updatedUser
            applyFilters()
        }
    }

    fun filterByStatus(isActive: Boolean?) {
        filteredStatus = isActive
        applyFilters()
    }

    fun filter(query: String) {
        currentSearchQuery = query
        applyFilters()
    }

    private fun applyFilters() {
        val statusFiltered = when (filteredStatus) {
            true -> allUsers.filter { it.isActive }
            false -> allUsers.filter { !it.isActive }
            null -> allUsers
        }

        val searchFiltered = if (currentSearchQuery.isNotEmpty()) {
            statusFiltered.filter {
                it.name.contains(currentSearchQuery, ignoreCase = true) ||
                        it.email.contains(currentSearchQuery, ignoreCase = true)
            }
        } else {
            statusFiltered
        }

        displayList.clear()
        displayList.addAll(searchFiltered)
        notifyDataSetChanged()
    }
}
