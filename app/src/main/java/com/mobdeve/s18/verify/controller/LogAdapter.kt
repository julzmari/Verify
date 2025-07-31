package com.mobdeve.s18.verify.adapter

import android.os.Build
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.mobdeve.s18.verify.R
import com.mobdeve.s18.verify.controller.AppLogger
import com.mobdeve.s18.verify.model.Logs
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class LogAdapter(
    private val logs: List<Logs>
) : RecyclerView.Adapter<LogAdapter.LogViewHolder>() {

    inner class LogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tag: TextView = itemView.findViewById(R.id.tvLogTag)
        val date: TextView = itemView.findViewById(R.id.tvLogDate)
        val text: TextView = itemView.findViewById(R.id.tvLogText)

        @RequiresApi(Build.VERSION_CODES.O)
        fun bind(log: Logs) {
            val context = itemView.context

            tag.text = log.tag
            date.text = formatDate(log.date)
            text.text = if (log.text.length > 30) log.text.take(30) + "..." else log.text

            itemView.setOnClickListener {
                val dialogView = LayoutInflater.from(context).inflate(R.layout.popup_log_details, null)
                val dialog = androidx.appcompat.app.AlertDialog.Builder(context).setView(dialogView).create()

                val tagFull = dialogView.findViewById<TextView>(R.id.fullLogTag)
                val dateFull = dialogView.findViewById<TextView>(R.id.fullLogDate)
                val textFull = dialogView.findViewById<TextView>(R.id.fullLogText)
                val closeBtn = dialogView.findViewById<Button>(R.id.btnCloseLog)

                tagFull.text = log.tag
                dateFull.text = formatDate(log.date)
                textFull.text = log.text

                closeBtn.setOnClickListener {
                    dialog.dismiss()
                }

                dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
                dialog.show()
            }
        }

        @RequiresApi(Build.VERSION_CODES.O)
        private fun formatDate(raw: String): String {
            return try {
                val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
                val parsedDateTime = LocalDateTime.parse(raw, inputFormatter)
                val outputFormatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy hh:mm a", Locale.getDefault())
                parsedDateTime.format(outputFormatter)
            } catch (e: Exception) {
                AppLogger.e("LogAdapter", "Date formatting failed: ${e.message}")
                "Invalid date"
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_log, parent, false)
        return LogViewHolder(view)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        holder.bind(logs[position])
    }

    override fun getItemCount(): Int = logs.size
}