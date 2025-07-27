package com.example.progetto_tosa.ui.account

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.progetto_tosa.R
import com.example.progetto_tosa.databinding.ActivityMainBinding
import com.example.progetto_tosa.workers.NotificationUtils
import com.example.progetto_tosa.workers.WeightReminderScheduler
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val REQUEST_NOTIF = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        // Tema
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        AppCompatDelegate.setDefaultNightMode(
            if (prefs.getBoolean("darkMode", true)) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Nav
        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home,
                R.id.navigation_workout
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        navView.setOnItemSelectedListener { item: MenuItem ->
            when (item.itemId) {
                R.id.navigation_home        -> { navController.navigate(R.id.navigation_home); true }
                R.id.navigation_workout     -> { navController.navigate(R.id.fragment_workout); true }
                R.id.navigation_cronotimer  -> { navController.navigate(R.id.navigation_cronotimer); true }
                R.id.navigation_account     -> { navController.navigate(R.id.navigation_account); true }
                else -> false
            }
        }

        // Deep link dalla notifica
        intent.getStringExtra("navigateTo")?.let { if (it == "account") navView.selectedItemId = R.id.navigation_account }

        // Notifiche
        NotificationUtils.createNotificationChannel(this)
        askPostNotificationPermissionIfNeeded()
        // Schedula solo se abbiamo (o non serve) il permesso
        if (NotificationUtils.canPostNotifications(this)) {
            WeightReminderScheduler.scheduleDaily(this, hour = 17, minute = 0)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    private fun askPostNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_NOTIF
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_NOTIF &&
            grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            // Ora che il permesso è concesso, schedula
            WeightReminderScheduler.scheduleDaily(this, 20, 0)
        }
    }
}
