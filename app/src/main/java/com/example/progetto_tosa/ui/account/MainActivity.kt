package com.example.progetto_tosa.ui.account

import android.os.Bundle
import androidx.core.view.WindowCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.progetto_tosa.R
import com.example.progetto_tosa.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1) Imposto il tema PRIMA di chiamare super.onCreate
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("darkMode", true)
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 2) Setup BottomNavigationView + NavController
        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)

        // 3) Configuro l’AppBar per questi quattro “top-level destinations”
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home,
                R.id.navigation_workout,
                R.id.navigation_cronotimer,  // terzo tab → CronoTimerFragment
                R.id.navigation_account
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // 4) Mantengo le icone nei loro colori originali
        navView.itemIconTintList = null

        // 5) (Opzionale) Se vuoi gestire manualmente la navigazione:
        navView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    navController.navigate(R.id.navigation_home)
                    true
                }
                R.id.navigation_workout -> {
                    navController.navigate(R.id.fragment_workout)
                    true
                }
                R.id.navigation_cronotimer -> {
                    navController.navigate(R.id.navigation_cronotimer)
                    true
                }
                R.id.navigation_account -> {
                    navController.navigate(R.id.navigation_account)
                    true
                }
                else -> false
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
