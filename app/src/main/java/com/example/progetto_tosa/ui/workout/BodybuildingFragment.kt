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

    // Sezioni di esercizi complete
    private val section1 = listOf(
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "petto",
            imageRes = R.drawable.pancadescrizione,
            descriptionImage = R.drawable.pancadescrizione,
            title = "PANCA PIANA",
            videoUrl = "https://youtu.be/…",
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
            videoUrl = "https://youtu.be/…",
            description = "Macchinario utile per il petto.",
            subtitle2 = "MUSCOLI COINVOLTI",
            description2 = "- Grande pettorale\n- Deltoide anteriore",
            detailImage1Res = R.drawable.chestpressdescrizione,
            detailImage2Res = R.drawable.chestpressdescrizione,
            descrizioneTotale = "3–4 serie da 10–15 ripetizioni"
        ),
        // …aggiungi qui gli altri esercizi petto…
    )

    private val section2 = listOf(
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "spalle",
            imageRes = R.drawable.lentoavantimanubri,
            descriptionImage = R.drawable.lentoavantimanubri,
            title = "LENTO AVANTI CON MANUBRI",
            videoUrl = "https://youtu.be/…",
            description = "Macchinario utile per le spalle.",
            subtitle2 = "MUSCOLI COINVOLTI",
            description2 = "- Deltoide anteriore\n- Deltoide laterale",
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
            videoUrl = "https://youtu.be/…",
            description = "Esercizio per il deltoide laterale.",
            subtitle2 = "MUSCOLI COINVOLTI",
            description2 = "- Deltoide laterale",
            detailImage1Res = R.drawable.alzatelaterali,
            detailImage2Res = R.drawable.alzatelaterali,
            descrizioneTotale = "3–4 serie da 12–15 ripetizioni"
        ),
        // …aggiungi qui gli altri esercizi spalle…
    )

    private val section3 = listOf(
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "schiena",
            imageRes = R.drawable.latmachinedritta,
            descriptionImage = R.drawable.latmachinedritta,
            title = "LAT MACHINE PRESA LARGA",
            videoUrl = "https://youtu.be/…",
            description = "Esercizio per la schiena alta.",
            subtitle2 = "MUSCOLI COINVOLTI",
            description2 = "- Gran dorsale\n- Bicipiti",
            detailImage1Res = R.drawable.latmachinedritta,
            detailImage2Res = R.drawable.latmachinedritta,
            descrizioneTotale = "3–4 serie da 8–12 ripetizioni"
        ),
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "schiena",
            imageRes = R.drawable.rematorebilanciere,
            descriptionImage = R.drawable.rematorebilanciere,
            title = "REMATORE BILANCIERE",
            videoUrl = "https://youtu.be/…",
            description = "Esercizio per il gran dorsale e romboidi.",
            subtitle2 = "MUSCOLI COINVOLTI",
            description2 = "- Gran dorsale\n- Rombodi",
            detailImage1Res = R.drawable.rematorebilanciere,
            detailImage2Res = R.drawable.rematorebilanciere,
            descrizioneTotale = "3–4 serie da 8–12 ripetizioni"
        ),
        // …aggiungi qui gli altri esercizi schiena…
    )

    private val section4 = listOf(
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "gambe",
            imageRes = R.drawable.backsquat,
            descriptionImage = R.drawable.backsquat,
            title = "BACK SQUAT",
            videoUrl = "https://youtu.be/…",
            description = "Esercizio fondamentale per le gambe.",
            subtitle2 = "MUSCOLI COINVOLTI",
            description2 = "- Quadricipiti\n- Glutei",
            detailImage1Res = R.drawable.backsquat,
            detailImage2Res = R.drawable.backsquat,
            descrizioneTotale = "3–4 serie da 6–10 ripetizioni"
        ),
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "gambe",
            imageRes = R.drawable.legpress45,
            descriptionImage = R.drawable.legpress45,
            title = "LEG PRESS 45°",
            videoUrl = "https://youtu.be/…",
            description = "Macchinario per quadricipiti e glutei.",
            subtitle2 = "MUSCOLI COINVOLTI",
            description2 = "- Quadricipiti\n- Glutei",
            detailImage1Res = R.drawable.legpress45,
            detailImage2Res = R.drawable.legpress45,
            descrizioneTotale = "3–4 serie da 8–12 ripetizioni"
        ),
        // …aggiungi qui gli altri esercizi gambe…
    )

    private val section5 = listOf(
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "bicipiti",
            imageRes = R.drawable.bicipitimanubri,
            descriptionImage = R.drawable.bicipitimanubri,
            title = "CURL MANUBRI",
            videoUrl = "https://youtu.be/…",
            description = "Esercizio base per i bicipiti.",
            subtitle2 = "MUSCOLI COINVOLTI",
            description2 = "- Bicipite brachiale",
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
            videoUrl = "https://youtu.be/…",
            description = "Isolamento del bicipite in panca Scott.",
            subtitle2 = "MUSCOLI COINVOLTI",
            description2 = "- Bicipite brachiale",
            detailImage1Res = R.drawable.spydercurl,
            detailImage2Res = R.drawable.spydercurl,
            descrizioneTotale = "3–4 serie da 8–12 ripetizioni"
        )
        // …aggiungi eventualmente altri esercizi bicipiti…
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
        val colorRes = if (night == Configuration.UI_MODE_NIGHT_YES) R.color.white else R.color.black
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
            requireContext(), if (night == Configuration.UI_MODE_NIGHT_YES) R.color.white else R.color.black
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
                .get()
                .addOnSuccessListener { snap ->
                    val esercizi = snap.documents.mapNotNull { doc ->
                        mapOf(
                            "title" to doc.id,
                            "sets" to (doc.getLong("numeroSerie")?.toInt() ?: 0),
                            "reps" to (doc.getLong("numeroRipetizioni")?.toInt() ?: 0)
                        )
                    }
                    snapshotList.add(muscolo to esercizi)
                    if (snapshotList.size == muscoliPrincipali.size) {
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
                        setupSection(R.id.cardSection1, R.id.rvSection1, section1)
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Errore caricamento $muscolo", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
