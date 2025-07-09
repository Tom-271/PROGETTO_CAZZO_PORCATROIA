// CorpoliberoViewModel.kt
package com.example.progetto_tosa.ui.workout

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.progetto_tosa.R
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class CorpoliberoViewModel(
    application: Application
) : AndroidViewModel(application) {

    data class Exercise(
        val category: String = "corpo-libero",
        val type: String,
        val imageRes: Int,
        val title: String,
        val videoUrl: String,
        val description: String,
        val benefit: String,
        val detailImage1Res: Int,
        val detailImage2Res: Int,
        val descrizioneTotale: String,
        var setsCount: Int = 0,
        var repsCount: Int = 0,
        var isSetsMode: Boolean = true
    )

    private val db = FirebaseFirestore.getInstance()

    private val _abs   = MutableLiveData<List<Exercise>>(loadAbs())
    val abs: LiveData<List<Exercise>> = _abs

    private val _chest = MutableLiveData<List<Exercise>>(loadChest())
    val chest: LiveData<List<Exercise>> = _chest

    private val _back  = MutableLiveData<List<Exercise>>(loadBack())
    val back: LiveData<List<Exercise>> = _back

    /** Carica da Firestore e aggiorna i counts su tutte le liste */
    fun loadSavedExercises(selectedDate: String?, selectedUser: String?) {
        if (selectedDate.isNullOrBlank()) return

        // raccogliamo tutte le reference e tutte le LiveData da aggiornare
        val refs = listOf(
            Pair(_abs,   getExercisesRef(selectedDate, selectedUser)),
            Pair(_chest, getExercisesRef(selectedDate, selectedUser)),
            Pair(_back,  getExercisesRef(selectedDate, selectedUser))
        )

        refs.forEach { (liveData, ref) ->
            ref.get()
                .addOnSuccessListener { snap ->
                    val updates = snap.documents.mapNotNull { doc ->
                        val type = doc.id
                        val sets = doc.getLong("numeroSerie")?.toInt() ?: return@mapNotNull null
                        val reps = doc.getLong("numeroRipetizioni")?.toInt() ?: return@mapNotNull null
                        type to (sets to reps)
                    }.toMap()
                    // aggiorno la lista
                    liveData.value = liveData.value!!.map { ex ->
                        updates[ex.type]?.let { (s, r) ->
                            ex.copy(setsCount = s, repsCount = r)
                        } ?: ex
                    }
                }
        }
    }

    /** Salva un esercizio su Firestore */
    fun saveExercise(ex: Exercise, selectedDate: String?, selectedUser: String?) {
        if (selectedDate.isNullOrBlank() || ex.setsCount == 0 || ex.repsCount == 0) return

        val ref = getExercisesRef(selectedDate, selectedUser)
        ref.document(ex.type)
            .set(mapOf(
                "category"          to ex.category,
                "nomeEsercizio"     to ex.title,
                "numeroSerie"       to ex.setsCount,
                "numeroRipetizioni" to ex.repsCount,
                "type"              to ex.type,
                "createdAt"         to FieldValue.serverTimestamp()
            ))
    }

    /**
     * Restituisce la collection giusta in base a selectedUser (il PT) o
     * --se null o blank-- prende saved_display_name da SharedPreferences per schede_giornaliere
     */
    private fun getExercisesRef(
        date: String,
        selectedUser: String?
    ): CollectionReference {
        val db = FirebaseFirestore.getInstance()
        return if (!selectedUser.isNullOrBlank()) {
            db.collection("schede_del_pt")
                .document(selectedUser)
                .collection(date)
                .document("corpo-libero")
                .collection("esercizi")
        } else {
            val prefs = getApplication<Application>()
                .getSharedPreferences("user_data", Context.MODE_PRIVATE)
            val user = prefs.getString("saved_display_name", "")
            db.collection("schede_giornaliere")
                .document(user!!)
                .collection(date)
                .document("corpo-libero")
                .collection("esercizi")
        }
    }

    // --- factory statiche ---
    private fun loadAbs() = listOf(
        Exercise(
            type               = "plank",
            imageRes           = R.drawable.plankdue,
            title              = "PLANK",
            videoUrl           = "https://www.youtube.com/watch?v=Is-7PPaBcsM",
            description        = "Mantieni il corpo dritto, appoggiato su gomiti e punte dei piedi, 30s.",
            benefit            = "Attiva core",
            detailImage1Res    = R.drawable.plankdue,
            detailImage2Res    = R.drawable.plank3,
            descrizioneTotale  = "3 serie da 30s"
        ),
        Exercise(
            type               = "crunch",
            imageRes           = R.drawable.crunchesroll,
            title              = "CRUNCH",
            videoUrl           = "https://www.youtube.com/watch?v=LHM2lZBi8Rg",
            description        = "Sollevamento busto da terra, contrai addominali, 15 rip.",
            benefit            = "Rafforza addominali",
            detailImage1Res    = R.drawable.crunches,
            detailImage2Res    = R.drawable.crunches3,
            descrizioneTotale  = "3 serie da 15 ripetizioni"
        )
    )

    private fun loadChest() = listOf(
        Exercise(
            type               = "push_up",
            imageRes           = R.drawable.push,
            title              = "PUSH-UP",
            videoUrl           = "https://www.youtube.com/watch?v=77ebGeXQO_g",
            description        = "Flessione a corpo libero, mani a larghezza spalle, 12 rip.",
            benefit            = "Petto & tricipiti",
            detailImage1Res    = R.drawable.push2,
            detailImage2Res    = R.drawable.push3,
            descrizioneTotale  = "3 serie da 12 ripetizioni"
        ),
        Exercise(
            type               = "diamond",
            imageRes           = R.drawable.diamond,
            title              = "DIAMOND PUSH-UP",
            videoUrl           = "https://www.youtube.com/watch?v=BVRlNzqhe8g",
            description        = "Mani a diamante sotto il petto, 10 rip.",
            benefit            = "Focalizza tricipiti",
            detailImage1Res    = R.drawable.diamond2,
            detailImage2Res    = R.drawable.diamond2,
            descrizioneTotale  = "3 serie da 10 ripetizioni"
        )
    )

    private fun loadBack() = listOf(
        Exercise(
            type               = "superman",
            imageRes           = R.drawable.super2,
            title              = "SUPERMAN",
            videoUrl           = "https://www.youtube.com/watch?v=DdFF9RBcheg",
            description        = "Da pancia a terra, alza braccia e gambe simultaneamente, 12 rip.",
            benefit            = "Schiena & glutei",
            detailImage1Res    = R.drawable.superdos,
            detailImage2Res    = R.drawable.super2,
            descrizioneTotale  = "3 serie da 12 ripetizioni"
        ),
        Exercise(
            type               = "reverse",
            imageRes           = R.drawable.libero_schiena,
            title              = "REVERSE SNOW ANGEL",
            videoUrl           = "https://www.youtube.com/watch?v=DdFF9RBcheg",
            description        = "Scivola le braccia dal basso verso l’alto, 10 rip.",
            benefit            = "Dorsali",
            detailImage1Res    = R.drawable.super2,
            detailImage2Res    = R.drawable.super2,
            descrizioneTotale  = "3 serie da 10 ripetizioni"
        )
    )
}
