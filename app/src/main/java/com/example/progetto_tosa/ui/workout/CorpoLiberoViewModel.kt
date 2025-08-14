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

    /** Load saved sets/reps from Firestore, user-centric path (PT or logged user). */
    fun loadSavedExercises(selectedDate: String?, selectedUser: String?) {
        if (selectedDate.isNullOrBlank()) return

        val refs = listOf(
            Pair(_abs,   getExercisesRef(selectedDate, selectedUser)),
            Pair(_chest, getExercisesRef(selectedDate, selectedUser)),
            Pair(_back,  getExercisesRef(selectedDate, selectedUser))
        )

        refs.forEach { (liveData, ref) ->
            ref.get()
                .addOnSuccessListener { snap ->
                    val updates: Map<String, Pair<Int, Int>> = snap.documents.mapNotNull { doc ->
                        // each document id == exercise.type
                        val type = doc.id
                        val sets = doc.getLong("numeroSerie")?.toInt()
                        val reps = doc.getLong("numeroRipetizioni")?.toInt()
                        if (sets != null && reps != null) type to (sets to reps) else null
                    }.toMap()

                    liveData.value = liveData.value!!.map { ex ->
                        updates[ex.type]?.let { (s, r) -> ex.copy(setsCount = s, repsCount = r) } ?: ex
                    }
                }
                .addOnFailureListener {
                    // optional: log or expose an error state
                }
        }
    }

    /** Save one exercise for the given date and user context. */
    fun saveExercise(ex: Exercise, selectedDate: String?, selectedUser: String?) {
        if (selectedDate.isNullOrBlank() || ex.setsCount == 0 || ex.repsCount == 0) return

        val ref = getExercisesRef(selectedDate, selectedUser)
        ref.document(ex.type).set(
            mapOf(
                "category"          to ex.category,
                "nomeEsercizio"     to ex.title,
                "numeroSerie"       to ex.setsCount,
                "numeroRipetizioni" to ex.repsCount,
                "type"              to ex.type,
                "createdAt"         to FieldValue.serverTimestamp()
            )
        )
    }

    /**
     * Return the right collection based on PT (`selectedUser`) or the logged user's display name.
     * Path: {root}/{userOrPt}/{date}/"corpo-libero"/"esercizi"
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
            val user = prefs.getString("saved_display_name", "") ?: ""
            db.collection("schede_giornaliere")
                .document(user)
                .collection(date)
                .document("corpo-libero")
                .collection("esercizi")
        }
    }

    // --- static lists (unchanged) ---
    private fun loadAbs() = listOf(
        Exercise(
            type              = "plank",
            imageRes          = R.drawable.plankdue,
            title             = "PLANK",
            videoUrl          = "https://www.youtube.com/watch?v=Is-7PPaBcsM",
            description       = "Mantieni il corpo dritto, appoggiato su gomiti e punte dei piedi, 30s.",
            benefit           = "Attiva core",
            detailImage1Res   = R.drawable.plankdue,
            detailImage2Res   = R.drawable.plank3,
            descrizioneTotale = "3 serie da 30s"
        ),
        Exercise(
            type              = "crunch",
            imageRes          = R.drawable.crunchesroll,
            title             = "CRUNCH",
            videoUrl          = "https://www.youtube.com/watch?v=LHM2lZBi8Rg",
            description       = "Sollevamento busto da terra, contrai addominali, 15 rip.",
            benefit           = "Rafforza addominali",
            detailImage1Res   = R.drawable.crunches,
            detailImage2Res   = R.drawable.crunches3,
            descrizioneTotale = "3 serie da 15 ripetizioni"
        )
    )

    private fun loadChest() = listOf(
        Exercise(
            type              = "push_up",
            imageRes          = R.drawable.push,
            title             = "PUSH-UP",
            videoUrl          = "https://www.youtube.com/watch?v=77ebGeXQO_g",
            description       = "Flessione a corpo libero, mani a larghezza spalle, 12 rip.",
            benefit           = "Petto & tricipiti",
            detailImage1Res   = R.drawable.push2,
            detailImage2Res   = R.drawable.push3,
            descrizioneTotale = "3 serie da 12 ripetizioni"
        ),
        Exercise(
            type              = "diamond",
            imageRes          = R.drawable.diamond,
            title             = "DIAMOND PUSH-UP",
            videoUrl          = "https://www.youtube.com/watch?v=BVRlNzqhe8g",
            description       = "Mani a diamante sotto il petto, 10 rip.",
            benefit           = "Focalizza tricipiti",
            detailImage1Res   = R.drawable.diamond2,
            detailImage2Res   = R.drawable.diamond2,
            descrizioneTotale = "3 serie da 10 ripetizioni"
        )
    )

    private fun loadBack() = listOf(
        Exercise(
            type              = "superman",
            imageRes          = R.drawable.super2,
            title             = "SUPERMAN",
            videoUrl          = "https://www.youtube.com/watch?v=DdFF9RBcheg",
            description       = "Da pancia a terra, alza braccia e gambe simultaneamente, 12 rip.",
            benefit           = "Schiena & glutei",
            detailImage1Res   = R.drawable.superdos,
            detailImage2Res   = R.drawable.super2,
            descrizioneTotale = "3 serie da 12 ripetizioni"
        ),
        Exercise(
            type              = "reverse",
            imageRes          = R.drawable.libero_schiena,
            title             = "REVERSE SNOW ANGEL",
            videoUrl          = "https://www.youtube.com/watch?v=DdFF9RBcheg",
            description       = "Scivola le braccia dal basso verso l’alto, 10 rip.",
            benefit           = "Dorsali",
            detailImage1Res   = R.drawable.super2,
            detailImage2Res   = R.drawable.super2,
            descrizioneTotale = "3 serie da 10 ripetizioni"
        )
    )

    // converte un nome drawable in resId (in ViewModel serve Application)
    private fun nameToResId(name: String?): Int {
        if (name.isNullOrBlank()) return 0
        return getApplication<Application>().resources.getIdentifier(
            name, "drawable", getApplication<Application>().packageName
        )
    }

    // mappa un documento dell'anagrafica a Exercise
    private fun mapDocToExercise(doc: com.google.firebase.firestore.DocumentSnapshot): Exercise {
        val category          = (doc.getString("category") ?: "corpo-libero")
        val type              = (doc.getString("type") ?: "")
        val title             = (doc.getString("title") ?: "")
        val videoUrl          = (doc.getString("videoUrl") ?: "")
        val description       = (doc.getString("description") ?: "")
        val benefit           = (doc.getString("benefit") ?: "")
        val descrTot          = (doc.getString("descrizioneTotale") ?: "")

        val imageRes          = nameToResId(doc.getString("imageResName"))
        val detail1Res        = nameToResId(doc.getString("detailImage1Name"))
        val detail2Res        = nameToResId(doc.getString("detailImage2Name"))

        return Exercise(
            category         = category,
            type             = type,
            imageRes         = imageRes,
            title            = title,
            videoUrl         = videoUrl,
            description      = description,
            benefit          = benefit,
            detailImage1Res  = detail1Res,
            detailImage2Res  = detail2Res,
            descrizioneTotale= descrTot
        )
    }

    /**
     * Carica l'anagrafica da Firestore: esercizi/corpo-libero/voci
     * e popola le tre LiveData (_abs, _chest, _back) usando il campo "gruppo".
     * Se "gruppo" manca, prova a dedurlo dal type/title.
     */
    fun loadAnagraficaCorpoLiberoFromFirestore() {
        db.collection("esercizi")
            .document("corpo-libero")
            .collection("voci")
            .get()
            .addOnSuccessListener { snap ->
                val all = snap.documents.map { mapDocToExercise(it) }

                // prendi gruppo dal doc, con fallback semplice
                val absList = mutableListOf<Exercise>()
                val chestList = mutableListOf<Exercise>()
                val backList = mutableListOf<Exercise>()

                snap.documents.forEachIndexed { idx, doc ->
                    val ex = all[idx]
                    val gruppo = (doc.getString("gruppo") ?: "").lowercase()
                    when {
                        gruppo == "abs" -> absList += ex
                        gruppo == "chest" -> chestList += ex
                        gruppo == "back" -> backList += ex
                        // fallback euristico se "gruppo" non c’è
                        ex.type.contains("plank", true)
                                || ex.type.contains("crunch", true) -> absList += ex
                        ex.type.contains("push", true)
                                || ex.title.contains("PUSH", true) -> chestList += ex
                        else -> backList += ex
                    }
                }

                _abs.value = absList
                _chest.value = chestList
                _back.value = backList
            }
            .addOnFailureListener {
                // opzionale: gestisci errore
            }
    }

}
