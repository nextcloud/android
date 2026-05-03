// MainActivity.kt
package com.example.cookbook

import android.accounts.AccountManager
import android.accounts.AccountManagerCallback
import android.accounts.AccountManagerFuture
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity() {
    
    private lateinit var accountManager: AccountManager
    private lateinit var userIcon: ImageView
    private lateinit var currentUserText: TextView
    private lateinit var loginButton: Button
    private lateinit var userRecyclerView: RecyclerView
    private lateinit var userAdapter: UserAccountAdapter
    
    private val accounts = mutableListOf<UserAccount>()
    private var currentAccount: UserAccount? = null
    
    companion object {
        private const val ACCOUNT_TYPE_NOTES = "com.example.notes"
        private const val ACCOUNT_TYPE_NEXTCLOUD = "com.nextcloud.client"
        private const val ACCOUNT_TYPE_COOKBOOK = "com.example.cookbook"
        private const val REQUEST_ADD_ACCOUNT = 1001
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        accountManager = AccountManager.get(this)
        initializeViews()
        loadExistingAccounts()
        setupListeners()
    }
    
    private fun initializeViews() {
        userIcon = findViewById(R.id.user_icon)
        currentUserText = findViewById(R.id.current_user_text)
        loginButton = findViewById(R.id.login_button)
        userRecyclerView = findViewById(R.id.user_recycler_view)
        
        userRecyclerView.layoutManager = LinearLayoutManager(this)
        userAdapter = UserAccountAdapter(accounts) { account ->
            switchToUser(account)
        }
        userRecyclerView.adapter = userAdapter
    }
    
    private fun loadExistingAccounts() {
        accounts.clear()
        
        // Load accounts from Notes app
        val notesAccounts = accountManager.getAccountsByType(ACCOUNT_TYPE_NOTES)
        notesAccounts.forEach { account ->
            accounts.add(UserAccount(
                name = account.name,
                type = "Notes",
                accountManagerAccount = account
            ))
        }
        
        // Load accounts from Nextcloud app
        val nextcloudAccounts = accountManager.getAccountsByType(ACCOUNT_TYPE_NEXTCLOUD)
        nextcloudAccounts.forEach { account ->
            accounts.add(UserAccount(
                name = account.name,
                type = "Nextcloud",
                accountManagerAccount = account
            ))
        }
        
        // Load existing Cookbook accounts
        val cookbookAccounts = accountManager.getAccountsByType(ACCOUNT_TYPE_COOKBOOK)
        cookbookAccounts.forEach { account ->
            accounts.add(UserAccount(
                name = account.name,
                type = "Cookbook",
                accountManagerAccount = account
            ))
        }
        
        userAdapter.notifyDataSetChanged()
        
        // Set current user if exists
        if (accounts.isNotEmpty()) {
            switchToUser(accounts[0])
        }
    }
    
    private fun setupListeners() {
        userIcon.setOnClickListener {
            showUserSwitcherDialog()
        }
        
        loginButton.setOnClickListener {
            showLoginOptions()
        }
    }
    
    private fun showUserSwitcherDialog() {
        val userNames = accounts.map { "${it.name} (${it.type})" }.toTypedArray()
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Switch User")
            .setItems(userNames) { _, which ->
                switchToUser(accounts[which])
            }
            .setPositiveButton("Add Account") { _, _ ->
                showLoginOptions()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showLoginOptions() {
        val options = arrayOf("Login with Notes", "Login with Nextcloud", "Create Cookbook Account")
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Add Account")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> loginWithNotes()
                    1 -> loginWithNextcloud()
                    2 -> createCookbookAccount()
                }
            }
            .show()
    }
    
    private fun loginWithNotes() {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            `package` = "com.example.notes"
        }
        
        try {
            startActivityForResult(intent, REQUEST_ADD_ACCOUNT)
        } catch (e: Exception) {
            Toast.makeText(this, "Notes app not installed", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun loginWithNextcloud() {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            `package` = "com.nextcloud.client"
        }
        
        try {
            startActivityForResult(intent, REQUEST_ADD_ACCOUNT)
        } catch (e: Exception) {
            Toast.makeText(this, "Nextcloud app not installed", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun createCookbookAccount() {
        accountManager.addAccount(
            ACCOUNT_TYPE_COOKBOOK,
            null,
            null,
            null,
            this,
            object : AccountManagerCallback<Bundle> {
                override fun run(future: AccountManagerFuture<Bundle>) {
                    try {
                        val result = future.result
                        val accountName = result.getString(AccountManager.KEY_ACCOUNT_NAME)
                        if (accountName != null) {
                            loadExistingAccounts()
                            Toast.makeText(this@MainActivity, "Account created", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@MainActivity, "Failed to create account", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            null
        )
    }
    
    private fun switchToUser(account: UserAccount) {
        currentAccount = account
        currentUserText.text = "${account.name} (${account.type})"
        userIcon.setImageResource(R.drawable.ic_user_placeholder)
        
        // Update UI with user-specific data
        loadUserData(account)
        
        // Close the user switcher if open
        userRecyclerView.visibility = RecyclerView.GONE
    }
    
    private fun loadUserData(account: UserAccount) {
        // Load recipes and settings for the selected user
        // This would typically involve loading from local storage or server
        Toast.makeText(this, "Switched to ${account.name}", Toast.LENGTH_SHORT).show()
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == REQUEST_ADD_ACCOUNT && resultCode == RESULT_OK) {
            loadExistingAccounts()
        }
    }
}

// UserAccount.kt
package com.example.cookbook

import android.accounts.Account

data class UserAccount(
    val name: String,
    val type: String,
    val accountManagerAccount: Account
)

// UserAccountAdapter.kt
package com.example.cookbook

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class UserAccountAdapter(
    private val accounts: List<UserAccount>,
    private val onItemClick: (UserAccount) -> Unit
) : RecyclerView.Adapter<UserAccountAdapter.ViewHolder>() {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_account, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val account = accounts[position]
        holder.bind(account)
        holder.itemView.setOnClickListener { onItemClick(account) }
    }
    
    override fun getItemCount() = accounts.size
    
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val userNameText: TextView = itemView.findViewById(R.id.user_name_text)
        private val userTypeText: TextView = itemView.findViewById(R.id.user_type_text)
        
        fun bind(account: UserAccount) {
            userNameText.text = account.name
            userTypeText.text = account.type
        }
    }
}
