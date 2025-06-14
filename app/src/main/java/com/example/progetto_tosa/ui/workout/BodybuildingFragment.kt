package com.example.progetto_tosa.ui.workout

import android.content.res.Configuration
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.progetto_tosa.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class BodybuildingFragment : Fragment(R.layout.fragment_bodybuilding) {

    /*──────────────── DATA CLASS (aggiunto muscoloPrincipale) ───────────────*/
    data class Exercise(
        val muscoloPrincipale: String,          // «Petto», «Spalle», «Gambe», …
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

    /*──────────────── Firebase ──────────────────────────────────────────────*/
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var userIsPT: Boolean = false

    /*──────────────── LISTE ORIGINALI (stesso formato) ─────────────────────*/
    private val sharedImage = R.drawable.pancadescrizione

    // 1️⃣ PETTO
    private val section1 = listOf(
        Exercise(
            muscoloPrincipale = "Petto",
            imageRes = sharedImage,
            descriptionImage = sharedImage,
            title = "PANCA PIANA",
            videoUrl = "https://youtu.be/...",
            description = "Esercizio fondamentale per il petto.",
            subtitle2 = "MUSCOLI COINVOLTI",
            description2 = "- Grande pettorale\n- Tricipite brachiale",
            detailImage1Res = sharedImage,
            detailImage2Res = sharedImage,
            descrizioneTotale = "3–4 serie da 8–12 ripetizioni"
        ),
        Exercise(
            muscoloPrincipale = "Petto",
            imageRes = sharedImage,
            descriptionImage = sharedImage,
            title = "CHEST PRESS",
            videoUrl = "https://youtu.be/...",
            description = "Macchina convergente.",
            subtitle2 = "MUSCOLI COINVOLTI",
            description2 = "- Grande pettorale",
            detailImage1Res = sharedImage,
            detailImage2Res = sharedImage,
            descrizioneTotale = "3–4 serie da 10–12 ripetizioni"
        )
    )

    // 2️⃣ SPALLE
    private val section2 = listOf(
        Exercise(
            muscoloPrincipale = "Spalle",
            imageRes = sharedImage,
            descriptionImage = sharedImage,
            title = "LENTO MANUBRI",
            videoUrl = "https://youtu.be/...",
            description = "Spinta verticale per deltoidi.",
            subtitle2 = "MUSCOLI COINVOLTI",
            description2 = "- Deltoide anteriore",
            detailImage1Res = sharedImage,
            detailImage2Res = sharedImage,
            descrizioneTotale = "4 serie da 6–10 ripetizioni"
        ),
        Exercise(
            muscoloPrincipale = "Spalle",
            imageRes = sharedImage,
            descriptionImage = sharedImage,
            title = "ALZATE LATERALI",
            videoUrl = "https://youtu.be/...",
            description = "Isolamento deltoide medio.",
            subtitle2 = "MUSCOLI COINVOLTI",
            description2 = "- Deltoide laterale",
            detailImage1Res = sharedImage,
            detailImage2Res = sharedImage,
            descrizioneTotale = "4 serie da 12–15 ripetizioni"
        )
    )

    // 3️⃣ GAMBE
    private val section3 = listOf(
        Exercise(
            muscoloPrincipale = "Gambe",
            imageRes = sharedImage,
            descriptionImage = sharedImage,
            title = "SQUAT",
            videoUrl = "https://youtu.be/...",
            description = "Re dei multiarticolari per le gambe.",
            subtitle2 = "MUSCOLI COINVOLTI",
            description2 = "- Quadricipite\n- Glutei",
            detailImage1Res = sharedImage,
            detailImage2Res = sharedImage,
            descrizioneTotale = "5 serie da 5 ripetizioni"
        ),
        Exercise(
            muscoloPrincipale = "Gambe",
            imageRes = sharedImage,
            descriptionImage = sharedImage,
            title = "LEG CURL",
            videoUrl = "https://youtu.be/...",
            description = "Isolamento femorali.",
            subtitle2 = "MUSCOLI COINVOLTI",
            description2 = "- Ischiocrurali",
            detailImage1Res = sharedImage,
            detailImage2Res = sharedImage,
            descrizioneTotale = "3 serie da 12 ripetizioni"
        )
    )

    // Per mantenere compatibilità con i tuoi ID di layout (cardSection4,5)
    private val section4 = listOf(section1[0])
    private val section5 = listOf(section1[0])

    /*──────────────── Lifecycle ─────────────────────────────────────────────*/
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth.currentUser?.uid?.let { uid ->
            db.collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    userIsPT = doc.getBoolean("isPersonalTrainer") == true
                    initUI(view)
                }
                .addOnFailureListener { initUI(view) }
        } ?: initUI(view)          // se non loggato comunque mostra esercizi
    }

    /*──────────────── UI setup  ─────────────────────────────────────────────*/
    private fun initUI(root: View) {
        applyStrokeColor(root)
        setupSection(R.id.cardSection1, R.id.rvSection1, section1) // PETTO
        setupSection(R.id.cardSection2, R.id.rvSection2, section2) // SPALLE
        setupSection(R.id.cardSection3, R.id.rvSection3, section3) // GAMBE
        setupSection(R.id.cardSection4, R.id.rvSection4, section4) // placeholder
        setupSection(R.id.cardSection5, R.id.rvSection5, section5) // placeholder
    }

    private fun applyStrokeColor(root: View) {
        val night = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val strokeColorRes =
            if (night == Configuration.UI_MODE_NIGHT_YES) R.color.white else R.color.black
        val strokeColor = ContextCompat.getColor(requireContext(), strokeColorRes)
        listOf(
            R.id.cardSection1, R.id.cardSection2, R.id.cardSection3,
            R.id.cardSection4, R.id.cardSection5
        ).forEach { id ->
            root.findViewById<MaterialCardView>(id).setStrokeColor(strokeColor)
        }
    }

    private fun setupSection(headerId: Int, recyclerId: Int, data: List<Exercise>) {
        val headerCard = requireView().findViewById<MaterialCardView>(headerId)
        val recyclerView = requireView().findViewById<RecyclerView>(recyclerId).apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = ExerciseAdapter(
                items = data,
                userIsPT = userIsPT,
                onCardClick = { ex -> openDetail(ex) },
                onConfirmClick = { ex -> saveExercise(ex) }
            )
        }
        headerCard.setOnClickListener {
            recyclerView.visibility =
                if (recyclerView.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }
    }

    /*──────────────── RecyclerView Adapter ───────────────────────────*/
    private inner class ExerciseAdapter(
        private val items: List<Exercise>,
        private val userIsPT: Boolean,
        private val onCardClick: (Exercise) -> Unit,
        private val onConfirmClick: (Exercise) -> Unit
    ) : RecyclerView.Adapter<ExerciseAdapter.VH>() {

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            private val card: MaterialCardView = view.findViewById(R.id.cardExercise)
            private val titleTv: TextView = view.findViewById(R.id.textViewTitleTop)
            private val btnSets: MaterialButton = view.findViewById(R.id.toggleSets)
            private val btnReps: MaterialButton = view.findViewById(R.id.toggleReps)
            private val counterSets: TextView = view.findViewById(R.id.counterSets)
            private val counterReps: TextView = view.findViewById(R.id.counterReps)
            private val btnMinus: FloatingActionButton = view.findViewById(R.id.buttonMinus)
            private val btnPlus: FloatingActionButton = view.findViewById(R.id.buttonPlus)
            private val btnConfirm: MaterialButton = view.findViewById(R.id.buttonConfirm)
            private val green = ContextCompat.getColor(view.context, R.color.green)
            private val black = ContextCompat.getColor(view.context, R.color.black)

            init {
                card.setOnClickListener { items.getOrNull(adapterPosition)?.let(onCardClick) }
                btnConfirm.setOnClickListener {
                    items.getOrNull(adapterPosition)?.let(onConfirmClick)
                }
                btnSets.setOnClickListener {
                    items[adapterPosition].isSetsMode = true; notifyItemChanged(adapterPosition)
                }
                btnReps.setOnClickListener {
                    items[adapterPosition].isSetsMode = false; notifyItemChanged(adapterPosition)
                }
                btnPlus.setOnClickListener {
                    val ex =
                        items[adapterPosition]; if (ex.isSetsMode) ex.setsCount++ else ex.repsCount++; notifyItemChanged(
                    adapterPosition
                )
                }
                btnMinus.setOnClickListener {
                    val ex =
                        items[adapterPosition]; if (ex.isSetsMode && ex.setsCount > 0) ex.setsCount-- else if (!ex.isSetsMode && ex.repsCount > 0) ex.repsCount--; notifyItemChanged(
                    adapterPosition
                )
                }
            }

            private fun highlight(ex: Exercise) {
                counterSets.visibility = if (ex.isSetsMode) View.VISIBLE else View.GONE
                counterReps.visibility = if (ex.isSetsMode) View.GONE else View.VISIBLE
                btnSets.isChecked = ex.isSetsMode
                btnReps.isChecked = !ex.isSetsMode
                btnSets.backgroundTintList =
                    ColorStateList.valueOf(if (ex.isSetsMode) green else black)
                btnReps.backgroundTintList =
                    ColorStateList.valueOf(if (ex.isSetsMode) black else green)
            }

            fun bind(ex: Exercise) {
                titleTv.text = ex.title
                counterSets.text = ex.setsCount.toString()
                counterReps.text = ex.repsCount.toString()
                highlight(ex)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.cards_exercise, parent, false))

        override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])
        override fun getItemCount(): Int = items.size
    }

    /*──────────────── Dialog dettaglio ───────────────────────────────*/
    private fun openDetail(ex: Exercise) {
        ExerciseDetailFragment.newInstance(
            ex.title, ex.videoUrl, ex.description, ex.subtitle2, ex.description2,
            ex.detailImage1Res, ex.detailImage2Res, ex.descriptionImage, ex.descrizioneTotale
        ).show((requireActivity() as FragmentActivity).supportFragmentManager, "exercise_detail")
    }

    /*──────────────── Salvataggio Firestore ─────────────────────────*/
    private fun saveExercise(ex: Exercise) {
        if (ex.setsCount <= 0 || ex.repsCount <= 0) {
            Toast.makeText(requireContext(), "Serie o ripetizioni non valide", Toast.LENGTH_SHORT)
                .show()
            return
        }
        val data = hashMapOf(
            "nomeEsercizio" to ex.title,
            "numeroSerie" to ex.setsCount,
            "numeroRipetizioni" to ex.repsCount
        )
        db.collection("scheda_creata_autonomamente")
            .document(ex.muscoloPrincipale.lowercase()) // petto
            .collection("esercizi")                     // esercizi
            .document(ex.title)                         // "PANCA PIANA"
            .set(data)
            .addOnSuccessListener {
                Toast.makeText(
                    requireContext(),
                    "Salvato sotto ${ex.muscoloPrincipale}", Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener {
                Toast.makeText(
                    requireContext(),
                    "Errore nel salvataggio", Toast.LENGTH_LONG
                ).show()
            }
    }
}
