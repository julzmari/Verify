package com.mobdeve.s18.verify.controller

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.mobdeve.s18.verify.R
import com.mobdeve.s18.verify.adapter.LogAdapter
import com.mobdeve.s18.verify.app.VerifiApp
import com.mobdeve.s18.verify.model.Logs
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class AdminLogs : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var allLogs: List<Logs>
    private var filteredLogs: MutableList<Logs> = mutableListOf()
    private var selectedFromDate: String? = null
    private var selectedToDate: String? = null
    private var sortOrder = "desc"

    private lateinit var emptyMessageText: TextView

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = applicationContext as VerifiApp
        if (app.authorizedRole == "worker") {
            finish()
            return
        }

        setContentView(R.layout.activity_admin_loghistory)

        recyclerView = findViewById(R.id.logsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        emptyMessageText = findViewById(R.id.emptyLogsMessageText)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.inflateMenu(R.menu.bottom_navbar2)
        setupBottomNavigation(bottomNav, R.id.nav_history)

        val searchInput = findViewById<EditText>(R.id.searchInput_logs)
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(editable: Editable?) {
                applyFilters(editable.toString().lowercase())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        findViewById<ImageView>(R.id.ivFilter_logs).setOnClickListener {
            showFilterDialog()
        }

        fetchLogs()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchLogs() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = applicationContext as VerifiApp
                val response = app.supabase.postgrest["logs?companyID=eq.${app.companyID}&order=date.$sortOrder"]
                    .select()

                allLogs = response.decodeList<Logs>()

                filteredLogs = allLogs.toMutableList()

                withContext(Dispatchers.Main) {
                    recyclerView.adapter = LogAdapter(filteredLogs)
                    updateEmptyMessage()
                }
            } catch (e: Exception) {
                AppLogger.e("AdminLogs", "Error loading logs: ${e.message}")
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showFilterDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_log_filter, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).create()

        val cancelButton = dialogView.findViewById<Button>(R.id.logCancelBtn)
        val submitButton = dialogView.findViewById<Button>(R.id.logSubmitBtn)
        val clearFiltersButton = dialogView.findViewById<Button>(R.id.logClearBtn)

        val selectFromDateBtn = dialogView.findViewById<Button>(R.id.logSelectFromDateBtn)
        val fromDateText = dialogView.findViewById<TextView>(R.id.logSelectedFromDateText)
        fromDateText.text = selectedFromDate?.let { formatForDisplay(it) } ?: "No date selected"

        selectFromDateBtn.setOnClickListener {
            showDateTimePickerDialog {
                selectedFromDate = it
                fromDateText.text = formatForDisplay(it)
            }
        }

        val selectToDateBtn = dialogView.findViewById<Button>(R.id.logSelectToDateBtn)
        val toDateText = dialogView.findViewById<TextView>(R.id.logSelectedToDateText)
        toDateText.text = selectedToDate?.let { formatForDisplay(it) } ?: "No date selected"

        selectToDateBtn.setOnClickListener {
            showDateTimePickerDialog {
                selectedToDate = it
                toDateText.text = formatForDisplay(it)
            }
        }

        val sortAscRadio = dialogView.findViewById<RadioButton>(R.id.logSortAscending)
        val sortDescRadio = dialogView.findViewById<RadioButton>(R.id.logSortDescending)

        if (sortOrder == "asc") sortAscRadio.isChecked = true else sortDescRadio.isChecked = true

        sortAscRadio.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                sortOrder = "asc"
                sortDescRadio.isChecked = false
            }
        }

        sortDescRadio.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                sortOrder = "desc"
                sortAscRadio.isChecked = false
            }
        }

        clearFiltersButton.setOnClickListener {
            selectedFromDate = null
            selectedToDate = null
            sortOrder = "desc"
            fromDateText.text = "No date selected"
            toDateText.text = "No date selected"
            sortDescRadio.isChecked = true
            sortAscRadio.isChecked = false
        }

        submitButton.setOnClickListener {
            applyFilters()
            dialog.dismiss()
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showDateTimePickerDialog(onDateTimeSelected: (String) -> Unit) {
        val now = Calendar.getInstance()
        val year = now.get(Calendar.YEAR)
        val month = now.get(Calendar.MONTH)
        val day = now.get(Calendar.DAY_OF_MONTH)
        val hour = now.get(Calendar.HOUR_OF_DAY)
        val minute = now.get(Calendar.MINUTE)

        val datePicker = DatePickerDialog(this, { _, y, m, d ->
            TimePickerDialog(this, { _, h, min ->
                val selected = LocalDateTime.of(y, m + 1, d, h, min)
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
                onDateTimeSelected(selected.format(formatter))
            }, hour, minute, false).show()
        }, year, month, day)
        datePicker.show()
    }

    private fun applyFilters(query: String = "") {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault())

        val filtered = allLogs.filter {
            val matchesSearch = it.tag.lowercase().contains(query) || it.text.lowercase().contains(query)

            val logDate = try { sdf.parse(it.date) } catch (_: Exception) { null }
            val from = selectedFromDate?.let { sdf.parse(it) }
            val to = selectedToDate?.let { sdf.parse(it) }

            val matchesFrom = selectedFromDate.isNullOrEmpty() || (logDate != null && from != null && logDate >= from)
            val matchesTo = selectedToDate.isNullOrEmpty() || (logDate != null && to != null && logDate <= to)

            matchesSearch && matchesFrom && matchesTo
        }

        val sorted = if (sortOrder == "asc") filtered.sortedBy { it.date } else filtered.sortedByDescending { it.date }

        filteredLogs.clear()
        filteredLogs.addAll(sorted)

        recyclerView.adapter = LogAdapter(filteredLogs)
        updateEmptyMessage()
    }

    private fun updateEmptyMessage() {
        if (filteredLogs.isEmpty()) {
            emptyMessageText.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyMessageText.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun formatForDisplay(date: String): String {
        return try {
            val input = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
            val output = DateTimeFormatter.ofPattern("MM-dd-yyyy hh:mm a")
            val dt = LocalDateTime.parse(date, input)
            dt.format(output)
        } catch (e: Exception) {
            "Invalid date"
        }
    }
}