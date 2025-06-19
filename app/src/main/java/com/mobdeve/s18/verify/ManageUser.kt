package com.mobdeve.s18.verify

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import com.google.android.material.bottomnavigation.BottomNavigationView

class ManageUser : AppCompatActivity() {

    private lateinit var userAdapter: UserAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var addUserLauncher: ActivityResultLauncher<Intent>
    private val users = mutableListOf(
        User("Alice", "alice@example.com", true),
        User("Bob", "bob@example.com", false),
        User("Charlie", "charlie@example.com", true),
        User("David", "david@example.com", true),
        User("Eva", "eva@example.com", false),
        User("Frank", "frank@example.com", true),
        User("Grace", "grace@example.com", false),
        User("Hank", "hank@example.com", true),
        User("Isla", "isla@example.com", false)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_manage_users)

        recyclerView = findViewById(R.id.rvUsers)
        recyclerView.isNestedScrollingEnabled = true
        userAdapter = UserAdapter(users)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = userAdapter


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
            addUserLauncher.launch(intent)
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav2)
        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_home -> {
                    //startActivity(Intent(this, AdminDashboard::class.java))
                    true
                }
                R.id.nav_history -> {
                    //startActivity(Intent(this, History::class.java))
                    true
                }
                R.id.nav_users -> {
                    startActivity(Intent(this, ManageUser::class.java))
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, AdminSettings::class.java))
                    true
                }
                else -> false
            }
        }
    }
}
