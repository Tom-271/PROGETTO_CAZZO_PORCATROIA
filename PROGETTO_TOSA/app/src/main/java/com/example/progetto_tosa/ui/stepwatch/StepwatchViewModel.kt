package com.example.progetto_tosa.ui.stepwatch

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class StepwatchViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is stepwatch Fragment"
    }
    val text: LiveData<String> = _text
}