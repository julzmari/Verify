package com.mobdeve.s18.verify.controller

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
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
import androidx.appcompat.app.AlertDialog
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.mobdeve.s18.verify.R
import com.mobdeve.s18.verify.model.User
import com.mobdeve.s18.verify.model.UserParcelable




import io.github.jan.supabase.postgrest.query.FilterOperation
import com.mobdeve.s18.verify.app.VerifiApp
import com.mobdeve.s18.verify.model.toUser
import java.util.*
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.*



class ManageUser : BaseActivity() {

    private lateinit var userAdapter: UserAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var addUserLauncher: ActivityResultLauncher<Intent>
    private val users = mutableListOf<User>()



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
    }



    private fun showUpdateUserDialog(user: User) {
        val dialogView = layoutInflater.inflate(R.layout.popup_edit_user, null)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.show()

        val usernameField = dialogView.findViewById<EditText>(R.id.editUsername)
        val emailField = dialogView.findViewById<EditText>(R.id.editEmail)
        val roleSpinner = dialogView.findViewById<Spinner>(R.id.userRoleSpinner)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelbtn)
        val updateButton = dialogView.findViewById<Button>(R.id.updateUserbtn)

        // Pre-fill fields
        usernameField.setText(user.name)
        emailField.setText(user.email)

        // Populate and set spinner selection
        val roles = listOf("admin", "reg_employee")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, roles)
        roleSpinner.adapter = adapter
        roleSpinner.setSelection(roles.indexOf(user.role))

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }



        updateButton.setOnClickListener {
            val newUsername = usernameField.text.toString().trim()
            val newEmail = emailField.text.toString().trim()
            val newRole = roleSpinner.selectedItem.toString()



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
                    Log.e("UpdateUser", "Exception: ${e.message}", e)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@ManageUser, "Something went wrong", Toast.LENGTH_SHORT).show()
                    }
                }
            }

        }
    }





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


                Log.d("ManageUser", "Fetched ${allUsers.size} users from Supabase")


                val filteredUsers = when (userRole) {
                    "owner" -> allUsers
                    "admin" -> allUsers.filter { it.role == "reg_employee" }
                    else -> emptyList()
                }
                Log.d("ManageUser", "Filtered users: ${filteredUsers.size}")


                withContext(Dispatchers.Main) {
                    userAdapter.setUsers(filteredUsers)
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
