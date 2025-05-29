package com.example.progetto_tosa.ui.account

import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SettingsViewModel : ViewModel() {

    // Inizializza dallo stato corrente di AppCompatDelegate
    val isDarkMode = MutableLiveData(
        AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
    )

    // Chiama questo metodo dal Fragment per cambiare tema
    fun setDarkMode(enabled: Boolean) {
        isDarkMode.value = enabled
        AppCompatDelegate.setDefaultNightMode(
            if (enabled)
                AppCompatDelegate.MODE_NIGHT_YES
            else
                AppCompatDelegate.MODE_NIGHT_NO
        )
    }
}
