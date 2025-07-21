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
import androidx.recyclerview.widget.RecyclerView
import com.mobdeve.s18.verify.R
import com.mobdeve.s18.verify.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale


import com.mobdeve.s18.verify.app.VerifiApp
import java.util.*
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.*


class UserAdapter(private val allUsers: MutableList<User>) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    private var displayedUsers: MutableList<User> = allUsers.toMutableList()

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val username: TextView = itemView.findViewById(R.id.tvUsername)
        val email: TextView = itemView.findViewById(R.id.tvEmail)
        val toggleButton: Button = itemView.findViewById(R.id.btnStatus)
        val createdAt: TextView = itemView.findViewById(R.id.tvCreatedAt)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    private fun updateUserStatusInSupabase(user: User, context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val supabase = (context.applicationContext as VerifiApp).supabase

                val response = supabase.postgrest["users?id=eq.${user.id}"]
                    .update(
                        mapOf("is_active" to user.isActive)
                    )

                //.decodeList<User>() // optional, if you expect a return value

                Log.d("Supabase", "Update response: $response")


            } catch (e: Exception) {
                Log.e("Supabase", "Exception during update: ${e.message}")
            }
        }
    }







    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = displayedUsers[position]

        holder.username.text = user.name
        holder.email.text = user.email

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        holder.createdAt.text = "Joined on: ${dateFormat.format(user.createdAt)}"

        holder.toggleButton.text = if (user.isActive) "Active" else "Inactive"
        holder.toggleButton.setBackgroundTintList(
            ColorStateList.valueOf(
                if (user.isActive) Color.parseColor("#4CAF50") else Color.parseColor("#F44336")
            )
        )

        holder.toggleButton.setOnClickListener {
            //val user = displayedUsers[position]
            val updatedUser = user.copy(isActive = !user.isActive)

            // Update UI immediately
            displayedUsers[position] = updatedUser
            notifyItemChanged(position)

            // Call a function to update in Supabase
            updateUserStatusInSupabase(updatedUser, holder.itemView.context)
        }

    }


    override fun getItemCount(): Int = displayedUsers.size

    fun filter(query: String) {
        displayedUsers = if (query.isEmpty()) {
            allUsers.toMutableList()
        } else {
            allUsers.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.email.contains(query, ignoreCase = true)
            }.toMutableList()
        }
        notifyDataSetChanged()
    }

    fun filterByStatus(showActive: Boolean?) {
        displayedUsers = when (showActive) {
            true -> allUsers.filter { it.isActive }.toMutableList()
            false -> allUsers.filter { !it.isActive }.toMutableList()
            null -> allUsers.toMutableList()
        }
        notifyDataSetChanged()
    }

    fun addUser(user: User) {
        allUsers.add(user)
        displayedUsers.add(user)
        notifyItemInserted(displayedUsers.size - 1)
    }
}

