package com.mobdeve.s18.verify

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class UserAdapter(private val allUsers: MutableList<User>) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    private var displayedUsers: MutableList<User> = allUsers.toMutableList()

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val username: TextView = itemView.findViewById(R.id.tvUsername)
        val email: TextView = itemView.findViewById(R.id.tvEmail)
        val toggleButton: Button = itemView.findViewById(R.id.btnStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = displayedUsers[position]

        holder.username.text = user.username
        holder.email.text = user.email

        holder.toggleButton.text = if (user.isActive) "Active" else "Inactive"
        holder.toggleButton.setBackgroundTintList(
            ColorStateList.valueOf(
                if (user.isActive) Color.parseColor("#4CAF50") else Color.parseColor("#F44336")
            )
        )

        holder.toggleButton.setOnClickListener {
            user.isActive = !user.isActive
            notifyItemChanged(position)
        }
    }

    override fun getItemCount(): Int = displayedUsers.size

    fun filter(query: String) {
        displayedUsers = if (query.isEmpty()) {
            allUsers.toMutableList()
        } else {
            allUsers.filter {
                it.username.contains(query, ignoreCase = true) ||
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

