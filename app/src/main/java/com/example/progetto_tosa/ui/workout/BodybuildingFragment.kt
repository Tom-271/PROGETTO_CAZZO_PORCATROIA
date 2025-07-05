package com.example.progetto_tosa.ui.workout

import android.content.res.ColorStateList
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.progetto_tosa.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore

class BodybuildingFragment : Fragment(R.layout.fragment_bodybuilding) {

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
    private val selectedUser: String? by lazy { arguments?.getString("selectedUser") }
    private val selectedDate: String? by lazy { arguments?.getString("selectedDate") }

    // === PETTO ===

    private val section1 = listOf(
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "petto",
            imageRes = R.drawable.pancadescrizione,
            descriptionImage = R.drawable.pancadescrizione,
            title = "PANCA PIANA",
            videoUrl = "https://youtu.be/...",
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
            videoUrl = "https://youtu.be/...",
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
            videoUrl = "https://youtu.be/...",
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
            videoUrl = "https://youtu.be/...",
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
            videoUrl = "https://youtu.be/...",
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
            videoUrl = "https://youtu.be/...",
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
            videoUrl = "https://youtu.be/...",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI COINVOLTI",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.dipparallele,
            detailImage2Res = R.drawable.dipparallele,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        )
    )

    // === SPALLE ===

    private val section2 = listOf(
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "spalle",
            imageRes = R.drawable.lentoavantimanubri,
            descriptionImage = R.drawable.lentoavantimanubri,
            title = "LENTO AVANTI CON MANUBRI",
            videoUrl = "https://youtu.be/...",
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
            videoUrl = "https://youtu.be/...",
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
            videoUrl = "https://youtu.be/...",
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
            muscoloPrincipale = "spalle",
            imageRes = R.drawable.deltoideposteriore,
            descriptionImage = R.drawable.deltoideposteriore,
            title = "DELTOIDE POSTERIORE",
            videoUrl = "https://youtu.be/...",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI COINVOLTI",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.deltoideposteriore,
            detailImage2Res = R.drawable.deltoideposteriore,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        )
    )

    // === SCHIENA ===

    private val section3 = listOf(
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

    private val section4 = listOf(
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

    private val section5 = listOf(
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
            title = "",
            videoUrl = "https://youtu.be/...",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI COINVOLTI",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.curlbilanciere,
            detailImage2Res = R.drawable.curlbilanciere,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        )
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUI(view)
    }

    private fun initUI(root: View) {
        applyStrokeColor(root)
        setupSection(R.id.cardSection1, R.id.rvSection1, section1)
        setupSection(R.id.cardSection2, R.id.rvSection2, section2)
        setupSection(R.id.cardSection3, R.id.rvSection3, section3)
        setupSection(R.id.cardSection4, R.id.rvSection4, section4)
        setupSection(R.id.cardSection5, R.id.rvSection5, section5)
        loadSavedExercises()
    }

    private fun applyStrokeColor(root: View) {
        val night = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val colorRes = if (night == Configuration.UI_MODE_NIGHT_YES) R.color.dark_gray else R.color.black
        root.findViewById<MaterialCardView>(R.id.cardSection1).strokeColor =
            ContextCompat.getColor(requireContext(), colorRes)
    }

    private fun setupSection(headerId: Int, recyclerId: Int, data: List<Exercise>) {
        val headerCard = requireView().findViewById<MaterialCardView>(headerId)
        val recyclerView = requireView().findViewById<RecyclerView>(recyclerId).apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = ExerciseAdapter(data, ::openDetail, ::saveExercise)
        }

        // Colore stroke dinamico
        val night = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val strokeColor = ContextCompat.getColor(
            requireContext(), if (night == Configuration.UI_MODE_NIGHT_YES) R.color.black else R.color.black
        )
        headerCard.strokeColor = strokeColor

        headerCard.setOnClickListener {
            recyclerView.visibility = if (recyclerView.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }
    }

    private inner class ExerciseAdapter(
        private val items: List<Exercise>,
        private val onCardClick: (Exercise) -> Unit,
        private val onConfirmClick: (Exercise) -> Unit
    ) : RecyclerView.Adapter<ExerciseAdapter.VH>() {

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            private val titleTv = view.findViewById<TextView>(R.id.textViewTitleTop)
            private val btnSets = view.findViewById<MaterialButton>(R.id.toggleSets)
            private val btnReps = view.findViewById<MaterialButton>(R.id.toggleReps)
            private val counterSets = view.findViewById<TextView>(R.id.counterSets)
            private val counterReps = view.findViewById<TextView>(R.id.counterReps)
            private val btnMinus = view.findViewById<FloatingActionButton>(R.id.buttonMinus)
            private val btnPlus = view.findViewById<FloatingActionButton>(R.id.buttonPlus)
            private val btnConfirm = view.findViewById<MaterialButton>(R.id.buttonConfirm)
            private val green = ContextCompat.getColor(view.context, R.color.green)
            private val black = ContextCompat.getColor(view.context, R.color.black)

            init {
                btnConfirm.setOnClickListener {
                    val pos = adapterPosition.takeIf { it != RecyclerView.NO_POSITION } ?: return@setOnClickListener
                    onConfirmClick(items[pos])
                }
                btnSets.setOnClickListener { toggleMode(true) }
                btnReps.setOnClickListener { toggleMode(false) }
                btnPlus.setOnClickListener { adjustCount(1) }
                btnMinus.setOnClickListener { adjustCount(-1) }
            }

            private fun toggleMode(isSets: Boolean) {
                val pos = adapterPosition.takeIf { it != RecyclerView.NO_POSITION } ?: return
                items[pos].isSetsMode = isSets
                notifyItemChanged(pos)
            }

            private fun adjustCount(delta: Int) {
                val pos = adapterPosition.takeIf { it != RecyclerView.NO_POSITION } ?: return
                val ex = items[pos]
                if (ex.isSetsMode) ex.setsCount = maxOf(0, ex.setsCount + delta)
                else ex.repsCount = maxOf(0, ex.repsCount + delta)
                notifyItemChanged(pos)
            }

            fun bind(ex: Exercise) {
                titleTv.text = ex.title
                counterSets.text = ex.setsCount.toString()
                counterReps.text = ex.repsCount.toString()
                btnSets.isChecked = ex.isSetsMode
                btnReps.isChecked = !ex.isSetsMode
                btnSets.backgroundTintList = ColorStateList.valueOf(if (ex.isSetsMode) green else black)
                btnReps.backgroundTintList = ColorStateList.valueOf(if (ex.isSetsMode) black else green)

                val isTracking = selectedDate != null
                listOf(btnSets, btnReps, btnPlus, btnMinus, btnConfirm).forEach {
                    it.visibility = if (isTracking) View.VISIBLE else View.GONE
                }
                counterSets.visibility = if (isTracking && ex.isSetsMode) View.VISIBLE else View.GONE
                counterReps.visibility = if (isTracking && !ex.isSetsMode) View.VISIBLE else View.GONE

                itemView.setOnClickListener {
                    if (!isTracking) onCardClick(ex)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
            LayoutInflater.from(parent.context).inflate(R.layout.cards_exercise, parent, false)
        )

        override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])
        override fun getItemCount() = items.size
    }

    private fun openDetail(ex: Exercise) {
        ExerciseDetailFragment.newInstance(
            ex.title,
            ex.videoUrl,
            ex.description,
            ex.subtitle2,
            ex.description2,
            ex.detailImage1Res,
            ex.detailImage2Res,
            ex.descriptionImage,
            ex.descrizioneTotale
        ).show((requireActivity() as FragmentActivity).supportFragmentManager, "exercise_detail")
    }

    private fun saveExercise(ex: Exercise) {
        if (selectedDate.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Seleziona prima una data per aggiungere l'esercizio", Toast.LENGTH_SHORT).show()
            return
        }
        if (ex.setsCount <= 0 || ex.repsCount <= 0) {
            Toast.makeText(requireContext(), "Imposta almeno 1 serie e 1 ripetizione", Toast.LENGTH_SHORT).show()
            return
        }

        val data = hashMapOf(
            "category" to ex.category,
            "nomeEsercizio" to ex.title,
            "numeroSerie" to ex.setsCount,
            "numeroRipetizioni" to ex.repsCount,
            "muscoloPrincipale" to ex.muscoloPrincipale,
            "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )

        val successMessage = "Esercizio \"${ex.title}\" aggiunto con successo"
        val currentUserName = requireActivity()
            .getSharedPreferences("user_data", Context.MODE_PRIVATE)
            .getString("saved_display_name", null)

        val collectionRef = if (!selectedUser.isNullOrBlank()) {
            db.collection("schede_del_pt")
                .document(selectedUser!!)
                .collection(selectedDate!!)
                .document(ex.category)
                .collection("esercizi")
        } else if (!currentUserName.isNullOrBlank()) {
            db.collection("schede_giornaliere")
                .document(currentUserName)
                .collection(selectedDate!!)
                .document(ex.category)
                .collection("esercizi")
        } else {
            Toast.makeText(requireContext(), "Impossibile identificare l'utente", Toast.LENGTH_SHORT).show()
            return
        }

        collectionRef.document(ex.title)
            .set(data)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), successMessage, Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Errore durante il salvataggio", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadSavedExercises() {
        if (selectedDate.isNullOrBlank()) return

        val muscoliPrincipali = section1.map { it.muscoloPrincipale }.distinct()
        val snapshotList = mutableListOf<Pair<String, List<Map<String, Any>>>>()

        muscoliPrincipali.forEach { muscolo ->
            db.collection("schede_giornaliere")
                .document(selectedDate!!)
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

                        mapOf(
                            "title" to title,
                            "sets" to sets,
                            "reps" to reps
                        )
                    }

                    snapshotList.add(muscolo to esercizi)

                    if (snapshotList.size == muscoliPrincipali.size) {
                        // aggiorna sets e reps in section1, mantenendo gli oggetti originali
                        snapshotList.forEach { (_, esercizi) ->
                            esercizi.forEach { entry ->
                                val title = entry["title"] as? String ?: return@forEach
                                val sets = entry["sets"] as? Int ?: return@forEach
                                val reps = entry["reps"] as? Int ?: return@forEach

                                section1.find { it.title == title }?.apply {
                                    setsCount = sets
                                    repsCount = reps
                                }
                            }
                        }

                        // ordina section1 seguendo l'ordine di Firebase
                        val orderedTitles = snapshotList.flatMap { it.second }.mapNotNull { it["title"] as? String }
                        val orderedSection = orderedTitles.mapNotNull { title -> section1.find { it.title == title } }

                        // finalmente carica tutto visivamente
                        setupSection(R.id.cardSection1, R.id.rvSection1, section1)
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Errore caricamento $muscolo", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
