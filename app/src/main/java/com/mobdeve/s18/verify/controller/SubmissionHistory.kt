package com.mobdeve.s18.verify.controller

import android.annotation.SuppressLint
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
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.mobdeve.s18.verify.R
import com.mobdeve.s18.verify.app.VerifiApp
import com.mobdeve.s18.verify.model.User
import com.mobdeve.s18.verify.model.UserEntry
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class SubmissionHistory : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var userEntries: List<UserEntry>
    private lateinit var userEntriesFiltered: MutableList<UserEntry>
    private var selectedFromDate: String? = null
    private var selectedToDate: String? = null
    private val selectedStatuses: MutableSet<String> = mutableSetOf()
    private var sortOrder: String = "desc"

    private lateinit var emptyMessageText: TextView

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_submissionhistory)

        val app = applicationContext as VerifiApp
        val supabase = app.supabase
        val role = app.authorizedRole

        val bottomNavbar = findViewById<BottomNavigationView>(R.id.bottomNav)
        if (role == "worker") {
            bottomNavbar.inflateMenu(R.menu.bottom_navbar)
        } else {
            bottomNavbar.inflateMenu(R.menu.bottom_navbar2)
        }
        setupBottomNavigation(bottomNavbar, R.id.nav_history)

        recyclerView = findViewById(R.id.submission_history_recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        emptyMessageText = findViewById(R.id.emptySubmissionMessageText)

        userEntriesFiltered = mutableListOf()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (role == "worker") {
                    val employeeID = app.employeeID
                    val userResponse = supabase.postgrest["users?id=eq.$employeeID"].select()
                    val user = userResponse.decodeList<User>().firstOrNull()
                    app.username = user?.name
                    val userEntriesResponse = supabase.postgrest["photos?user_id=eq.$employeeID&order=datetime.$sortOrder"]
                        .select()
                    userEntries = userEntriesResponse.decodeList<UserEntry>()
                } else {
                    val companyID = app.companyID
                    val userEntriesResponse = supabase.postgrest["photos?company_id=eq.$companyID&order=datetime.$sortOrder"]
                        .select()
                    userEntries = userEntriesResponse.decodeList<UserEntry>()
                }

                userEntriesFiltered.addAll(userEntries)

                withContext(Dispatchers.Main) {
                    recyclerView.adapter = UserEntryAdapter(userEntriesFiltered) { }
                }
            } catch (e: Exception) {
                AppLogger.e("Supabase", "Error fetching user/submissions: ${e.message}")
            }
        }

        val searchInput = findViewById<EditText>(R.id.searchInput_submissionhistory)
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(editable: Editable?) {
                val query = editable.toString().lowercase()
                applyFilters(query)
            }
        })

        val filterButton = findViewById<ImageView>(R.id.ivFilter)
        filterButton.setOnClickListener {
            showFilterDialog()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SetTextI18n")
    private fun showFilterDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_submission_filter, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).create()

        val statusDeliveryCheckBox = dialogView.findViewById<CheckBox>(R.id.statusDelivery)
        val statusInTransitCheckBox = dialogView.findViewById<CheckBox>(R.id.statusInTransit)
        val statusUnexpectedCheckBox = dialogView.findViewById<CheckBox>(R.id.statusUnexpected)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelBtn)
        val submitButton = dialogView.findViewById<Button>(R.id.submitBtn)
        val clearFiltersButton = dialogView.findViewById<Button>(R.id.clearBtn)

        val selectFromDateButton = dialogView.findViewById<Button>(R.id.selectFromDateBtn)
        val selectedFromDateText = dialogView.findViewById<TextView>(R.id.selectedFromDateText)
        selectedFromDateText.text = selectedFromDate?.let { formatForDisplay(it) } ?: "No date selected"
        selectFromDateButton.setOnClickListener {
            showDateTimePickerDialog { dateTime ->
                selectedFromDate = dateTime
                selectedFromDateText.text = formatForDisplay(dateTime)
            }
        }

        val selectToDateButton = dialogView.findViewById<Button>(R.id.selectToDateBtn)
        val selectedToDateText = dialogView.findViewById<TextView>(R.id.selectedToDateText)
        selectedToDateText.text = selectedToDate?.let { formatForDisplay(it) } ?: "No date selected"
        selectToDateButton.setOnClickListener {
            showDateTimePickerDialog { dateTime ->
                selectedToDate = dateTime
                selectedToDateText.text = formatForDisplay(dateTime)
            }
        }

        val sortAscendingRadioButton = dialogView.findViewById<RadioButton>(R.id.sortAscending)
        val sortDescendingRadioButton = dialogView.findViewById<RadioButton>(R.id.sortDescending)

        if (sortOrder == "asc") {
            sortAscendingRadioButton.isChecked = true
        } else {
            sortDescendingRadioButton.isChecked = true
        }

        sortAscendingRadioButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                sortOrder = "asc"
                sortDescendingRadioButton.isChecked = false
            }
        }

        sortDescendingRadioButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                sortOrder = "desc"
                sortAscendingRadioButton.isChecked = false
            }
        }

        statusDeliveryCheckBox.isChecked = selectedStatuses.contains("Delivery")
        statusInTransitCheckBox.isChecked = selectedStatuses.contains("In-transit")
        statusUnexpectedCheckBox.isChecked = selectedStatuses.contains("Unexpected Stop")

        clearFiltersButton.setOnClickListener {
            selectedFromDate = null
            selectedToDate = null
            selectedStatuses.clear()
            selectedFromDateText.text = "No date selected"
            selectedToDateText.text = "No date selected"
            statusDeliveryCheckBox.isChecked = false
            statusInTransitCheckBox.isChecked = false
            statusUnexpectedCheckBox.isChecked = false
            sortOrder = "desc"
            sortDescendingRadioButton.isChecked = true
            sortAscendingRadioButton.isChecked = false
        }

        submitButton.setOnClickListener {
            selectedStatuses.clear()
            if (statusDeliveryCheckBox.isChecked) selectedStatuses.add("Delivery")
            if (statusInTransitCheckBox.isChecked) selectedStatuses.add("In-transit")
            if (statusUnexpectedCheckBox.isChecked) selectedStatuses.add("Unexpected Stop")

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
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val timePickerDialog = TimePickerDialog(this, { _, selectedHour, selectedMinute ->
                val selectedDateTime = LocalDateTime.of(selectedYear, selectedMonth + 1, selectedDay, selectedHour, selectedMinute)
                val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault())
                val formattedDateTime = selectedDateTime.format(dateTimeFormatter)
                onDateTimeSelected(formattedDateTime)
            }, hour, minute, false)
            timePickerDialog.show()
        }, year, month, day)
        datePickerDialog.show()
    }

    private fun applyFilters(query: String = "") {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault())

        val filteredList = userEntries.filter {
            val matchesSearch = it.username.lowercase().contains(query) ||
                    it.location_name.lowercase().contains(query)

            val matchesStatus = selectedStatuses.isEmpty() ||
                    selectedStatuses.contains(it.status)

            val entryDate = try {
                dateFormat.parse(it.datetime)
            } catch (e: Exception) {
                null
            }

            val fromDate = selectedFromDate?.let { dateFormat.parse(it) }
            val toDate = selectedToDate?.let { dateFormat.parse(it) }

            val matchesFromDate = selectedFromDate.isNullOrEmpty() ||
                    (entryDate != null && fromDate != null && entryDate >= fromDate)

            val matchesToDate = selectedToDate.isNullOrEmpty() ||
                    (entryDate != null && toDate != null && entryDate <= toDate)

            matchesSearch && matchesStatus && matchesFromDate && matchesToDate
        }

        val sortedList = if (sortOrder == "asc") {
            filteredList.sortedBy { it.datetime }
        } else {
            filteredList.sortedByDescending { it.datetime }
        }

        if (filteredList.isEmpty()) {
            emptyMessageText.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyMessageText.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }

        recyclerView.adapter = UserEntryAdapter(sortedList) { }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun formatForDisplay(supabaseDateTime: String): String {
        return try {
            val supabaseFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
            val dateTime = LocalDateTime.parse(supabaseDateTime, supabaseFormat)
            DateTimeFormatter.ofPattern("MM-dd-yyyy hh:mm a").format(dateTime)
        } catch (e: Exception) {
            AppLogger.e("DateFormat", "Error formatting date: $supabaseDateTime ${e.message}")
            "Invalid date"
        }
    }
}