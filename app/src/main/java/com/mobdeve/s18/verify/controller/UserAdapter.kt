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
import kotlinx.datetime.toJavaInstant


import com.mobdeve.s18.verify.app.VerifiApp
import java.util.*
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.*


class UserAdapter(
    private var users: MutableList<User>,
    private val onUserClick: (User) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {


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

    override fun getItemCount(): Int {
        Log.d("UserAdapter", "Item count: ${users.size}")
        return users.size
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        Log.d("UserAdapter", "Binding user at position $position: ${user.name}")

        holder.username.text = user.name
        holder.email.text = user.email
        holder.role.text = "Role: ${user.role}"

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

        holder.toggleButton.setOnClickListener {
            val updatedUser = user.copy(isActive = !user.isActive)
            users[position] = updatedUser
            notifyItemChanged(position)
            updateUserStatusInSupabase(updatedUser, holder.itemView.context)
        }

        holder.itemView.setOnClickListener {
            onUserClick(user)
        }
    }
    private fun updateUserStatusInSupabase(user: User, context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val supabase = (context.applicationContext as VerifiApp).supabase
                val response = supabase.postgrest["users?id=eq.${user.id}"]
                    .update(mapOf("is_active" to user.isActive))
                Log.d("Supabase", "Update response: $response")
            } catch (e: Exception) {
                Log.e("Supabase", "Exception during update: ${e.message}")
            }
        }
    }

    // 🔄 Use this from ManageUser to update the list
    fun setUsers(newUsers: List<User>) {
        this.users = newUsers.toMutableList()
        notifyDataSetChanged()
    }

    fun addUser(user: User) {
        users.add(user)
        notifyItemInserted(users.size - 1)
    }

    fun filter(query: String) {
        users = users.filter {
            it.name.contains(query, ignoreCase = true) || it.email.contains(query, ignoreCase = true)
        }.toMutableList()
        notifyDataSetChanged()
    }

    fun filterByStatus(showActive: Boolean?) {
        users = when (showActive) {
            true -> users.filter { it.isActive }.toMutableList()
            false -> users.filter { !it.isActive }.toMutableList()
            null -> users.toMutableList()
        }
        notifyDataSetChanged()
    }
}