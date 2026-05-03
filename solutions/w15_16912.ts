// MainActivity.kt
package com.example.cookbook

import android.accounts.AccountManager
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cookbook.adapter.AccountAdapter
import com.example.cookbook.model.Account
import com.example.cookbook.util.AccountHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity() {

    private lateinit var accountHelper: AccountHelper
    private lateinit var currentUserTextView: TextView
    private lateinit var userIconImageView: ImageView
    private lateinit var accountRecyclerView: RecyclerView
    private lateinit var accountAdapter: AccountAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        accountHelper = AccountHelper(this)
        currentUserTextView = findViewById(R.id.currentUserTextView)
        userIconImageView = findViewById(R.id.userIconImageView)
        accountRecyclerView = findViewById(R.id.accountRecyclerView)

        setupUserIcon()
        setupAccountList()
        updateCurrentUserDisplay()
    }

    private fun setupUserIcon() {
        userIconImageView.setOnClickListener {
            showAccountSwitcherDialog()
        }
    }

    private fun setupAccountList() {
        accountAdapter = AccountAdapter(
            accounts = accountHelper.getAccounts(),
            onAccountClick = { account ->
                switchToAccount(account)
            }
        )
        accountRecyclerView.layoutManager = LinearLayoutManager(this)
        accountRecyclerView.adapter = accountAdapter
    }

    private fun updateCurrentUserDisplay() {
        val currentAccount = accountHelper.getCurrentAccount()
        currentUserTextView.text = currentAccount?.username ?: "Not logged in"
    }

    private fun showAccountSwitcherDialog() {
        val accounts = accountHelper.getAccounts()
        val accountNames = accounts.map { it.username }.toTypedArray()

        MaterialAlertDialogBuilder(this)
            .setTitle("Switch Account")
            .setItems(accountNames) { _, which ->
                switchToAccount(accounts[which])
            }
            .setPositiveButton("Add Account") { _, _ ->
                addNewAccount()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun switchToAccount(account: Account) {
        accountHelper.setCurrentAccount(account)
        updateCurrentUserDisplay()
        Toast.makeText(this, "Switched to ${account.username}", Toast.LENGTH_SHORT).show()
    }

    private fun addNewAccount() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivityForResult(intent, REQUEST_ADD_ACCOUNT)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ADD_ACCOUNT && resultCode == RESULT_OK) {
            val username = data?.getStringExtra("username")
            val password = data?.getStringExtra("password")
            if (username != null && password != null) {
                accountHelper.addAccount(Account(username, password))
                accountAdapter.updateAccounts(accountHelper.getAccounts())
                updateCurrentUserDisplay()
                Toast.makeText(this, "Account added: $username", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val REQUEST_ADD_ACCOUNT = 1001
    }
}

// LoginActivity.kt
package com.example.cookbook

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val usernameEditText = findViewById<EditText>(R.id.usernameEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val loginButton = findViewById<Button>(R.id.loginButton)

        loginButton.setOnClickListener {
            val username = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (username.isNotEmpty() && password.isNotEmpty()) {
                val resultIntent = Intent()
                resultIntent.putExtra("username", username)
                resultIntent.putExtra("password", password)
                setResult(RESULT_OK, resultIntent)
                finish()
            } else {
                Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

// AccountHelper.kt
package com.example.cookbook.util

import android.content.Context
import android.content.SharedPreferences
import com.example.cookbook.model.Account
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class AccountHelper(context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("accounts", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getAccounts(): List<Account> {
        val json = sharedPreferences.getString("accounts_list", "[]")
        val type = object : TypeToken<List<Account>>() {}.type
        return gson.fromJson(json, type)
    }

    fun addAccount(account: Account) {
        val accounts = getAccounts().toMutableList()
        accounts.add(account)
        saveAccounts(accounts)
    }

    fun getCurrentAccount(): Account? {
        val json = sharedPreferences.getString("current_account", null)
        return if (json != null) gson.fromJson(json, Account::class.java) else null
    }

    fun setCurrentAccount(account: Account) {
        val json = gson.toJson(account)
        sharedPreferences.edit().putString("current_account", json).apply()
    }

    private fun saveAccounts(accounts: List<Account>) {
        val json = gson.toJson(accounts)
        sharedPreferences.edit().putString("accounts_list", json).apply()
    }
}

// Account.kt
package com.example.cookbook.model

data class Account(
    val username: String,
    val password: String
)

// AccountAdapter.kt
package com.example.cookbook.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cookbook.R
import com.example.cookbook.model.Account

class AccountAdapter(
    private var accounts: List<Account>,
    private val onAccountClick: (Account) -> Unit
) : RecyclerView.Adapter<AccountAdapter.AccountViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_account, parent, false)
        return AccountViewHolder(view)
    }

    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
        val account = accounts[position]
        holder.usernameTextView.text = account.username
        holder.itemView.setOnClickListener { onAccountClick(account) }
    }

    override fun getItemCount(): Int = accounts.size

    fun updateAccounts(newAccounts: List<Account>) {
        accounts = newAccounts
        notifyDataSetChanged()
    }

    class AccountViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val usernameTextView: TextView = itemView.findViewById(R.id.usernameTextView)
    }
}
