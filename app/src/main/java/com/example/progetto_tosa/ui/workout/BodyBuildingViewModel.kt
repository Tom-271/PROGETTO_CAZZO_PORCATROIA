package com.example.progetto_tosa.ui.workout

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.progetto_tosa.R
import com.google.firebase.firestore.FirebaseFirestore

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
            subtitle2 = "MUSCOLI :",
            description2 = "- Grande pettorale\n- Tricipite brachiale",
            detailImage1Res = R.drawable.papa,
            detailImage2Res = R.drawable.papadue,
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
            subtitle2 = "MUSCOLI :",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.chch,
            detailImage2Res = R.drawable.werwer,
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
            subtitle2 = "MUSCOLI :",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.spispi,
            detailImage2Res = R.drawable.spaspa,
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
            subtitle2 = "MUSCOLI :",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.crocro,
            detailImage2Res = R.drawable.crocrodue,
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
            subtitle2 = "MUSCOLI :",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.crecre,
            detailImage2Res = R.drawable.crcr,
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
            subtitle2 = "MUSCOLI :",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.pp,
            detailImage2Res = R.drawable.psps,
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
            subtitle2 = "MUSCOLI :",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.dippdipp,
            detailImage2Res = R.drawable.werwerwer,
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
            subtitle2 = "MUSCOLI :",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.spalleee,
            detailImage2Res = R.drawable.lentissimo,
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
            subtitle2 = "MUSCOLI :",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.lateral,
            detailImage2Res = R.drawable.militare,
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
            subtitle2 = "MUSCOLI :",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.rovazzi,
            detailImage2Res = R.drawable.chespalla,
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
            subtitle2 = "MUSCOLI :",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.menton,
            detailImage2Res = R.drawable.mentonis,
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
            subtitle2 = "MUSCOLI :",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.posterior,
            detailImage2Res = R.drawable.pelato,
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
            videoUrl = "https://www.youtube.com/watch?v=NL6Lqd6nU-g",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI :",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.latma,
            detailImage2Res = R.drawable.latmachineneutra,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        ),
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "schiena",
            imageRes = R.drawable.latmachineinversa,
            descriptionImage = R.drawable.latmachineinversa,
            title = "LAT MACHINE PRESA INVERSA",
            videoUrl = "https://www.youtube.com/watch?v=NL6Lqd6nU-g",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI :",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.latma,
            detailImage2Res = R.drawable.latinv,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        ),
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "schiena",
            imageRes = R.drawable.latmachineneutra,
            descriptionImage = R.drawable.latmachineneutra,
            title = "LAT MACHINE PRESA NEUTRA",
            videoUrl = "https://www.youtube.com/watch?v=NL6Lqd6nU-g",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI :",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.latma,
            detailImage2Res = R.drawable.latmachineneutra,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        ),
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "schiena",
            imageRes = R.drawable.tiratealmento,
            descriptionImage = R.drawable.tiratealmento,
            title = "TIRATE AL MENTO",
            videoUrl = "https://www.youtube.com/watch?v=NL6Lqd6nU-g",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI :",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.tiraaaaaa,
            detailImage2Res = R.drawable.tira,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        ),
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "schiena",
            imageRes = R.drawable.pulleytriangolo,
            descriptionImage = R.drawable.pulleytriangolo,
            title = "PULLEY TRIANGOLO",
            videoUrl = "https://www.youtube.com/watch?v=nqnBYu9bZTI",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI :",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.pulpul,
            detailImage2Res = R.drawable.sasasa,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        ),
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "schiena",
            imageRes = R.drawable.pulleybilancere,
            descriptionImage = R.drawable.pulleybilancere,
            title = "PULLEY BILANCERE",
            videoUrl = "https://www.youtube.com/watch?v=8JZoZoQoPsI",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI :",
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
            videoUrl = "https://www.youtube.com/watch?v=8JZoZoQoPsI",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI :",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.rasg,
            detailImage2Res = R.drawable.bibi,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        ),
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "schiena",
            imageRes = R.drawable.rematoremanubrio,
            descriptionImage = R.drawable.rematoremanubrio,
            title = "REMATORE MANUBRIO",
            videoUrl = "https://www.youtube.com/watch?v=BioWHk5PxVE",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI :",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.rasg,
            detailImage2Res = R.drawable.remmab,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        ),
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "schiena",
            imageRes = R.drawable.rematoretbar,
            descriptionImage = R.drawable.rematoretbar,
            title = "REMATORE T BAR",
            videoUrl = "https://www.youtube.com/watch?v=BioWHk5PxVE",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI :",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.tbar,
            detailImage2Res = R.drawable.trapbar,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        ),
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "schiena",
            imageRes = R.drawable.pulldown,
            descriptionImage = R.drawable.pulldown,
            title = "PULL DOWN",
            videoUrl = "https://www.youtube.com/watch?v=IfgUF3RbutE",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI :",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.pulldownnnnn,
            detailImage2Res = R.drawable.raraear,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        ),
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "schiena",
            imageRes = R.drawable.trazionisbarra,
            descriptionImage = R.drawable.trazionisbarra,
            title = "TRAZIONI ALLA SBARRA",
            videoUrl = "https://www.youtube.com/watch?v=Z5haXppd7EQ",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI :",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.belincheschiena,
            detailImage2Res = R.drawable.tratra,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        ),
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "schiena",
            imageRes = R.drawable.belincheschiena,
            descriptionImage = R.drawable.trazionipresaneutra,
            title = "TRAZIONI PRESA NEUTRA",
            videoUrl = "https://www.youtube.com/watch?v=Z5haXppd7EQ",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI :",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.tratra,
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
            videoUrl = "https://www.youtube.com/watch?v=DMUflnGM8E0",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI :",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.hacksquat,
            detailImage2Res = R.drawable.squatpendulum,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        ),
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "gambe",
            imageRes = R.drawable.squatmultipower,
            descriptionImage = R.drawable.squatmultipower,
            title = "SQUAT AL MULTIPOWER",
            videoUrl = "https://www.youtube.com/watch?v=DMUflnGM8E0",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI :",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.squatpendulum,
            detailImage2Res = R.drawable.squatbulgaro,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        ),
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "gambe",
            imageRes = R.drawable.legpress45,
            descriptionImage = R.drawable.legpress45,
            title = "LEG PRESS 45°",
            videoUrl = "https://www.youtube.com/watch?v=LMTyPl_oo38",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI :",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.prepre,
            detailImage2Res = R.drawable.presspress,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        ),
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "gambe",
            imageRes = R.drawable.hacksquat,
            descriptionImage = R.drawable.hacksquat,
            title = "HACK SQUAT",
            videoUrl = "https://www.youtube.com/watch?v=PH9uLhNr-s4",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI :",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.hacksquat,
            detailImage2Res = R.drawable.hackdue,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        ),
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "gambe",
            imageRes = R.drawable.squatbulgaro,
            descriptionImage = R.drawable.squatbulgaro,
            title = "SQUAT BULGARO",
            videoUrl = "https://www.youtube.com/watch?v=nan5BHL1kaY",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI :",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.pepe,
            detailImage2Res = R.drawable.squatpendulum,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        ),
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "gambe",
            imageRes = R.drawable.squatpendulum,
            descriptionImage = R.drawable.squatpendulum,
            title = "SQUAT PENDULUM",
            videoUrl = "https://www.youtube.com/watch?v=nan5BHL1kaY",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI :",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.pepe,
            detailImage2Res = R.drawable.squatpendulum,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        ),
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "gambe",
            imageRes = R.drawable.legextension,
            descriptionImage = R.drawable.legextension,
            title = "LEG EXTENSION",
            videoUrl = "https://www.youtube.com/watch?v=wRSr98kKUsg",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI :",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.lig,
            detailImage2Res = R.drawable.legleg,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        ),
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "gambe",
            imageRes = R.drawable.rematoremanubrio,
            descriptionImage = R.drawable.rematoremanubrio,
            title = "LEG CURL SDRAIATO",
            videoUrl = "https://www.youtube.com/watch?v=wRSr98kKUsg",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI :",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.rere,
            detailImage2Res = R.drawable.riri,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        ),
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "gambe",
            imageRes = R.drawable.adductormachine,
            descriptionImage = R.drawable.adductormachine,
            title = "ADDUCTOR MACHINE",
            videoUrl = "https://www.youtube.com/watch?v=wRSr98kKUsg",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI :",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.abab,
            detailImage2Res = R.drawable.ebeb,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        ),
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "gambe",
            imageRes = R.drawable.abductormachine,
            descriptionImage = R.drawable.abductormachine,
            title = "ABDUCTOR MACHINE",
            videoUrl = "https://www.youtube.com/watch?v=wRSr98kKUsg",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI :",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.abab,
            detailImage2Res = R.drawable.ebeb,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        ),
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "gambe",
            imageRes = R.drawable.calfinpiedi,
            descriptionImage = R.drawable.calfinpiedi,
            title = "CALF IN PIEDI",
            videoUrl = "https://www.youtube.com/watch?v=d1QmQTdt91U",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI :",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.calf_seduto,
            detailImage2Res = R.drawable.calfraise,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        ),
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "gambe",
            imageRes = R.drawable.calfraise,
            descriptionImage = R.drawable.calfraise,
            title = "CALF RAISE",
            videoUrl = "https://www.youtube.com/watch?v=d1QmQTdt91U",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI :",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.caca,
            detailImage2Res = R.drawable.calfraise,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        ),
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "gambe",
            imageRes = R.drawable.caca,
            descriptionImage = R.drawable.calfseduto,
            title = "CALF SEDUTO",
            videoUrl = "https://www.youtube.com/watch?v=d1QmQTdt91U",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI :",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.calf_seduto,
            detailImage2Res = R.drawable.caca,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        ),
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "gambe",
            imageRes = R.drawable.affondi,
            descriptionImage = R.drawable.affondi,
            title = "AFFONDI",
            videoUrl = "https://www.youtube.com/watch?v=b6Mv-lFWxy0",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI :",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.afaf,
            detailImage2Res = R.drawable.efef,
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
            videoUrl = "https://www.youtube.com/watch?v=0o5foceYAnA",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI :",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.rara,
            detailImage2Res = R.drawable.wowww,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        ),
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "bicipiti",
            imageRes = R.drawable.spydercurl,
            descriptionImage = R.drawable.ultima,
            title = "SPYDER CURL",
            videoUrl = "https://www.youtube.com/watch?v=0o5foceYAnA",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI :",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.spydercurl,
            detailImage2Res = R.drawable.spyderwe,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        ),
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "bicipiti",
            imageRes = R.drawable.curlinno,
            descriptionImage = R.drawable.pancascott,
            title = "CURL SU PANCA SCOTT",
            videoUrl = "https://www.youtube.com/watch?v=0o5foceYAnA",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI :",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.curlinno,
            detailImage2Res = R.drawable.bicipitimanubri,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        ),
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "bicipiti",
            imageRes = R.drawable.curlbilanciere,
            descriptionImage = R.drawable.curlbilanciere,
            title = "CURL BILANCIERE",
            videoUrl = "https://www.youtube.com/watch?v=0o5foceYAnA",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI :",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.curlinno,
            detailImage2Res = R.drawable.curlbilanciere,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        )
    )
}
