package com.example.progetto_tosa.ui.home

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

class HomeViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _combinedStatus = MutableLiveData<String>()
    val combinedStatus: LiveData<String> = _combinedStatus

    private val TAG = "HomeViewModel"
    private val CATEGORIES = listOf("bodybuilding", "cardio", "corpo_libero", "stretching")

    fun loadData(fullName: String, todayId: String) {
        val userUid = auth.currentUser?.uid ?: return

        val ptTasks = CATEGORIES.map { category ->
            db.collection("schede_del_pt")
                .document(fullName)
                .collection(todayId)
                .document(category)
                .collection("esercizi")
                .limit(1)
                .get()
        }

        val userTasks = CATEGORIES.map { category ->
            db.collection("schede_giornaliere")
                .document(fullName) // ← NON usare uid
                .collection(todayId)
                .document(category)
                .collection("esercizi")
                .limit(1)
                .get()
        }

        // esegue entrambi in parallelo
        val allTasks = ptTasks + userTasks

        Tasks.whenAllSuccess<QuerySnapshot>(allTasks)
            .addOnSuccessListener { results ->
                val ptResults = results.take(ptTasks.size)
                val userResults = results.drop(ptTasks.size)

                val hasPtData = ptResults.any { it.documents.isNotEmpty() }
                val hasUserData = userResults.any { it.documents.isNotEmpty() }

                val message = when {
                    hasPtData && hasUserData -> "Sì! hai una scheda in programma per oggi.\nSì! il pt ha caricato una scheda."
                    hasUserData -> "Sì! hai una scheda in programma per oggi."
                    hasPtData -> "Sì! il pt ha caricato una scheda."
                    else -> "No! oggi giornata libera!"
                }

                Log.d(TAG, "pt=$hasPtData | user=$hasUserData")
                _combinedStatus.value = message
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Errore controllo esercizi", e)
                _combinedStatus.value = "Errore controllo scheda"
            }
    }
}
