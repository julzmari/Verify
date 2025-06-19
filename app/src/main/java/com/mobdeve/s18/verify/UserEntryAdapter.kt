package com.mobdeve.s18.verify

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class UserEntryAdapter(
    private val users: List<UserEntry>,
    private val onItemClick: (UserEntry) -> Unit
) : RecyclerView.Adapter<UserEntryAdapter.UserEntryViewHolder>() {

    inner class UserEntryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val username = itemView.findViewById<TextView>(R.id.tvUsername)
        val location = itemView.findViewById<TextView>(R.id.tvLocation)
        val datetime = itemView.findViewById<TextView>(R.id.tvDateTime)
        val viewPhoto = itemView.findViewById<Button>(R.id.btnViewPhoto)

        fun bind(user: UserEntry) {
            username.text = user.username
            location.text = user.locationName
            datetime.text = user.datetime

            itemView.setOnClickListener {
                onItemClick(user)
            }

            viewPhoto.setOnClickListener {
                val context = itemView.context  // Get context safely

                val dialogView = LayoutInflater.from(context).inflate(R.layout.popup_user_location_image, null)
                val dialog = androidx.appcompat.app.AlertDialog.Builder(context).setView(dialogView).create()

                val imageView = dialogView.findViewById<ImageView>(R.id.popup_image)
                val locationText = dialogView.findViewById<TextView>(R.id.popup_location)
                val datetimeText = dialogView.findViewById<TextView>(R.id.popup_datetime)
                val closeBtn = dialogView.findViewById<Button>(R.id.btn_close)

                // Load static or dynamic image
                imageView.setImageResource(R.drawable.sample_image)

                locationText.text = "Location: ${user.locationName}"
                datetimeText.text = "Date: ${user.datetime}"

                closeBtn.setOnClickListener {
                    dialog.dismiss()
                }

                dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
                dialog.show()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserEntryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_activity, parent, false)
        return UserEntryViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserEntryViewHolder, position: Int) {
        holder.bind(users[position])
    }

    override fun getItemCount(): Int = users.size
}
