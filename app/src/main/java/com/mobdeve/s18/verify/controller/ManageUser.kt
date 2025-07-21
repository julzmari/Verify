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
import android.widget.EditText
import android.widget.ImageView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.mobdeve.s18.verify.R
import com.mobdeve.s18.verify.model.User



import com.mobdeve.s18.verify.app.VerifiApp
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
        userAdapter = UserAdapter(users)
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
                val newUser = result.data?.getSerializableExtra("newUser") as? User
                newUser?.let {
                    userAdapter.addUser(it)
                }
            }
        }

        addButton.setOnClickListener {

            val intent = Intent(this, AddUser::class.java)
            startActivity(intent)
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav2)
        setupBottomNavigation(bottomNav, R.id.nav_users)

    }

    private fun fetchUsersFromSupabase() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val supabase = (application as VerifiApp).supabase
                val result = supabase.postgrest["users"]
                    .select()
                    .decodeList<User>()
                    .filter { it.role == "reg_employee" }

                withContext(Dispatchers.Main) {
                    users.clear()
                    users.addAll(result)
                    userAdapter.notifyDataSetChanged()
                }
            } catch (e: Exception) {
                e.printStackTrace() // Optional: show toast or log
            }
        }
    }


}
