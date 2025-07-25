package com.example.progetto_tosa.ui.workout

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.progetto_tosa.R
import com.google.firebase.firestore.FirebaseFirestore

class StretchingViewModel : ViewModel() {

    data class Stretch(
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

    // Sezioni statiche
    private val _neck      = MutableLiveData<List<Stretch>>(loadNeck())
    private val _shoulders = MutableLiveData<List<Stretch>>(loadShoulders())
    private val _back      = MutableLiveData<List<Stretch>>(loadBack())
    private val _legs      = MutableLiveData<List<Stretch>>(loadLegs())
    private val _arms      = MutableLiveData<List<Stretch>>(loadArms())

    val neck: LiveData<List<Stretch>>      = _neck
    val shoulders: LiveData<List<Stretch>> = _shoulders
    val back: LiveData<List<Stretch>>      = _back
    val legs: LiveData<List<Stretch>>      = _legs
    val arms: LiveData<List<Stretch>>      = _arms

    /**
     * Carica da Firestore i valori già salvati per la sezione "neck"
     * e aggiorna _neck, poi richiama onComplete().
     */
    fun loadSavedStretches(
        selectedDate: String?,
        onComplete: () -> Unit
    ) {
        if (selectedDate.isNullOrBlank()) return

        val currentList = _neck.value!!.toMutableList()
        val types = currentList.map { it.type }
        val snapshotList = mutableListOf<Pair<String, Pair<Int,Int>>>()

        types.forEach { type ->
            db.collection("schede_giornaliere")
                .document(selectedDate)
                .collection("stretching")
                .document("esercizi")
                .collection("esercizi")
                .document(type)
                .get()
                .addOnSuccessListener { doc ->
                    val sets = doc.getLong("numeroSerie")?.toInt() ?: 0
                    val reps = doc.getLong("numeroRipetizioni")?.toInt() ?: 0
                    snapshotList += type to (sets to reps)
                    if (snapshotList.size == types.size) {
                        snapshotList.forEach { (t, sr) ->
                            currentList.find { it.type == t }?.apply {
                                setsCount = sr.first
                                repsCount = sr.second
                            }
                        }
                        _neck.value = currentList
                        onComplete()
                    }
                }
                .addOnFailureListener {
                    // eventuale gestione errori
                }
        }
    }

    // --- Helpers per le liste statiche ---

    private fun loadNeck() = listOf(
        Stretch(
            type               = "neck_side",
            imageRes           = R.drawable.nivea,
            descriptionImage   = R.drawable.nivea,
            title              = "Side Neck Stretch",
            videoUrl           = "https://www.youtube.com/watch?v=s2XtMJP6XcI",
            description        = "Inclina la testa verso una spalla, mantieni 20s.",
            subtitle2          = "BENEFICI",
            description2       = "- Aumenta la flessibilità del collo\n- Riduce tensione",
            detailImage1Res    = R.drawable.belinchecollo,
            detailImage2Res    = R.drawable.treadmill2,
            descrizioneTotale  = "Ripeti 3 volte per lato"
        ),
        Stretch(
            type               = "neck_forward",
            imageRes           = R.drawable.nivea,
            descriptionImage   = R.drawable.nivea,
            title              = "Forward Neck Stretch",
            videoUrl           = "https://www.youtube.com/watch?v=s2XtMJP6XcI",
            description        = "Porta il mento verso il petto, mantieni 20s.",
            subtitle2          = "BENEFICI",
            description2       = "- Allunga muscoli posteriori del collo\n- Migliora postura",
            detailImage1Res    = R.drawable.belinchecollo,
            detailImage2Res    = R.drawable.belinchebraccino,
            descrizioneTotale  = "Ripeti 3 volte"
        )
    )

    private fun loadShoulders() = listOf(
        Stretch(
            type               = "shoulder_cross",
            imageRes           = R.drawable.belinchecollo,
            descriptionImage   = R.drawable.ahahaha,
            title              = "Cross-Body Shoulder",
            videoUrl           = "https://www.youtube.com/watch?app=desktop&v=nrZzInPLiK8",
            description        = "Porta un braccio al petto, mantieni 20s.",
            subtitle2          = "BENEFICI",
            description2       = "- Allunga deltoidi posteriori\n- Allevia tensione spalle",
            detailImage1Res    = R.drawable.aahaha2,
            detailImage2Res    = R.drawable.ebboh,
            descrizioneTotale  = "Ripeti 3 volte per braccio"
        ),
        Stretch(
            type               = "shoulder_tricep",
            imageRes           = R.drawable.stretch_arms,
            descriptionImage   = R.drawable.tricio,
            title              = "Tricep Stretch",
            videoUrl           = "https://www.youtube.com/watch?app=desktop&v=nrZzInPLiK8",
            description        = "Piega il gomito dietro la testa, mantieni 20s.",
            subtitle2          = "BENEFICI",
            description2       = "- Allunga tricipiti\n- Migliora mobilità spalle",
            detailImage1Res    = R.drawable.ebboh,
            detailImage2Res    = R.drawable.belinchebraccino,
            descrizioneTotale  = "Ripeti 3 volte per braccio"
        )
    )

    private fun loadBack() = listOf(
        Stretch(
            type               = "cat_cow",
            imageRes           = R.drawable.stretch_back,
            descriptionImage   = R.drawable.gatto3,
            title              = "Cat-Cow",
            videoUrl           = "https://www.youtube.com/watch?v=hmHbBReLWQ8",
            description        = "Alterna gobba e inarcata, 8 ripetizioni.",
            subtitle2          = "BENEFICI",
            description2       = "- Mobilizza la colonna vertebrale\n- Riscalda schiena",
            detailImage1Res    = R.drawable.gatto2,
            detailImage2Res    = R.drawable.gattone,
            descrizioneTotale  = "2 serie da 8 ripetizioni"
        ),
        Stretch(
            type               = "child_pose",
            imageRes           = R.drawable.stretch_back,
            descriptionImage   = R.drawable.child,
            title              = "Child’s Pose",
            videoUrl           = "https://www.youtube.com/watch?v=hmHbBReLWQ8",
            description        = "Seduto sui talloni, braccia in avanti, 30s.",
            subtitle2          = "BENEFICI",
            description2       = "- Allunga schiena e fianchi\n- Favorisce rilassamento",
            detailImage1Res    = R.drawable.child2,
            detailImage2Res    = R.drawable.child3,
            descrizioneTotale  = "Mantieni 30s"
        )
    )

    private fun loadLegs() = listOf(
        Stretch(
            type               = "hamstring",
            imageRes           = R.drawable.stretch_legs,
            descriptionImage   = R.drawable.gamba,
            title              = "Hamstring Stretch",
            videoUrl           = "https://www.youtube.com/watch?v=hmHbBReLWQ8",
            description        = "Gamba distesa, busto in avanti, 20s.",
            subtitle2          = "BENEFICI",
            description2       = "- Allunga ischiocrurali\n- Migliora flessibilità gambe",
            detailImage1Res    = R.drawable.gamba2,
            detailImage2Res    = R.drawable.gamba3,
            descrizioneTotale  = "Ripeti 3 volte per gamba"
        ),
        Stretch(
            type               = "quad",
            imageRes           = R.drawable.stretch_legs,
            descriptionImage   = R.drawable.quad,
            title              = "Quad Stretch",
            videoUrl           = "https://www.youtube.com/watch?v=ELu4rhf5LCw",
            description        = "Tira un piede al gluteo, mantieni 20s.",
            subtitle2          = "BENEFICI",
            description2       = "- Allunga quadricipiti\n- Migliora mobilità anca",
            detailImage1Res    = R.drawable.quad2,
            detailImage2Res    = R.drawable.quad3,
            descrizioneTotale  = "Ripeti 3 volte per gamba"
        )
    )

    private fun loadArms() = listOf(
        Stretch(
            type               = "wrist",
            imageRes           = R.drawable.stretch_arms,
            descriptionImage   = R.drawable.wrist,
            title              = "Wrist Stretch",
            videoUrl           = "https://www.youtube.com/watch?v=hmHbBReLWQ8",
            description        = "Tira indietro le dita, mantieni 20s.",
            subtitle2          = "BENEFICI",
            description2       = "- Riduce tensione polsi\n- Previene infiammazioni",
            detailImage1Res    = R.drawable.wrist2,
            detailImage2Res    = R.drawable.wrist3,
            descrizioneTotale  = "Mantieni 20s"
        ),
        Stretch(
            type               = "bicep_wall",
            imageRes           = R.drawable.stretch_arms,
            descriptionImage   = R.drawable.muro,
            title              = "Bicep Wall Stretch",
            videoUrl           = "https://www.youtube.com/watch?v=hmHbBReLWQ8",
            description        = "Mano al muro, mantieni 20s.",
            subtitle2          = "BENEFICI",
            description2       = "- Allunga bicipiti\n- Migliora postura spalle",
            detailImage1Res    = R.drawable.muro2,
            detailImage2Res    = R.drawable.muro3,
            descrizioneTotale  = "Mantieni 20s"
        )
    )
}
