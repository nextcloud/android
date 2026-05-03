// MainActivity.kt
package com.example.cookbook

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    
    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var biometricLoginButton: Button
    private lateinit var switchUserButton: Button
    
    private val userManager = UserManager()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        usernameEditText = findViewById(R.id.usernameEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        loginButton = findViewById(R.id.loginButton)
        biometricLoginButton = findViewById(R.id.biometricLoginButton)
        switchUserButton = findViewById(R.id.switchUserButton)
        
        loginButton.setOnClickListener { performLogin() }
        biometricLoginButton.setOnClickListener { performBiometricLogin() }
        switchUserButton.setOnClickListener { showUserSwitcher() }
        
        // Check if user is already logged in
        if (userManager.isLoggedIn()) {
            navigateToHome()
        }
        
        // Check for shared credentials from Notes/Nextcloud
        checkSharedCredentials()
    }
    
    private fun performLogin() {
        val username = usernameEditText.text.toString()
        val password = passwordEditText.text.toString()
        
        if (username.isNotEmpty() && password.isNotEmpty()) {
            // Simulate login - in real app, this would call an API
            userManager.login(username, password)
            Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
            navigateToHome()
        } else {
            Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun performBiometricLogin() {
        val executor = Executors.newSingleThreadExecutor()
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Biometric login successful", Toast.LENGTH_SHORT).show()
                        navigateToHome()
                    }
                }
                
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Authentication error: $errString", Toast.LENGTH_SHORT).show()
                    }
                }
                
                override fun onAuthenticationFailed() {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Authentication failed", Toast.LENGTH_SHORT).show()
                    }
                }
            })
        
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric Login")
            .setSubtitle("Log in using your biometric credential")
            .setNegativeButtonText("Cancel")
            .build()
        
        biometricPrompt.authenticate(promptInfo)
    }
    
    private fun showUserSwitcher() {
        val users = userManager.getLoggedInUsers()
        if (users.isNotEmpty()) {
            val builder = android.app.AlertDialog.Builder(this)
            builder.setTitle("Switch User")
            
            val userNames = users.map { it.username }.toTypedArray()
            builder.setItems(userNames) { _, which ->
                val selectedUser = users[which]
                userManager.switchToUser(selectedUser)
                Toast.makeText(this, "Switched to ${selectedUser.username}", Toast.LENGTH_SHORT).show()
                navigateToHome()
            }
            
            builder.setPositiveButton("Add New User") { _, _ ->
                // Clear fields for new login
                usernameEditText.text.clear()
                passwordEditText.text.clear()
            }
            
            builder.show()
        } else {
            Toast.makeText(this, "No users logged in", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun checkSharedCredentials() {
        // Check if Notes or Nextcloud apps have shared credentials
        val sharedPref = getSharedPreferences("shared_credentials", MODE_PRIVATE)
        val sharedUsername = sharedPref.getString("username", null)
        val sharedPassword = sharedPref.getString("password", null)
        
        if (sharedUsername != null && sharedPassword != null) {
            // Auto-login with shared credentials
            userManager.login(sharedUsername, sharedPassword)
            Toast.makeText(this, "Logged in with shared credentials", Toast.LENGTH_SHORT).show()
            navigateToHome()
        }
    }
    
    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }
}

// UserManager.kt
package com.example.cookbook

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class UserManager {
    
    data class User(val username: String, val token: String)
    
    private val users = mutableListOf<User>()
    private var currentUser: User? = null
    
    fun login(username: String, password: String): Boolean {
        // Simulate API call - in real app, this would validate credentials
        val token = generateToken(username, password)
        val user = User(username, token)
        
        if (!users.any { it.username == username }) {
            users.add(user)
        }
        currentUser = user
        saveUserToPreferences(user)
        return true
    }
    
    fun isLoggedIn(): Boolean {
        return currentUser != null
    }
    
    fun getLoggedInUsers(): List<User> {
        return users.toList()
    }
    
    fun switchToUser(user: User) {
        currentUser = user
    }
    
    fun getCurrentUser(): User? {
        return currentUser
    }
    
    private fun generateToken(username: String, password: String): String {
        // In real app, this would be a JWT or similar token
        return "token_${username}_${System.currentTimeMillis()}"
    }
    
    private fun saveUserToPreferences(user: User) {
        // Save to encrypted shared preferences for security
        val context = App.instance
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        
        val sharedPreferences = EncryptedSharedPreferences.create(
            context,
            "user_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        
        sharedPreferences.edit()
            .putString("current_username", user.username)
            .putString("current_token", user.token)
            .apply()
    }
}

// HomeActivity.kt
package com.example.cookbook

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class HomeActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        
        val welcomeText = findViewById<TextView>(R.id.welcomeText)
        val logoutButton = findViewById<Button>(R.id.logoutButton)
        val switchUserButton = findViewById<Button>(R.id.switchUserButton)
        
        val userManager = UserManager()
        val currentUser = userManager.getCurrentUser()
        
        welcomeText.text = "Welcome, ${currentUser?.username ?: "User"}"
        
        logoutButton.setOnClickListener {
            Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()
            finish()
        }
        
        switchUserButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}

// App.kt (Application class)
package com.example.cookbook

import android.app.Application

class App : Application() {
    
    companion object {
        lateinit var instance: App
            private set
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}

// activity_main.xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp"
    android:gravity="center">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Cookbook Login"
        android:textSize="24sp"
        android:textStyle="bold"
        android:layout_marginBottom="32dp"/>

    <EditText
        android:id="@+id/usernameEditText"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:hint="Username"
        android:inputType="text"
        android:layout_marginBottom="16dp"/>

    <EditText
        android:id="@+id/passwordEditText"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:hint="Password"
        android:inputType="textPassword"
        android:layout_marginBottom="24dp"/>

    <Button
        android:id="@+id/loginButton"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Login"
        android:layout_marginBottom="16dp"/>

    <Button
        android:id="@+id/biometricLoginButton"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Biometric Login"
        android:layout_marginBottom="16dp"/>

    <Button
        android:id="@+id/switchUserButton"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Switch User"/>

</LinearLayout>

// activity_home.xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp"
    android:gravity="center">

    <TextView
        android:id="@+id/welcomeText"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:textSize="20sp"
        android:layout_marginBottom="32dp"/>

    <Button
        android:id="@+id/logoutButton"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Logout"
        android:layout_marginBottom="16dp"/>

    <Button
        android:id="@+id/switchUserButton"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Switch User"/>

</LinearLayout>

// build.gradle (Module: app) dependencies
dependencies {
    implementation 'androidx.biometric:biometric:1.2.0-alpha05'
    implementation 'androidx.security:security-crypto:1.1.0-alpha06'
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.9.0'
}
