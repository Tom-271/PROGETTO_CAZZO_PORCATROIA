package com.example.progetto_tosa.ui.account

import android.os.Bundle
import androidx.core.view.WindowCompat
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.progetto_tosa.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.progetto_tosa.R

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        // ⬇️ Imposta il tema PRIMA di tutto
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

        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)

        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home,
                R.id.navigation_workout,
                R.id.navigation_stepwatch,
                R.id.navigation_account,
                R.id.navigation_tools // Assicurati che esista anche questo
            )
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // ⬇️ Disattiva il cambio automatico di colore delle icone
        navView.itemIconTintList = null

        // ⬇️ Menu a tendina per "Strumenti"
        navView.setOnItemSelectedListener {  item ->
            when (item.itemId) {
                R.id.navigation_tools -> {
                    // Usa navView come ancora del popup
                    val popup = PopupMenu(this, navView)
                    popup.menu.add("Cronometro")
                    popup.menu.add("Timer")

                    popup.setOnMenuItemClickListener { menuItem ->
                        when (menuItem.title) {
                            "Cronometro" -> navController.navigate(R.id.navigation_stepwatch)
                            "Timer" -> navController.navigate(R.id.navigation_timer)
                        }
                        true
                    }

                    popup.show()
                    true
                }
                else -> {
                    when (item.itemId) {
                        R.id.navigation_workout -> navController.navigate(R.id.fragment_workout)
                        else -> navController.navigate(item.itemId)
                    }
                    true
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
