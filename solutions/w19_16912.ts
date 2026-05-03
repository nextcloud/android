// MainActivity.kt
package com.example.cookbook

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity() {
    
    private lateinit var accountManager: AccountManager
    private lateinit var userList: RecyclerView
    private lateinit var loginButton: Button
    private lateinit var usernameInput: EditText
    private lateinit var passwordInput: EditText
    
    private val accountType = "com.example.cookbook.account"
    private val tokenType = "cookbook_auth"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        accountManager = AccountManager.get(this)
        userList = findViewById(R.id.user_list)
        loginButton = findViewById(R.id.login_button)
        usernameInput = findViewById(R.id.username_input)
        passwordInput = findViewById(R.id.password_input)
        
        setupUserList()
        setupLoginButton()
        checkExistingAccounts()
    }
    
    private fun setupUserList() {
        userList.layoutManager = LinearLayoutManager(this)
        userList.adapter = UserAdapter(getAccounts()) { account ->
            switchToUser(account)
        }
    }
    
    private fun setupLoginButton() {
        loginButton.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            
            if (username.isNotEmpty() && password.isNotEmpty()) {
                addAccount(username, password)
            } else {
                Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun checkExistingAccounts() {
        val accounts = getAccounts()
        if (accounts.isNotEmpty()) {
            showUserSelectionDialog(accounts)
        }
    }
    
    private fun getAccounts(): Array<Account> {
        return accountManager.getAccountsByType(accountType)
    }
    
    private fun addAccount(username: String, password: String) {
        val account = Account(username, accountType)
        
        if (accountManager.addAccountExplicitly(account, password, null)) {
            accountManager.setAuthToken(account, tokenType, generateToken(username, password))
            Toast.makeText(this, "Account added: $username", Toast.LENGTH_SHORT).show()
            refreshUserList()
            switchToUser(account)
        } else {
            Toast.makeText(this, "Account already exists", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun switchToUser(account: Account) {
        val intent = Intent(this, CookbookActivity::class.java)
        intent.putExtra("username", account.name)
        intent.putExtra("token", accountManager.peekAuthToken(account, tokenType))
        startActivity(intent)
    }
    
    private fun generateToken(username: String, password: String): String {
        return "cookbook_token_${username}_${System.currentTimeMillis()}"
    }
    
    private fun refreshUserList() {
        (userList.adapter as UserAdapter).updateAccounts(getAccounts())
    }
    
    private fun showUserSelectionDialog(accounts: Array<Account>) {
        val accountNames = accounts.map { it.name }.toTypedArray()
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Select User")
            .setItems(accountNames) { _, which ->
                switchToUser(accounts[which])
            }
            .setPositiveButton("Add New User") { _, _ ->
                // Stay on login screen
            }
            .show()
    }
}

// UserAdapter.kt
package com.example.cookbook

import android.accounts.Account
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class UserAdapter(
    private var accounts: Array<Account>,
    private val onUserClick: (Account) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val account = accounts[position]
        holder.bind(account)
        holder.itemView.setOnClickListener { onUserClick(account) }
    }
    
    override fun getItemCount() = accounts.size
    
    fun updateAccounts(newAccounts: Array<Account>) {
        accounts = newAccounts
        notifyDataSetChanged()
    }
    
    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val usernameText: TextView = itemView.findViewById(R.id.username_text)
        
        fun bind(account: Account) {
            usernameText.text = account.name
        }
    }
}

// activity_main.xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Cookbook Login"
        android:textSize="24sp"
        android:textStyle="bold"
        android:layout_marginBottom="24dp"/>

    <EditText
        android:id="@+id/username_input"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:hint="Username"
        android:inputType="text"
        android:layout_marginBottom="12dp"/>

    <EditText
        android:id="@+id/password_input"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:hint="Password"
        android:inputType="textPassword"
        android:layout_marginBottom="16dp"/>

    <Button
        android:id="@+id/login_button"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Login"
        android:layout_marginBottom="24dp"/>

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Saved Users:"
        android:textSize="18sp"
        android:textStyle="bold"
        android:layout_marginBottom="8dp"/>

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/user_list"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"/>

</LinearLayout>

// item_user.xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="horizontal"
    android:padding="12dp"
    android:background="?android:attr/selectableItemBackground">

    <TextView
        android:id="@+id/username_text"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:textSize="16sp"/>

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text=">"
        android:textSize="20sp"
        android:textColor="?android:attr/colorAccent"/>

</LinearLayout>

// AndroidManifest.xml additions
<uses-permission android:name="android.permission.GET_ACCOUNTS" />
<uses-permission android:name="android.permission.AUTHENTICATE_ACCOUNTS" />
<uses-permission android:name="android.permission.MANAGE_ACCOUNTS" />

<application>
    <activity android:name=".MainActivity" android:exported="true">
        <intent-filter>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LAUNCHER" />
        </intent-filter>
    </activity>
    <activity android:name=".CookbookActivity" android:exported="true" />
</application>
