// CardioViewModel.kt
package com.example.progetto_tosa.ui.workout

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.progetto_tosa.R
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

class CardioViewModel(application: Application) : AndroidViewModel(application) {

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

    // ----------- ANAGRAFICA: loader + mapping ------------

    fun loadAnagraficaCardioFromFirestore(onComplete: () -> Unit = {}) {
        db.collection("esercizi")
            .document("cardio")
            .collection("voci")
            .get()
            .addOnSuccessListener { snap ->
                val all = snap.documents.map { mapDocToExercise(it) }
                if (all.isNotEmpty()) _exercises.value = all
                onComplete()
            }
            .addOnFailureListener { onComplete() }
    }

    private fun nameToResId(name: String?): Int {
        if (name.isNullOrBlank()) return 0
        val r = getApplication<Application>().resources
        return r.getIdentifier(name, "drawable", getApplication<Application>().packageName)
    }

    private fun mapDocToExercise(doc: DocumentSnapshot): Exercise {
        val category       = doc.getString("category") ?: "cardio"
        val type           = doc.getString("type") ?: ""
        val title          = doc.getString("title") ?: ""
        val videoUrl       = doc.getString("videoUrl") ?: ""
        val description    = doc.getString("description") ?: ""
        val subtitle2      = doc.getString("subtitle2") ?: ""
        val description2   = doc.getString("description2") ?: ""
        val descrTot       = doc.getString("descrizioneTotale") ?: ""

        val descImgRes     = nameToResId(doc.getString("descriptionImageName"))
        val detail1Res     = nameToResId(doc.getString("detailImage1Name"))
        val detail2Res     = nameToResId(doc.getString("detailImage2Name"))

        return Exercise(
            category = category,
            type = type,
            imageRes = descImgRes,
            descriptionImage = descImgRes,
            title = title,
            videoUrl = videoUrl,
            description = description,
            subtitle2 = subtitle2,
            description2 = description2,
            detailImage1Res = detail1Res,
            detailImage2Res = detail2Res,
            descrizioneTotale = descrTot
        )
    }

    // ----------- Saved sets/reps ------------

    /** Carica sets/reps dal path utente/PT. ATTENZIONE: docId = TITOLO (coerente col salvataggio). */
    fun loadSavedExercises(selectedDate: String?, selectedUser: String?, currentUserName: String? = null) {
        if (selectedDate.isNullOrBlank()) return
        val ref = getExercisesRef(selectedDate, selectedUser, currentUserName)

        val currentList = _exercises.value!!.toMutableList()
        val snapshotList = mutableListOf<Pair<String, Pair<Int, Int>>>()

        currentList.forEach { ex ->
            // docId = TITLE (non type)
            ref.document(ex.title)
                .get()
                .addOnSuccessListener { doc ->
                    val sets = doc.getLong("numeroSerie")?.toInt() ?: 0
                    val reps = doc.getLong("numeroRipetizioni")?.toInt() ?: 0
                    snapshotList.add(ex.title to (sets to reps))

                    if (snapshotList.size == currentList.size) {
                        snapshotList.forEach { (title, sr) ->
                            currentList.find { it.title == title }?.apply {
                                setsCount = sr.first
                                repsCount = sr.second
                            }
                        }
                        _exercises.value = currentList
                    }
                }
        }
    }

    /** Path helper: {schede_del_pt|schede_giornaliere}/{utente}/{data}/cardio/esercizi */
    private fun getExercisesRef(date: String, selectedUser: String?, currentUserName: String?): CollectionReference {
        val user = selectedUser ?: currentUserName ?: getPrefsUser()
        return if (!selectedUser.isNullOrBlank()) {
            db.collection("schede_del_pt").document(user)
                .collection(date).document("cardio")
                .collection("esercizi")
        } else {
            db.collection("schede_giornaliere").document(user)
                .collection(date).document("cardio")
                .collection("esercizi")
        }
    }

    private fun getPrefsUser(): String {
        val prefs = getApplication<Application>().getSharedPreferences("user_data", Context.MODE_PRIVATE)
        return prefs.getString("saved_display_name", "") ?: ""
    }

    // ----------- Fallback statico ------------

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
            detailImage1Res = R.drawable.duecorda,
            detailImage2Res = R.drawable.salto_corda,
            descrizioneTotale = "30–60 secondi x 3 set"
        )
    )
}
