// MyAutoScheduleViewModel.kt
package com.example.progetto_tosa.ui.home

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.*

class MyAutoScheduleViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val db = FirebaseFirestore.getInstance()

    val selectedDateId = MutableLiveData<String>() // data selezionata dinamicamente

    // --- LiveData per il nome del giorno ("LUNEDÌ", ecc.) ---
    private val _dayName = MutableLiveData<String>()
    val dayName: LiveData<String> = _dayName

    // --- LiveData per la lista degli esercizi (nome, categoria, docId) ---
    private val _exercises = MutableLiveData<List<ScheduledExercise>>()
    val exercises: LiveData<List<ScheduledExercise>> = _exercises

    // --- LiveData per il contatore di esercizi rimanenti ---
    private val _remaining = MutableLiveData(0)
    val remaining: LiveData<Int> = _remaining

    // --- MediatorLiveData che emette un evento (Unit) quando remaining==0 ---
    private val _notifyCompletion = MediatorLiveData<Unit>().apply {
        addSource(_remaining) { if (it == 0) value = Unit }
    }
    /** Osserva questo per lanciare la notifica di “scheda completata” */
    val notifyCompletion: LiveData<Unit> = _notifyCompletion

    // Mantengo tutte le registrazioni per rimuoverle in onCleared()
    private val listenerRegistrations = mutableListOf<ListenerRegistration>()

    // Mappa temporanea per i dati di ogni categoria
    private val categoryData = mutableMapOf<String, List<ScheduledExercise>>()

    init {
        selectedDateId.observeForever { dateStr ->
            unsubscribeFromExercises()
            computeDayName(dateStr)
            subscribeToExercises(dateStr)
        }
    }

    /** Calcola il nome del giorno della settimana dalla stringa "yyyy-MM-dd" */
    private fun computeDayName(dateStr: String) {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = fmt.parse(dateStr)!!
        val cal = Calendar.getInstance().apply { time = date }
        val names = mapOf(
            Calendar.SUNDAY to "DOMENICA", Calendar.MONDAY to "LUNEDÌ",
            Calendar.TUESDAY to "MARTEDÌ", Calendar.WEDNESDAY to "MERCOLEDÌ",
            Calendar.THURSDAY to "GIOVEDÌ", Calendar.FRIDAY to "VENERDÌ",
            Calendar.SATURDAY to "SABATO"
        )
        _dayName.value = names[cal.get(Calendar.DAY_OF_WEEK)] ?: ""
    }

    /** Registra un listener Firestore su ciascuna category/esercizi */
    private fun subscribeToExercises(dateId: String) {
        val prefs = getApplication<Application>()
            .getSharedPreferences("user_data", Context.MODE_PRIVATE)
        val user = prefs.getString("saved_display_name", null) ?: return

        val cats = listOf("bodybuilding", "cardio", "corpo-libero", "stretching")
        cats.forEach { cat ->
            val ref = db.collection("schede_giornaliere")
                .document(user)
                .collection(dateId)
                .document(cat)
                .collection("esercizi")

            val registration = ref.addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                val listForCat = snap.documents
                    .sortedBy { it.getLong("ordine") ?: 0L }
                    .map { doc ->
                        val name = doc.getString("nomeEsercizio") ?: doc.id
                        val sets = doc.getLong("numeroSerie")?.toInt() ?: 0
                        val reps = doc.getLong("numeroRipetizioni")?.toInt() ?: 0
                        val peso = doc.getString("peso")
                        ScheduledExercise(name, cat, doc.id, sets, reps, peso)
                    }

                categoryData[cat] = listForCat
                val all = cats.flatMap { categoryData[it].orEmpty() }
                _exercises.value = all
                _remaining.value = all.size
            }

            listenerRegistrations += registration
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Rimuove tutti i listener per evitare memory leak
        listenerRegistrations.forEach { it.remove() }
    }

    fun markExerciseDone(
        category: String,
        docId: String,
        onSuccess: () -> Unit,
        onError: () -> Unit
    ) {
        val prefs = getApplication<Application>()
            .getSharedPreferences("user_data", Context.MODE_PRIVATE)
        val user = prefs.getString("saved_display_name", null) ?: run {
            onError(); return
        }

        db.collection("schede_giornaliere")
            .document(user)
            .collection(selectedDateId.value!!)
            .document(category)
            .collection("esercizi")
            .document(docId)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError() }
    }

    fun saveExerciseWeight(
        category: String,
        docId: String,
        weight: String,
        onSuccess: () -> Unit,
        onError: () -> Unit
    ) {
        val prefs = getApplication<Application>()
            .getSharedPreferences("user_data", Context.MODE_PRIVATE)
        val user = prefs.getString("saved_display_name", null) ?: run {
            onError(); return
        }

        val docRef = db.collection("schede_giornaliere")
            .document(user)
            .collection(selectedDateId.value!!)
            .document(category)
            .collection("esercizi")
            .document(docId)

        docRef.update("peso", weight)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError() }
    }

    fun updateExerciseOrder(newList: List<ScheduledExercise>) {
        _exercises.value = newList
    }

    fun saveReorderedExercises(reorderedList: List<ScheduledExercise>) {
        val prefs = getApplication<Application>()
            .getSharedPreferences("user_data", Context.MODE_PRIVATE)
        val user = prefs.getString("saved_display_name", null) ?: return

        val db = FirebaseFirestore.getInstance()

        // raggruppa gli esercizi per categoria
        val grouped = reorderedList.groupBy { it.categoria }

        grouped.forEach { (categoria, exercisesInCat) ->
            val collectionRef = db.collection("schede_giornaliere")
                .document(user)
                .collection(selectedDateId.value!!)
                .document(categoria)
                .collection("esercizi")

            val batch = db.batch()

            exercisesInCat.forEachIndexed { index, exercise ->
                val docRef = collectionRef.document(exercise.docId)
                batch.update(docRef, "ordine", index)
            }

            batch.commit()
        }
    }

    private fun unsubscribeFromExercises() {
        listenerRegistrations.forEach { it.remove() }
        listenerRegistrations.clear()
        categoryData.clear()
    }
}

data class ScheduledExercise(
    val nome: String,
    val categoria: String,
    val docId: String,
    val sets: Int,
    val reps: Int,
    val peso: String? = null
)