package com.example.progetto_tosa.ui.home

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class MyAutoScheduleViewModel(
    application: Application,
    private val selectedDateId: String
) : AndroidViewModel(application) {

    private val db = FirebaseFirestore.getInstance()

    // Giorno della settimana per il subtitle
    private val _dayName = MutableLiveData<String>()
    val dayName: LiveData<String> = _dayName

    // Lista di esercizi: (nome, categoria, docId)
    private val _exercises = MutableLiveData<List<Triple<String, String, String>>>()
    val exercises: LiveData<List<Triple<String, String, String>>> = _exercises

    // Contatore rimanenti
    private val _remaining = MutableLiveData(0)
    /** Esposto al Fragment per verificare quando arriva a zero */
    val remaining: LiveData<Int> = _remaining

    // Notifica completamento quando remaining == 0
    private val _notifyCompletion = MediatorLiveData<Unit>().apply {
        addSource(_remaining) { if (it == 0) value = Unit }
    }
    val notifyCompletion: LiveData<Unit> = _notifyCompletion

    init {
        computeDayName()
        loadExercises()
    }

    private fun computeDayName() {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = fmt.parse(selectedDateId)!!
        val cal = Calendar.getInstance().apply { time = date }
        val names = mapOf(
            Calendar.SUNDAY    to "DOMENICA",
            Calendar.MONDAY    to "LUNEDÌ",
            Calendar.TUESDAY   to "MARTEDÌ",
            Calendar.WEDNESDAY to "MERCOLEDÌ",
            Calendar.THURSDAY  to "GIOVEDÌ",
            Calendar.FRIDAY    to "VENERDÌ",
            Calendar.SATURDAY  to "SABATO"
        )
        _dayName.value = names[cal.get(Calendar.DAY_OF_WEEK)] ?: ""
    }

    private fun loadExercises() {
        viewModelScope.launch {
            val prefs = getApplication<Application>()
                .getSharedPreferences("user_data", Context.MODE_PRIVATE)
            val user = prefs.getString("saved_display_name", null) ?: return@launch
            val cats = listOf("bodybuilding", "cardio", "corpo-libero", "stretching")
            val list = mutableListOf<Triple<String, String, String>>()
            for (cat in cats) {
                try {
                    val snap = db.collection("schede_giornaliere")
                        .document(user)
                        .collection(selectedDateId)
                        .document(cat)
                        .collection("esercizi")
                        .get()
                        .await()
                    snap.documents.forEach { doc ->
                        val name = doc.getString("nomeEsercizio") ?: doc.id
                        list += Triple(name, cat, doc.id)
                    }
                } catch (_: Exception) {}
            }
            _exercises.value = list
            _remaining.value = list.size
        }
    }

    fun markExerciseDone(
        category: String,
        docId: String,
        onSuccess: () -> Unit,
        onError: () -> Unit
    ) {
        viewModelScope.launch {
            val prefs = getApplication<Application>()
                .getSharedPreferences("user_data", Context.MODE_PRIVATE)
            val user = prefs.getString("saved_display_name", null) ?: return@launch
            try {
                db.collection("schede_giornaliere")
                    .document(user)
                    .collection(selectedDateId)
                    .document(category)
                    .collection("esercizi")
                    .document(docId)
                    .delete()
                    .await()
                // decrementa il contatore
                _remaining.value = (_remaining.value ?: 1) - 1
                onSuccess()
            } catch (_: Exception) {
                onError()
            }
        }
    }
}
