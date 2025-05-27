package com.example.progetto_tosa.ui.account

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SettingsViewModel : ViewModel(){
    val isDarkMode = MutableLiveData<Boolean>()
}