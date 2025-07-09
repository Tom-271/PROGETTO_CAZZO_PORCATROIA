package com.example.progetto_tosa.ui.workout

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.progetto_tosa.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue

class BodybuildingViewModel : ViewModel() {

    data class Exercise(
        val category: String,
        val muscoloPrincipale: String,
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

    // sezioni statiche
    private val _section1 = MutableLiveData<List<Exercise>>(loadSection1())
    val section1: LiveData<List<Exercise>> = _section1

    private val _section2 = MutableLiveData<List<Exercise>>(loadSection2())
    val section2: LiveData<List<Exercise>> = _section2

    private val _section3 = MutableLiveData<List<Exercise>>(loadSection3())
    val section3: LiveData<List<Exercise>> = _section3

    private val _section4 = MutableLiveData<List<Exercise>>(loadSection4())
    val section4: LiveData<List<Exercise>> = _section4

    private val _section5 = MutableLiveData<List<Exercise>>(loadSection5())
    val section5: LiveData<List<Exercise>> = _section5

    // carica i dati Firestore e aggiorna section1
    fun loadSavedExercises(selectedDate: String?, onComplete: () -> Unit) {
        if (selectedDate.isNullOrBlank()) return
        val currentList = _section1.value!!.toMutableList()
        val muscoli = currentList.map { it.muscoloPrincipale }.distinct()
        val snapshotList = mutableListOf<Pair<String, List<Map<String, Any>>>>()

        muscoli.forEach { muscolo ->
            db.collection("schede_giornaliere")
                .document(selectedDate)
                .collection("bodybuilding")
                .document(muscolo)
                .collection("esercizi")
                .orderBy("createdAt")
                .get()
                .addOnSuccessListener { snap ->
                    val esercizi = snap.documents.mapNotNull { doc ->
                        val title = doc.id
                        val sets = doc.getLong("numeroSerie")?.toInt() ?: 0
                        val reps = doc.getLong("numeroRipetizioni")?.toInt() ?: 0
                        mapOf("title" to title, "sets" to sets, "reps" to reps)
                    }
                    snapshotList.add(muscolo to esercizi)
                    if (snapshotList.size == muscoli.size) {
                        // aggiorna counts
                        snapshotList.forEach { (_, list) ->
                            list.forEach { entry ->
                                currentList.find { it.title == entry["title"] }?.apply {
                                    setsCount = entry["sets"] as Int
                                    repsCount = entry["reps"] as Int
                                }
                            }
                        }
                        _section1.value = currentList
                        onComplete()
                    }
                }
                .addOnFailureListener { /* gestione errore se serve */ }
        }
    }

    // helper per la factory delle liste
    private fun loadSection1() = listOf(
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "petto",
            imageRes = R.drawable.pancadescrizione,
            descriptionImage = R.drawable.pancadescrizione,
            title = "PANCA PIANA",
            videoUrl = "https://www.youtube.com/watch?v=nclAIgM4NJE",
            description = "Esercizio fondamentale per il petto.",
            subtitle2 = "MUSCOLI COINVOLTI",
            description2 = "- Grande pettorale\n- Tricipite brachiale",
            detailImage1Res = R.drawable.pancadescrizione,
            detailImage2Res = R.drawable.pancadescrizione,
            descrizioneTotale = "3–4 serie da 8–12 ripetizioni"
        ),
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "petto",
            imageRes = R.drawable.chestpressdescrizione,
            descriptionImage = R.drawable.chestpressdescrizione,
            title = "CHEST PRESS",
            videoUrl = "https://www.youtube.com/watch?v=lAkcFUulVn4",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI COINVOLTI",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.chestpressdescrizione,
            detailImage2Res = R.drawable.chestpressdescrizione,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        ),
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "petto",
            imageRes = R.drawable.spinte,
            descriptionImage = R.drawable.spinte,
            title = "SPINTE SU PANCA INCLINATA",
            videoUrl = "https://www.youtube.com/watch?v=n75xKtO7ppU",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI COINVOLTI",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.spinte,
            detailImage2Res = R.drawable.spinte,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        ),
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "petto",
            imageRes = R.drawable.crocicavi,
            descriptionImage = R.drawable.crocicavi,
            title = "CROCI AI CAVI ALTI",
            videoUrl = "https://www.youtube.com/watch?v=d7B7bXZr26c",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI COINVOLTI",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.crocicavi,
            detailImage2Res = R.drawable.crocicavi,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        ),
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "petto",
            imageRes = R.drawable.crocipiana,
            descriptionImage = R.drawable.crocipiana,
            title = "CROCI SU PANCA PIANA",
            videoUrl = "https://www.youtube.com/watch?v=d7B7bXZr26c",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI COINVOLTI",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.crocipiana,
            detailImage2Res = R.drawable.crocipiana,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        ),
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "petto",
            imageRes = R.drawable.peckdeck,
            descriptionImage = R.drawable.peckdeck,
            title = "PECK DECK",
            videoUrl = "https://www.youtube.com/watch?v=6dExVifvwR8",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI COINVOLTI",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.peckdeck,
            detailImage2Res = R.drawable.peckdeck,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        ),
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "petto",
            imageRes = R.drawable.dipparallele,
            descriptionImage = R.drawable.dipparallele,
            title = "DIP ALLE PARALLELE",
            videoUrl = "https://www.youtube.com/watch?v=SLVwguvd6io",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI COINVOLTI",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.dipparallele,
            detailImage2Res = R.drawable.dipparallele,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        )
    )

    private fun loadSection2() = listOf(
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "spalle",
            imageRes = R.drawable.lentoavantimanubri,
            descriptionImage = R.drawable.lentoavantimanubri,
            title = "LENTO AVANTI CON MANUBRI",
            videoUrl = "https://www.youtube.com/watch?v=AfIJ6VwYR5g",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI COINVOLTI",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.lentoavantimanubri,
            detailImage2Res = R.drawable.lentoavantimanubri,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        ),
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "spalle",
            imageRes = R.drawable.alzatelaterali,
            descriptionImage = R.drawable.alzatelaterali,
            title = "ALZATE LATERALI",
            videoUrl = "https://www.youtube.com/watch?v=Z-67PLUt43E",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI COINVOLTI",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.alzatelaterali,
            detailImage2Res = R.drawable.alzatelaterali,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        ),
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "spalle",
            imageRes = R.drawable.alzatefrontali,
            descriptionImage = R.drawable.alzatefrontali,
            title = "ALZATE FRONTALI",
            videoUrl = "https://www.youtube.com/watch?v=Z-67PLUt43E",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI COINVOLTI",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.alzatefrontali,
            detailImage2Res = R.drawable.alzatefrontali,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        ),
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "spalle",
            imageRes = R.drawable.tiratealmento,
            descriptionImage = R.drawable.tiratealmento,
            title = "TIRATE AL MENTO",
            videoUrl = "https://www.youtube.com/watch?v=jTMo4r9FLD8",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI COINVOLTI",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.tiratealmento,
            detailImage2Res = R.drawable.tiratealmento,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        ),
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "spalle",
            imageRes = R.drawable.deltoideposteriore,
            descriptionImage = R.drawable.deltoideposteriore,
            title = "DELTOIDE POSTERIORE",
            videoUrl = "https://www.youtube.com/watch?v=Uxf3ATJOXEg",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI COINVOLTI",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.deltoideposteriore,
            detailImage2Res = R.drawable.deltoideposteriore,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        )
    )

    private fun loadSection3() = listOf(
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "schiena",
            imageRes = R.drawable.latmachinedritta,
            descriptionImage = R.drawable.latmachinedritta,
            title = "LAT MACHINE PRESA LARGA",
            videoUrl = "https://youtu.be/...",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI COINVOLTI",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.latmachinedritta,
            detailImage2Res = R.drawable.latmachinedritta,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        ),
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "schiena",
            imageRes = R.drawable.latmachineinversa,
            descriptionImage = R.drawable.latmachineinversa,
            title = "LAT MACHINE PRESA INVERSA",
            videoUrl = "https://youtu.be/...",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI COINVOLTI",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.latmachineinversa,
            detailImage2Res = R.drawable.latmachineinversa,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        ),
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "schiena",
            imageRes = R.drawable.latmachineneutra,
            descriptionImage = R.drawable.latmachineneutra,
            title = "LAT MACHINE PRESA NEUTRA",
            videoUrl = "https://youtu.be/...",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI COINVOLTI",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.latmachineneutra,
            detailImage2Res = R.drawable.latmachineneutra,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        ),
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "schiena",
            imageRes = R.drawable.tiratealmento,
            descriptionImage = R.drawable.tiratealmento,
            title = "TIRATE AL MENTO",
            videoUrl = "https://youtu.be/...",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI COINVOLTI",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.tiratealmento,
            detailImage2Res = R.drawable.tiratealmento,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        ),
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "schiena",
            imageRes = R.drawable.pulleytriangolo,
            descriptionImage = R.drawable.pulleytriangolo,
            title = "PULLEY TRIANGOLO",
            videoUrl = "https://youtu.be/...",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI COINVOLTI",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.pulleytriangolo,
            detailImage2Res = R.drawable.pulleytriangolo,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        ),
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "schiena",
            imageRes = R.drawable.pulleybilancere,
            descriptionImage = R.drawable.pulleybilancere,
            title = "PULLEY BILANCERE",
            videoUrl = "https://youtu.be/...",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI COINVOLTI",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.pulleybilancere,
            detailImage2Res = R.drawable.pulleybilancere,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        ),
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "schiena",
            imageRes = R.drawable.rematorebilanciere,
            descriptionImage = R.drawable.rematorebilanciere,
            title = "REMATORE BILANCIERE",
            videoUrl = "https://youtu.be/...",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI COINVOLTI",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.rematorebilanciere,
            detailImage2Res = R.drawable.rematorebilanciere,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        ),
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "schiena",
            imageRes = R.drawable.rematoremanubrio,
            descriptionImage = R.drawable.rematoremanubrio,
            title = "REMATORE MANUBRIO",
            videoUrl = "https://youtu.be/...",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI COINVOLTI",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.rematoremanubrio,
            detailImage2Res = R.drawable.rematoremanubrio,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        ),
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "schiena",
            imageRes = R.drawable.rematoretbar,
            descriptionImage = R.drawable.rematoretbar,
            title = "REMATORE T BAR",
            videoUrl = "https://youtu.be/...",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI COINVOLTI",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.rematoretbar,
            detailImage2Res = R.drawable.rematoretbar,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        ),
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "schiena",
            imageRes = R.drawable.pulldown,
            descriptionImage = R.drawable.pulldown,
            title = "PULL DOWN",
            videoUrl = "https://youtu.be/...",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI COINVOLTI",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.pulldown,
            detailImage2Res = R.drawable.pulldown,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        ),
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "schiena",
            imageRes = R.drawable.trazionisbarra,
            descriptionImage = R.drawable.trazionisbarra,
            title = "TRAZIONI ALLA SBARRA",
            videoUrl = "https://youtu.be/...",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI COINVOLTI",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.trazionisbarra,
            detailImage2Res = R.drawable.trazionisbarra,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        ),
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "schiena",
            imageRes = R.drawable.trazionipresaneutra,
            descriptionImage = R.drawable.trazionipresaneutra,
            title = "TRAZIONI PRESA NEUTRA",
            videoUrl = "https://youtu.be/...",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI COINVOLTI",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.trazionipresaneutra,
            detailImage2Res = R.drawable.trazionipresaneutra,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        )
    )

    private fun loadSection4() = listOf(
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "gambe",
            imageRes = R.drawable.backsquat,
            descriptionImage = R.drawable.backsquat,
            title = "BACK SQUAT",
            videoUrl = "https://youtu.be/...",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI COINVOLTI",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.backsquat,
            detailImage2Res = R.drawable.backsquat,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        ),
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "gambe",
            imageRes = R.drawable.squatmultipower,
            descriptionImage = R.drawable.squatmultipower,
            title = "SQUAT AL MULTIPOWER",
            videoUrl = "https://youtu.be/...",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI COINVOLTI",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.squatmultipower,
            detailImage2Res = R.drawable.squatmultipower,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        ),
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "gambe",
            imageRes = R.drawable.legpress45,
            descriptionImage = R.drawable.legpress45,
            title = "LEG PRESS 45°",
            videoUrl = "https://youtu.be/...",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI COINVOLTI",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.legpress45,
            detailImage2Res = R.drawable.legpress45,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        ),
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "gambe",
            imageRes = R.drawable.hacksquat,
            descriptionImage = R.drawable.hacksquat,
            title = "HACK SQUAT",
            videoUrl = "https://youtu.be/...",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI COINVOLTI",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.hacksquat,
            detailImage2Res = R.drawable.hacksquat,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        ),
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "gambe",
            imageRes = R.drawable.squatbulgaro,
            descriptionImage = R.drawable.squatbulgaro,
            title = "SQUAT BULGARO",
            videoUrl = "https://youtu.be/...",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI COINVOLTI",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.squatbulgaro,
            detailImage2Res = R.drawable.squatbulgaro,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        ),
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "gambe",
            imageRes = R.drawable.squatpendulum,
            descriptionImage = R.drawable.squatpendulum,
            title = "SQUAT PENDULUM",
            videoUrl = "https://youtu.be/...",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI COINVOLTI",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.squatpendulum,
            detailImage2Res = R.drawable.squatpendulum,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        ),
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "gambe",
            imageRes = R.drawable.legextension,
            descriptionImage = R.drawable.legextension,
            title = "LEG EXTENSION",
            videoUrl = "https://youtu.be/...",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI COINVOLTI",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.legextension,
            detailImage2Res = R.drawable.legextension,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        ),
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "gambe",
            imageRes = R.drawable.rematoremanubrio,
            descriptionImage = R.drawable.rematoremanubrio,
            title = "LEG CURL SDRAIATO",
            videoUrl = "https://youtu.be/...",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI COINVOLTI",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.rematoremanubrio,
            detailImage2Res = R.drawable.rematoremanubrio,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        ),
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "gambe",
            imageRes = R.drawable.adductormachine,
            descriptionImage = R.drawable.adductormachine,
            title = "ADDUCTOR MACHINE",
            videoUrl = "https://youtu.be/...",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI COINVOLTI",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.adductormachine,
            detailImage2Res = R.drawable.adductormachine,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        ),
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "gambe",
            imageRes = R.drawable.abductormachine,
            descriptionImage = R.drawable.abductormachine,
            title = "ABDUCTOR MACHINE",
            videoUrl = "https://youtu.be/...",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI COINVOLTI",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.abductormachine,
            detailImage2Res = R.drawable.abductormachine,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        ),
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "gambe",
            imageRes = R.drawable.calfinpiedi,
            descriptionImage = R.drawable.calfinpiedi,
            title = "CALF IN PIEDI",
            videoUrl = "https://youtu.be/...",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI COINVOLTI",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.calfinpiedi,
            detailImage2Res = R.drawable.calfinpiedi,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        ),
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "gambe",
            imageRes = R.drawable.calfraise,
            descriptionImage = R.drawable.calfraise,
            title = "CALF RAISE",
            videoUrl = "https://youtu.be/...",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI COINVOLTI",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.calfraise,
            detailImage2Res = R.drawable.calfraise,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        ),
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "gambe",
            imageRes = R.drawable.calfseduto,
            descriptionImage = R.drawable.calfseduto,
            title = "CALF SEDUTO",
            videoUrl = "https://youtu.be/...",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI COINVOLTI",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.calfseduto,
            detailImage2Res = R.drawable.calfseduto,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        ),
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "gambe",
            imageRes = R.drawable.affondi,
            descriptionImage = R.drawable.affondi,
            title = "AFFONDI",
            videoUrl = "https://youtu.be/...",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI COINVOLTI",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.affondi,
            detailImage2Res = R.drawable.affondi,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        )
    )

    private fun loadSection5() = listOf(
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "bicipiti",
            imageRes = R.drawable.bicipitimanubri,
            descriptionImage = R.drawable.bicipitimanubri,
            title = "CURL MANUBRI",
            videoUrl = "https://youtu.be/...",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI COINVOLTI",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.bicipitimanubri,
            detailImage2Res = R.drawable.bicipitimanubri,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        ),
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "bicipiti",
            imageRes = R.drawable.spydercurl,
            descriptionImage = R.drawable.spydercurl,
            title = "SPYDER CURL",
            videoUrl = "https://youtu.be/...",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI COINVOLTI",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.spydercurl,
            detailImage2Res = R.drawable.spydercurl,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        ),
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "bicipiti",
            imageRes = R.drawable.pancascott,
            descriptionImage = R.drawable.pancascott,
            title = "CURL SU PANCA SCOTT",
            videoUrl = "https://youtu.be/...",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI COINVOLTI",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.pancascott,
            detailImage2Res = R.drawable.pancascott,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        ),
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "bicipiti",
            imageRes = R.drawable.curlbilanciere,
            descriptionImage = R.drawable.curlbilanciere,
            title = "CURL BILANCIERE",
            videoUrl = "https://youtu.be/...",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI COINVOLTI",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.curlbilanciere,
            detailImage2Res = R.drawable.curlbilanciere,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        )
    )
}
