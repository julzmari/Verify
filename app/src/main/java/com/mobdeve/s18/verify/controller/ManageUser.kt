package com.mobdeve.s18.verify.controller

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.mobdeve.s18.verify.R
import com.mobdeve.s18.verify.model.User
import com.mobdeve.s18.verify.model.UserParcelable




import com.mobdeve.s18.verify.app.VerifiApp
import com.mobdeve.s18.verify.model.toUser
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.*
import kotlinx.serialization.json.JsonObject


class ManageUser : BaseActivity() {

    private lateinit var userAdapter: UserAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var addUserLauncher: ActivityResultLauncher<Intent>
    private val users = mutableListOf<User>()



    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_manage_users)

        recyclerView = findViewById(R.id.rvUsers)
        recyclerView.isNestedScrollingEnabled = true

        val app = application as VerifiApp
        val currentUserRole = app.authorizedRole ?: ""

        userAdapter = UserAdapter(users, { selectedUser ->
            showUpdateUserDialog(selectedUser)
        }, currentUserRole)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = userAdapter

        fetchUsersFromSupabase()

        val searchInput = findViewById<EditText>(R.id.searchInput)
        val filterBtn = findViewById<ImageView>(R.id.ivFilter)
        val addButton = findViewById<ImageView>(R.id.ivAdd)

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                userAdapter.filter(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
        })

        var currentFilter: Boolean? = null
        filterBtn.setOnClickListener {
            currentFilter = when (currentFilter) {
                null -> true
                true -> false
                false -> null
            }
            userAdapter.filterByStatus(currentFilter)
        }

        addUserLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val newUser = result.data?.getParcelableExtra<UserParcelable>("newUser")
                newUser?.let {
                    userAdapter.addUser(it.toUser())
                }
            }
        }

        addButton.setOnClickListener {
            val intent = Intent(this, AddUser::class.java)
            addUserLauncher.launch(intent)
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav2)
        setupBottomNavigation(bottomNav, R.id.nav_users)

        val role = app.authorizedRole

        if (role == "admin") {
            bottomNav.menu.removeItem(R.id.nav_logs)
        }
    }



    @RequiresApi(Build.VERSION_CODES.O)
    private fun showUpdateUserDialog(user: User) {
        val dialogView = layoutInflater.inflate(R.layout.popup_edit_user, null)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val usernameField = dialogView.findViewById<EditText>(R.id.editUsername)
        val emailField = dialogView.findViewById<EditText>(R.id.editEmail)
        val roleSpinner = dialogView.findViewById<Spinner>(R.id.userRoleSpinner)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelbtn)
        val updateButton = dialogView.findViewById<Button>(R.id.updateUserbtn)

        // Pre-fill fields
        usernameField.setText(user.name)
        emailField.setText(user.email)

        // Display labels shown in the spinner
        val roleDisplayNames = listOf("Admin", "Regular Worker")

        // Map from display name to actual role value
        val roleMap = mapOf(
            "Admin" to "admin",
            "Regular Worker" to "reg_employee"
        )

        // Reverse map to set the correct spinner position from user's current role
        val reverseRoleMap = roleMap.entries.associate { (k, v) -> v to k }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, roleDisplayNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        roleSpinner.adapter = adapter

        // Set spinner to current user role
        val currentDisplayRole = reverseRoleMap[user.role] ?: "Regular Worker"
        roleSpinner.setSelection(roleDisplayNames.indexOf(currentDisplayRole))


        cancelButton.setOnClickListener {
            dialog.dismiss()
        }



        updateButton.setOnClickListener {
            val newUsername = usernameField.text.toString().trim()
            val newEmail = emailField.text.toString().trim().lowercase()
            val selectedRole = roleSpinner.selectedItem.toString()
            val newRole = when (selectedRole) {
                "Admin" -> "admin"
                "Regular Worker" -> "reg_employee"
                else -> {
                    Toast.makeText(this, "Invalid role selected", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }



            if (newUsername.isEmpty() || newEmail.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Check if any changes were made
            val isChanged = newUsername != user.name ||
                    newEmail != user.email ||
                    newRole != user.role

            if (!isChanged) {
                Toast.makeText(this, "No changes detected", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }



            // Update the user locally
            val updatedUser = user.copy(
                name = newUsername,
                email = newEmail,
                role = newRole
            )

            userAdapter.updateUser(updatedUser)

            // Perform the update
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val supabase = (application as VerifiApp).supabase

                    // Check if email exists in companies
                    val companyEmailCheck = supabase.postgrest["companies"]
                        .select {
                            eq("email", newEmail)
                        }
                        .decodeList<JsonObject>()


                    // Check if email exists in users
                    val userEmailCheck = supabase.postgrest["users"]
                        .select {
                            eq("email", newEmail)
                            neq("id", user.id) // exclude the current user
                        }
                        .decodeList<JsonObject>()



                    if (companyEmailCheck.isNotEmpty() || userEmailCheck.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@ManageUser, "Email is already registered.", Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }

                    val updatedUsers = supabase.postgrest["users"]
                        .update(
                            mapOf(
                                "name" to newUsername,
                                "email" to newEmail,
                                "role" to newRole
                            )
                        )
                        {eq("id", user.id) }// Ensure this is imported
                        .decodeList<User>()

                    if (updatedUsers.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@ManageUser, "User updated", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@ManageUser,
                                "Update failed: No rows returned",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    AppLogger.e("UpdateUser", "Exception: ${e.message}")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@ManageUser, "Something went wrong", Toast.LENGTH_SHORT).show()
                    }
                }
            }

        }
    }



    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchUsersFromSupabase() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val supabase = (application as VerifiApp).supabase
                val app = application as VerifiApp
                val userRole = app.authorizedRole
                val companyId = app.companyID.toString()

                val allUsers = supabase.postgrest["users"]
                    .select(){
                        eq("companyID", companyId) } // ← This is the working filter
                .decodeList<User>()


                AppLogger.d("ManageUser", "Fetched ${allUsers.size} users from Supabase")


                val filteredUsers = when (userRole) {
                    "owner" -> allUsers
                    "admin" -> allUsers.filter { it.role == "reg_employee" }
                    else -> emptyList()
                }
                AppLogger.d("ManageUser", "Filtered users: ${filteredUsers.size}")

                if (userRole != "owner" && userRole != "admin") {
                    AppLogger.w("AccessControl", "Unauthorized role '$userRole' attempted to access ManageUser screen")
                }

                withContext(Dispatchers.Main) {
                    userAdapter.setUsers(filteredUsers)
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

