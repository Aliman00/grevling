package com.smsforwarder

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView

    // Cache fragments for å unngå gjenskaping ved tab-bytte
    private val homeFragment by lazy { HomeFragment() }
    private val appSelectionFragment by lazy { AppSelectionFragment() }
    private val settingsFragment by lazy { SettingsFragment() }

    // Moderne permissions API (erstatter deprecated onRequestPermissionsResult)
    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val deniedCount = permissions.values.count { !it }
        if (deniedCount > 0) {
            Toast.makeText(
                this,
                getString(R.string.permissions_needed_toast, deniedCount),
                Toast.LENGTH_LONG
            ).show()
        } else {
            Toast.makeText(this, getString(R.string.permissions_granted_toast), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottom_navigation)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    loadFragment(homeFragment)
                    true
                }
                R.id.nav_apps -> {
                    loadFragment(appSelectionFragment)
                    true
                }
                R.id.nav_settings -> {
                    loadFragment(settingsFragment)
                    true
                }
                else -> false
            }
        }

        // Load home fragment by default
        if (savedInstanceState == null) {
            loadFragment(homeFragment)
        }

        requestPermissions()
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun requestPermissions() {
        val permissionsToRequest = PermissionsHelper.REQUIRED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
}