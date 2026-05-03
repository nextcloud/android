// MainActivity.kt
package com.cookbook.app

import android.accounts.AccountManager
import android.accounts.Account
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cookbook.app.adapters.RecipeAdapter
import com.cookbook.app.models.Recipe
import com.cookbook.app.utils.AccountUtils
import com.cookbook.app.utils.NextcloudAuthHelper
import com.cookbook.app.utils.NotesAuthHelper

class MainActivity : AppCompatActivity() {

    private lateinit var userIcon: ImageView
    private lateinit var recipeRecyclerView: RecyclerView
    private lateinit var recipeAdapter: RecipeAdapter
    private var currentAccount: Account? = null
    private val accountManager: AccountManager by lazy { AccountManager.get(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        userIcon = findViewById(R.id.user_icon)
        recipeRecyclerView = findViewById(R.id.recipe_recycler_view)

        setupRecyclerView()
        setupUserIcon()
        checkExistingAccounts()
    }

    private fun setupRecyclerView() {
        recipeAdapter = RecipeAdapter(mutableListOf())
        recipeRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = recipeAdapter
        }
    }

    private fun setupUserIcon() {
        userIcon.setOnClickListener { showUserSwitcher() }
    }

    private fun checkExistingAccounts() {
        val accounts = accountManager.getAccountsByType("com.cookbook")
        if (accounts.isNotEmpty()) {
            currentAccount = accounts[0]
            updateUIForAccount(currentAccount!!)
        } else {
            // Try to find accounts from Notes or Nextcloud
            val nextcloudAccounts = AccountUtils.getNextcloudAccounts(this)
            val notesAccounts = AccountUtils.getNotesAccounts(this)
            
            when {
                nextcloudAccounts.isNotEmpty() -> {
                    currentAccount = nextcloudAccounts[0]
                    importAccountFromNextcloud(currentAccount!!)
                }
                notesAccounts.isNotEmpty() -> {
                    currentAccount = notesAccounts[0]
                    importAccountFromNotes(currentAccount!!)
                }
                else -> {
                    // No accounts found, show login prompt
                    showLoginDialog()
                }
            }
        }
    }

    private fun showUserSwitcher() {
        val popupMenu = PopupMenu(this, userIcon)
        val allAccounts = getAllAvailableAccounts()
        
        allAccounts.forEach { account ->
            popupMenu.menu.add(0, account.hashCode(), 0, account.name)
        }
        
        popupMenu.menu.add(0, -1, allAccounts.size, "Add new account")
        
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                -1 -> showLoginDialog()
                else -> {
                    val selectedAccount = allAccounts.find { it.hashCode() == item.itemId }
                    selectedAccount?.let {
                        switchToAccount(it)
                    }
                }
            }
            true
        }
        
        popupMenu.show()
    }

    private fun getAllAvailableAccounts(): List<Account> {
        val cookbookAccounts = accountManager.getAccountsByType("com.cookbook").toMutableList()
        val nextcloudAccounts = AccountUtils.getNextcloudAccounts(this)
        val notesAccounts = AccountUtils.getNotesAccounts(this)
        
        cookbookAccounts.addAll(nextcloudAccounts)
        cookbookAccounts.addAll(notesAccounts)
        
        return cookbookAccounts.distinctBy { it.name }
    }

    private fun switchToAccount(account: Account) {
        currentAccount = account
        updateUIForAccount(account)
        Toast.makeText(this, "Switched to ${account.name}", Toast.LENGTH_SHORT).show()
    }

    private fun updateUIForAccount(account: Account) {
        // Update UI with account-specific data
        val recipes = fetchRecipesForAccount(account)
        recipeAdapter.updateRecipes(recipes)
        userIcon.setImageResource(R.drawable.ic_user_placeholder)
    }

    private fun fetchRecipesForAccount(account: Account): List<Recipe> {
        // Fetch recipes from the server associated with this account
        return when {
            account.type == "com.nextcloud" -> {
                NextcloudAuthHelper.fetchRecipes(this, account)
            }
            account.type == "com.notes" -> {
                NotesAuthHelper.fetchRecipes(this, account)
            }
            else -> {
                // Fetch from cookbook's own server
                fetchRecipesFromCookbookServer(account)
            }
        }
    }

    private fun fetchRecipesFromCookbookServer(account: Account): List<Recipe> {
        // Implementation for fetching recipes from cookbook's own server
        return emptyList()
    }

    private fun importAccountFromNextcloud(account: Account) {
        // Import Nextcloud account as cookbook account
        val cookbookAccount = Account(account.name, "com.cookbook")
        accountManager.addAccountExplicitly(cookbookAccount, null, account.extras)
        currentAccount = cookbookAccount
        updateUIForAccount(cookbookAccount)
    }

    private fun importAccountFromNotes(account: Account) {
        // Import Notes account as cookbook account
        val cookbookAccount = Account(account.name, "com.cookbook")
        accountManager.addAccountExplicitly(cookbookAccount, null, account.extras)
        currentAccount = cookbookAccount
        updateUIForAccount(cookbookAccount)
    }

    private fun showLoginDialog() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivityForResult(intent, LOGIN_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LOGIN_REQUEST_CODE && resultCode == RESULT_OK) {
            val accountName = data?.getStringExtra("account_name")
            accountName?.let {
                val account = Account(it, "com.cookbook")
                accountManager.addAccountExplicitly(account, null, Bundle())
                switchToAccount(account)
            }
        }
    }

    companion object {
        private const val LOGIN_REQUEST_CODE = 1001
    }
}

