package com.mobdeve.s18.verify.controller

import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.mobdeve.s18.verify.R
import com.mobdeve.s18.verify.model.UserEntry
import com.bumptech.glide.Glide
import java.time.format.DateTimeFormatter
import java.time.LocalDateTime
import java.util.*

class UserEntryAdapter(
    private val users: List<UserEntry>,
    private val onItemClick: (UserEntry) -> Unit
) : RecyclerView.Adapter<UserEntryAdapter.UserEntryViewHolder>() {

    inner class UserEntryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val username = itemView.findViewById<TextView>(R.id.tvUsername)
        val location = itemView.findViewById<TextView>(R.id.tvLocation)
        val datetime = itemView.findViewById<TextView>(R.id.tvDateTime)
        val status = itemView.findViewById<TextView>(R.id.tvStatus)

        @RequiresApi(Build.VERSION_CODES.O)
        fun bind(user: UserEntry) {

            username.text = user.username
            location.text = user.location_name
            datetime.text = formatDate(user.datetime)
            status.text = user.status

            val context = itemView.context

            Log.d("UserEntryAdapter", "Binding user entry: username='${user.username}', status='${user.status}'")


            when (user.status.trim().lowercase()) {
                "in-transit" -> status.setBackgroundColor(context.getColor(R.color.InTransit))
                "unexpected stop" -> status.setBackgroundColor(context.getColor(R.color.UnexpectedStop))
                else -> status.setBackgroundColor(context.getColor(R.color.Delivery))
            }

            itemView.setOnClickListener {
                onItemClick(user)

                val dialogView = LayoutInflater.from(context).inflate(R.layout.popup_user_location_image, null)
                val dialog = androidx.appcompat.app.AlertDialog.Builder(context).setView(dialogView).create()

                val imageView = dialogView.findViewById<ImageView>(R.id.popup_image)
                val locationText = dialogView.findViewById<TextView>(R.id.popup_location)
                val datetimeText = dialogView.findViewById<TextView>(R.id.popup_datetime)
                val statusText = dialogView.findViewById<TextView>(R.id.popup_status)
                val closeBtn = dialogView.findViewById<Button>(R.id.btn_close)

                Glide.with(context)
                    .load(user.image_url)
                    .placeholder(R.drawable.sample_image)
                    .into(imageView)

                locationText.text = user.location_name
                statusText.text = user.status
                datetimeText.text = formatDate(user.datetime)

                closeBtn.setOnClickListener {
                    dialog.dismiss()
                }

                dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
                dialog.show()
            }
        }

        @RequiresApi(Build.VERSION_CODES.O)
        fun formatDate(timestamp: String): String {
            try {
                val inputFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
                val dateTime = LocalDateTime.parse(timestamp, inputFormat)
                val outputFormat = DateTimeFormatter.ofPattern("MM-dd-yyyy hh:mm a", Locale.getDefault())
                return dateTime.format(outputFormat)
            } catch (e: Exception) {
                Log.e("DateFormatError", "Error parsing or formatting the date: ${e.message}")
                return "Invalid date"
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserEntryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_activity, parent, false)
        return UserEntryViewHolder(view)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: UserEntryViewHolder, position: Int) {
        holder.bind(users[position])
    }

    override fun getItemCount(): Int = users.size
}
