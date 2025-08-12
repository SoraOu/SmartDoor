package com.example.smartdoor

import UserRegistrationFragment
import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.smartdoor.databinding.ActivityMainBinding
import com.example.smartdoor.fragments.*
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.database.FirebaseDatabase

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // ✅ Request notification permission (Android 13+)
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            // ✅ Start service regardless of user response
            startDoorMonitorService()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
        FirebaseDatabase.getInstance().goOnline()

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Handle the intent if coming from a notification click
        val targetFragment = intent?.getStringExtra("targetFragment")
        if (targetFragment == "history") {
            replaceFragment(HistoryFragment())  // Navigate to HistoryFragment
            binding.bottomNav.selectedItemId = R.id.nav_history  // Highlight the History tab
        } else {
            replaceFragment(HomeFragment())  // Default to HomeFragment
        }

        // Request notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startDoorMonitorService()
        }

        // Bottom navigation listener
        binding.bottomNav.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> replaceFragment(HomeFragment())
                R.id.nav_register -> replaceFragment(UserRegistrationFragment())
                R.id.nav_history -> replaceFragment(HistoryFragment())
            }
            true
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun startDoorMonitorService() {
        val intent = Intent(this, com.example.smartdoor.service.DoorMonitorService::class.java)
        Log.d("MainActivity", "Starting DoorMonitorService")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    // Handle the intent when the app is brought to the foreground from a notification tap
    // We already handle intent inside onCreate(), no need for onNewIntent()
}