// AccountUtils.kt
package com.cookbook.app.utils

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context

object AccountUtils {
    
    fun getNextcloudAccounts(context: Context): List<Account> {
        val accountManager = AccountManager.get(context)
        return accountManager.getAccountsByType("com.nextcloud.cloud").toList()
    }
    
    fun getNotesAccounts(context: Context): List<Account> {
        val accountManager = AccountManager.get(context)
        return accountManager.getAccountsByType("com.notes.android").toList()
    }
    
    fun getCookbookAccounts(context: Context): List<Account> {
        val accountManager = AccountManager.get(context)
        return accountManager.getAccountsByType("com.cookbook").toList()
    }
}

// NextcloudAuthHelper.kt
package com.cookbook.app.utils

import android.accounts.Account
import android.content.Context
import com.cookbook.app.models.Recipe

object NextcloudAuthHelper {
    
    fun fetchRecipes(context: Context, account: Account): List<Recipe> {
        // Implementation to fetch recipes from Nextcloud server
        // This would use the Nextcloud API with the account credentials
        return emptyList()
    }
}

// NotesAuthHelper.kt
package com.cookbook.app.utils

import android.accounts.Account
import android.content.Context
import com.cookbook.app.models.Recipe

object NotesAuthHelper {
    
    fun fetchRecipes(context: Context, account: Account): List<Recipe> {
        // Implementation to fetch recipes from Notes server
        // This would use the Notes API with the account credentials
        return emptyList()
    }
}

// LoginActivity.kt
package com.cookbook.app

import android.accounts.Account
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
        
        usernameEditText = findViewById(R.id.username)
        passwordEditText = findViewById(R.id.password)
        loginButton = findViewById(R.id.login_button)
        accountManager = AccountManager.get(this)
        
        loginButton.setOnClickListener { performLogin() }
    }
    
    private fun performLogin() {
        val username = usernameEditText.text.toString()
        val password = passwordEditText.text.toString()
        
        if (username.isBlank() || password.isBlank()) {
            Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Perform authentication with your server
        authenticateUser(username, password) { success ->
            if (success) {
                val account = Account(username, "com.cookbook")
                accountManager.addAccountExplicitly(account, password, Bundle())
                
                val resultIntent = Intent()
                resultIntent.putExtra("account_name", username)
                setResult(RESULT_OK, resultIntent)
                finish()
            } else {
                Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun authenticateUser(username: String, password: String, callback: (Boolean) -> Unit) {
        // Implement actual authentication logic here
        // For now, just return success
        callback(true)
    }
}

// activity_main.xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout 
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <ImageView
        android:id="@+id/user_icon"
        android:layout_width="48dp"
        android:layout_height="48dp"
        android:layout_marginEnd="16dp"
        android:layout_marginTop="16dp"
        android:src="@drawable/ic_user_placeholder"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/recipe_recycler_view"
        android:layout_width="0dp"
        android:layout_height="0dp"
        android:layout_marginStart="16dp"
        android:layout_marginEnd="16dp"
        android:layout_marginTop="80dp"
        android:layout_marginBottom="16dp"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>

// activity_login.xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="24dp"
    android:gravity="center">

    <EditText
        android:id="@+id/username"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:hint="Username"
        android:inputType="text" />

    <EditText
        android:id="@+id/password"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="16dp"
        android:hint="Password"
        android:inputType="textPassword" />

    <Button
        android:id="@+id/login_button"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="24dp"
        android:text="Login" />

</LinearLayout>
