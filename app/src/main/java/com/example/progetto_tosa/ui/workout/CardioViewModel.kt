package com.example.progetto_tosa.ui.workout

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CardioViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is bodybuilding Fragment"
    }
    val text: LiveData<String> = _text
}