package com.example.progetto_tosa.ui.workout

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CorpoliberoViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is corpolibero Fragment"
    }
    val text: LiveData<String> = _text
}