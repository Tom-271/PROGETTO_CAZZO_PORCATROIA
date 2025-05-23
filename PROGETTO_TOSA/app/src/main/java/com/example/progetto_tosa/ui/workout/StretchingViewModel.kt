package com.example.progetto_tosa.ui.workout

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class StretchingViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is stretching Fragment"
    }
    val text: LiveData<String> = _text
}