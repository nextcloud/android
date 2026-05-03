// MainActivity.kt
package com.example.cookbook

import android.accounts.AccountManager
import android.accounts.AccountManagerCallback
import android.accounts.AccountManagerFuture
import android.accounts.AuthenticatorException
import android.accounts.OperationCanceledException
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var accountManager: AccountManager
    private lateinit var currentUserTextView: TextView
    private lateinit var userIconImageView: ImageView
    private lateinit var loginButton: Button
    private var currentAccount: android.accounts.Account? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        accountManager = AccountManager.get(this)
        currentUserTextView = findViewById(R.id.currentUserTextView)
        userIconImageView = findViewById(R.id.userIconImageView)
        loginButton = findViewById(R.id.loginButton)

        // Check for existing accounts from Notes and Nextcloud
        checkExistingAccounts()

        userIconImageView.setOnClickListener {
            showUserSwitchDialog()
        }

        loginButton.setOnClickListener {
            // Trigger login flow for Cookbook
            val intent = Intent(this, LoginActivity::class.java)
            startActivityForResult(intent, LOGIN_REQUEST_CODE)
        }
    }

    private fun checkExistingAccounts() {
        val notesAccounts = accountManager.getAccountsByType("com.notes.android")
        val nextcloudAccounts = accountManager.getAccountsByType("com.nextcloud.client")
        val allAccounts = notesAccounts + nextcloudAccounts

        if (allAccounts.isNotEmpty()) {
            // Auto-login with first available account
            currentAccount = allAccounts[0]
            updateUIForAccount(currentAccount!!)
        } else {
            currentUserTextView.text = "Not logged in"
        }
    }

    private fun updateUIForAccount(account: android.accounts.Account) {
        currentAccount = account
        currentUserTextView.text = account.name
        // Fetch user display name from account
        val userData = accountManager.getUserData(account, "display_name")
        if (userData != null) {
            currentUserTextView.text = userData
        }
    }

    private fun showUserSwitchDialog() {
        val notesAccounts = accountManager.getAccountsByType("com.notes.android")
        val nextcloudAccounts = accountManager.getAccountsByType("com.nextcloud.client")
        val allAccounts = notesAccounts + nextcloudAccounts

        if (allAccounts.isEmpty()) {
            Toast.makeText(this, "No accounts found. Please login first.", Toast.LENGTH_SHORT).show()
            return
        }

        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_user_switch, null)
        val recyclerView = view.findViewById<RecyclerView>(R.id.userRecyclerView)
        val adapter = UserAccountAdapter(allAccounts.toList()) { account ->
            switchToAccount(account)
            dialog.dismiss()
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        dialog.setContentView(view)
        dialog.show()
    }

    private fun switchToAccount(account: android.accounts.Account) {
        // Get auth token for the selected account
        accountManager.getAuthToken(account, "cookbook_access", null, this,
            object : AccountManagerCallback<Bundle> {
                override fun run(future: AccountManagerFuture<Bundle>) {
                    try {
                        val result = future.result
                        val authToken = result.getString(AccountManager.KEY_AUTHTOKEN)
                        if (authToken != null) {
                            // Use this token to authenticate with Cookbook backend
                            authenticateWithCookbook(authToken, account)
                        }
                    } catch (e: OperationCanceledException) {
                        e.printStackTrace()
                    } catch (e: AuthenticatorException) {
                        e.printStackTrace()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }, null)
    }

    private fun authenticateWithCookbook(authToken: String, account: android.accounts.Account) {
        // Simulate API call to Cookbook backend
        runOnUiThread {
            updateUIForAccount(account)
            Toast.makeText(this, "Switched to ${account.name}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LOGIN_REQUEST_CODE && resultCode == RESULT_OK) {
            val accountName = data?.getStringExtra("account_name")
            if (accountName != null) {
                // Add account to AccountManager for future use
                val account = android.accounts.Account(accountName, "com.cookbook.android")
                accountManager.addAccountExplicitly(account, null, null)
                currentAccount = account
                updateUIForAccount(account)
            }
        }
    }

    companion object {
        private const val LOGIN_REQUEST_CODE = 1001
    }
}

// UserAccountAdapter.kt
package com.example.cookbook

import android.accounts.Account
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class UserAccountAdapter(
    private val accounts: List<Account>,
    private val onAccountClick: (Account) -> Unit
) : RecyclerView.Adapter<UserAccountAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val account = accounts[position]
        holder.textView.text = account.name
        holder.itemView.setOnClickListener {
            onAccountClick(account)
        }
    }

    override fun getItemCount() = accounts.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(android.R.id.text1)
    }
}

// LoginActivity.kt
package com.example.cookbook

import android.accounts.AccountManager
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var accountManager: AccountManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        accountManager = AccountManager.get(this)
        usernameEditText = findViewById(R.id.usernameEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        loginButton = findViewById(R.id.loginButton)

        loginButton.setOnClickListener {
            val username = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (username.isNotEmpty() && password.isNotEmpty()) {
                // Simulate login API call
                performLogin(username, password)
            } else {
                Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun performLogin(username: String, password: String) {
        // Simulate successful login
        val account = android.accounts.Account(username, "com.cookbook.android")
        accountManager.addAccountExplicitly(account, password, null)
        accountManager.setUserData(account, "display_name", username)

        val intent = Intent()
        intent.putExtra("account_name", username)
        setResult(RESULT_OK, intent)
        finish()
    }
}
