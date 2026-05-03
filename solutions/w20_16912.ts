// LoginActivity.kt
package com.example.cookbook

import android.accounts.AccountManager
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.cookbook.databinding.ActivityLoginBinding
import kotlinx.coroutines.*

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val accountManager by lazy { AccountManager.get(this) }
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        checkExistingAccounts()
    }

    private fun setupUI() {
        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            
            if (username.isNotEmpty() && password.isNotEmpty()) {
                performLogin(username, password)
            } else {
                Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnSwitchAccount.setOnClickListener {
            showAccountSwitcher()
        }
    }

    private fun checkExistingAccounts() {
        val accounts = accountManager.getAccountsByType("com.example.cookbook")
        if (accounts.isNotEmpty()) {
            binding.btnSwitchAccount.visibility = android.view.View.VISIBLE
            binding.tvCurrentUser.text = "Logged in as: ${accounts[0].name}"
        }
    }

    private fun performLogin(username: String, password: String) {
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    // Simulate network login
                    delay(1000)
                    authenticateUser(username, password)
                }

                if (result) {
                    saveAccount(username, password)
                    navigateToMain()
                } else {
                    Toast.makeText(this@LoginActivity, "Login failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun authenticateUser(username: String, password: String): Boolean {
        // Replace with actual authentication logic
        return username.isNotEmpty() && password.isNotEmpty()
    }

    private fun saveAccount(username: String, password: String) {
        val account = android.accounts.Account(username, "com.example.cookbook")
        accountManager.addAccountExplicitly(account, password, null)
    }

    private fun showAccountSwitcher() {
        val accounts = accountManager.getAccountsByType("com.example.cookbook")
        if (accounts.isNotEmpty()) {
            val accountNames = accounts.map { it.name }.toTypedArray()
            
            android.app.AlertDialog.Builder(this)
                .setTitle("Switch Account")
                .setItems(accountNames) { _, which ->
                    switchToAccount(accounts[which])
                }
                .setPositiveButton("Add Account") { _, _ ->
                    clearFields()
                }
                .show()
        }
    }

    private fun switchToAccount(account: android.accounts.Account) {
        val password = accountManager.getPassword(account)
        if (password != null) {
            binding.etUsername.setText(account.name)
            binding.etPassword.setText(password)
            binding.tvCurrentUser.text = "Logged in as: ${account.name}"
        }
    }

    private fun clearFields() {
        binding.etUsername.text.clear()
        binding.etPassword.text.clear()
        binding.tvCurrentUser.text = "Not logged in"
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
