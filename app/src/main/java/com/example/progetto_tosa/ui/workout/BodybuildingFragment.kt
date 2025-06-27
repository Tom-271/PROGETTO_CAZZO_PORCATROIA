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
import java.text.SimpleDateFormat
import java.util.*

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

    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()
    private var userIsPT = false

    // La data selezionata viene passata come argomento; qui non facciamo più fallback a "oggi"
    private val selectedDate: String by lazy {
        arguments?.getString("selectedDate")
            ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
    // Immagine di placeholder
    private val sharedImage = R.drawable.pancadescrizione

    // Dati di esempio
    private val section1 = listOf(
        Exercise(
            category = "bodybuilding",
            muscoloPrincipale = "petto",
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
            category = "bodybuilding",
            muscoloPrincipale = "petto",
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
    private val section2 = emptyList<Exercise>()
    private val section3 = emptyList<Exercise>()
    private val section4 = emptyList<Exercise>()
    private val section5 = emptyList<Exercise>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Verifica PT
        auth.currentUser?.uid?.let { uid ->
            db.collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    userIsPT = doc.getBoolean("isPersonalTrainer") == true
                    initUI(view)
                }
                .addOnFailureListener {
                    initUI(view)
                }
        } ?: initUI(view)
    }

    private fun initUI(root: View) {
        applyStrokeColor(root)
        setupSection(R.id.cardSection1, R.id.rvSection1, section1)
        setupSection(R.id.cardSection2, R.id.rvSection2, section2)
        setupSection(R.id.cardSection3, R.id.rvSection3, section3)
        setupSection(R.id.cardSection4, R.id.rvSection4, section4)
        setupSection(R.id.cardSection5, R.id.rvSection5, section5)
    }

    private fun applyStrokeColor(root: View) {
        val night = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val strokeColorRes = if (night == Configuration.UI_MODE_NIGHT_YES) R.color.white else R.color.black
        val strokeColor = ContextCompat.getColor(requireContext(), strokeColorRes)
        arrayOf(
            R.id.cardSection1,
            R.id.cardSection2,
            R.id.cardSection3,
            R.id.cardSection4,
            R.id.cardSection5
        ).forEach { id ->
            root.findViewById<MaterialCardView>(id).strokeColor = strokeColor
        }
    }

    private fun setupSection(headerId: Int, recyclerId: Int, data: List<Exercise>) {
        val headerCard   = requireView().findViewById<MaterialCardView>(headerId)
        val recyclerView = requireView().findViewById<RecyclerView>(recyclerId).apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = ExerciseAdapter(data, userIsPT, ::openDetail, ::saveExercise)
        }
        headerCard.setOnClickListener {
            recyclerView.visibility = if (recyclerView.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }
    }

    private inner class ExerciseAdapter(
        private val items: List<Exercise>,
        private val userIsPT: Boolean,
        private val onCardClick: (Exercise) -> Unit,
        private val onConfirmClick: (Exercise) -> Unit
    ) : RecyclerView.Adapter<ExerciseAdapter.VH>() {

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            private val card        = view.findViewById<MaterialCardView>(R.id.cardExercise)
            private val titleTv     = view.findViewById<TextView>(R.id.textViewTitleTop)
            private val btnSets     = view.findViewById<MaterialButton>(R.id.toggleSets)
            private val btnReps     = view.findViewById<MaterialButton>(R.id.toggleReps)
            private val counterSets = view.findViewById<TextView>(R.id.counterSets)
            private val counterReps = view.findViewById<TextView>(R.id.counterReps)
            private val btnMinus    = view.findViewById<FloatingActionButton>(R.id.buttonMinus)
            private val btnPlus     = view.findViewById<FloatingActionButton>(R.id.buttonPlus)
            private val btnConfirm  = view.findViewById<MaterialButton>(R.id.buttonConfirm)
            private val green       = ContextCompat.getColor(view.context, R.color.green)
            private val black       = ContextCompat.getColor(view.context, R.color.black)

            init {
                card.setOnClickListener { onCardClick(items[adapterPosition]) }
                btnConfirm.setOnClickListener { onConfirmClick(items[adapterPosition]) }
                btnSets.setOnClickListener {
                    items[adapterPosition].isSetsMode = true
                    notifyItemChanged(adapterPosition)
                }
                btnReps.setOnClickListener {
                    items[adapterPosition].isSetsMode = false
                    notifyItemChanged(adapterPosition)
                }
                btnPlus.setOnClickListener {
                    val ex = items[adapterPosition]
                    if (ex.isSetsMode) ex.setsCount++ else ex.repsCount++
                    notifyItemChanged(adapterPosition)
                }
                btnMinus.setOnClickListener {
                    val ex = items[adapterPosition]
                    if (ex.isSetsMode && ex.setsCount > 0) ex.setsCount--
                    else if (!ex.isSetsMode && ex.repsCount > 0) ex.repsCount--
                    notifyItemChanged(adapterPosition)
                }
            }

            fun bind(ex: Exercise) {
                titleTv.text = ex.title
                counterSets.text = ex.setsCount.toString()
                counterReps.text = ex.repsCount.toString()
                btnSets.isChecked = ex.isSetsMode
                btnReps.isChecked = !ex.isSetsMode
                btnSets.backgroundTintList = ColorStateList.valueOf(if (ex.isSetsMode) green else black)
                btnReps.backgroundTintList = ColorStateList.valueOf(if (ex.isSetsMode) black else green)
                counterSets.visibility = if (ex.isSetsMode) View.VISIBLE else View.GONE
                counterReps.visibility = if (ex.isSetsMode) View.GONE else View.VISIBLE
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.cards_exercise, parent, false))

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
        if (ex.setsCount <= 0 || ex.repsCount <= 0) {
            Toast.makeText(requireContext(), "Serie o ripetizioni non valide", Toast.LENGTH_SHORT).show()
            return
        }

        val data = hashMapOf(
            "category"          to ex.category,
            "nomeEsercizio"     to ex.title,
            "numeroSerie"       to ex.setsCount,
            "numeroRipetizioni" to ex.repsCount
        )

        db.collection("schede_giornaliere")
            .document(selectedDate)
            .collection(ex.category)
            .document(ex.muscoloPrincipale)
            .collection("esercizi")
            .document(ex.title)
            .set(data)
            .addOnSuccessListener {
                Toast.makeText(
                    requireContext(),
                    "Salvato per $selectedDate → ${ex.category} → ${ex.muscoloPrincipale}",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Errore nel salvataggio", Toast.LENGTH_LONG).show()
            }
    }
}
