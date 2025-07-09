// CardioViewModel.kt
package com.example.progetto_tosa.ui.workout

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.progetto_tosa.R
import com.google.firebase.firestore.FirebaseFirestore

class CardioViewModel : ViewModel() {

    data class Exercise(
        val category: String = "cardio",
        val type: String,
        val imageRes: Int,
        val descriptionImage: Int,
        val title: String,
        val videoUrl: String,
        val description: String,
        val subtitle2: String,
        val description2: String,
        val detailImage1Res: Int,
        val detailImage2Res: Int,
        val descrizioneTotale: String,
        var setsCount: Int = 0,
        var repsCount: Int = 0,
        var isSetsMode: Boolean = true
    )

    private val db = FirebaseFirestore.getInstance()

    private val _exercises = MutableLiveData<List<Exercise>>(initialExercises())
    val exercises: LiveData<List<Exercise>> = _exercises

    private fun initialExercises() = listOf(
        Exercise(
            type = "Corsa",
            imageRes = R.drawable.treadmill1,
            descriptionImage = R.drawable.corsa,
            title = "TAPIS ROULANT",
            videoUrl = "https://www.youtube.com/watch?v=DwzWPvS9DG0",
            description = "Corsa sul tapis roulant per resistenza aerobica.",
            subtitle2 = "BENEFICI",
            description2 = "- Migliora capacità cardiovascolare\n- Brucia calorie",
            detailImage1Res = R.drawable.treadmill1,
            detailImage2Res = R.drawable.treadmill2,
            descrizioneTotale = "15–30 minuti a intensità moderata"
        ),
        Exercise(
            type = "Jumping_jacks",
            imageRes = R.drawable.junping,
            descriptionImage = R.drawable.junping2,
            title = "JUMPING-JACKS",
            videoUrl = "https://www.youtube.com/watch?v=K_98p0I1nD8",
            description = "Salti in accoppiata ad apertura e chiusura di gambe e braccia",
            subtitle2 = "BENEFICI",
            description2 = "- Rafforza articolazioni\n- Stimola mente e corpo",
            detailImage1Res = R.drawable.junping2,
            detailImage2Res = R.drawable.junping3,
            descrizioneTotale = "1 minuto ciascuna rep"
        ),
        Exercise(
            type = "Salto_della_corda",
            imageRes = R.drawable.salto_della_corda,
            descriptionImage = R.drawable.salto_corda,
            title = "JUMP ROPE",
            videoUrl = "https://www.youtube.com/watch?v=jUBirAI-nQU",
            description = "Salto con la corda per coordinazione e cardio veloce.",
            subtitle2 = "BENEFICI",
            description2 = "- Migliora agilità\n- Alto dispendio calorico",
            detailImage1Res = R.drawable.salto_corda,
            detailImage2Res = R.drawable.duecorda,
            descrizioneTotale = "5–10 minuti di round da 1 minuto"
        ),
        Exercise(
            type = "high_knees",
            imageRes = R.drawable.knees2,
            descriptionImage = R.drawable.salto_corda,
            title = "HIGH KNEES",
            videoUrl = "https://www.youtube.com/watch?app=desktop&v=hzLR_WrtWKQ",
            description = "Corsa sul posto portando le ginocchia alte.",
            subtitle2 = "BENEFICI",
            description2 = "- Aumenta frequenza cardiaca\n- Attiva core e gambe",
            detailImage1Res = R.drawable.salto_corda,
            detailImage2Res = R.drawable.salto_corda,
            descrizioneTotale = "30–60 secondi x 3 set"
        )
    )

    fun loadSavedExercises(selectedDate: String?, selectedUser: String?) {
        if (selectedDate.isNullOrBlank()) return
        val currentList = _exercises.value!!.toMutableList()
        val snapshotList = mutableListOf<Pair<String, Pair<Int, Int>>>()
        currentList.forEach { ex ->
            db.collection("schede_giornaliere")
                .document(selectedDate)
                .collection("cardio")
                .document(ex.type)
                .collection("esercizi")
                .orderBy("createdAt")
                .get()
                .addOnSuccessListener { snap ->
                    snap.documents.firstOrNull()?.let { doc ->
                        val sets = doc.getLong("numeroSerie")?.toInt() ?: 0
                        val reps = doc.getLong("numeroRipetizioni")?.toInt() ?: 0
                        snapshotList.add(ex.type to (sets to reps))
                    }
                    if (snapshotList.size == currentList.size) {
                        snapshotList.forEach { (type, sr) ->
                            currentList.find { it.type == type }?.apply {
                                setsCount = sr.first
                                repsCount = sr.second
                            }
                        }
                        _exercises.value = currentList
                    }
                }
                .addOnFailureListener { /* gestisci errore se serve */ }
        }
    }
}
