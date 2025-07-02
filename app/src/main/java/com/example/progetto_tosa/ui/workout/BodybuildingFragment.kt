package com.example.progetto_tosa.ui.workout

import android.content.res.ColorStateList
import android.content.res.Configuration
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

    private val db = FirebaseFirestore.getInstance()

    private val selectedUser: String? by lazy {
        arguments?.getString("selectedUser")
    }

    private val selectedDate: String? by lazy {
        arguments?.getString("selectedDate")
    }

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
        )
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUI(view)
    }

    private fun initUI(root: View) {
        applyStrokeColor(root)
        setupSection(R.id.cardSection1, R.id.rvSection1, section1)
    }

    private fun applyStrokeColor(root: View) {
        val night = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val colorRes = if (night == Configuration.UI_MODE_NIGHT_YES) R.color.white else R.color.black
        val strokeColor = ContextCompat.getColor(requireContext(), colorRes)
        root.findViewById<MaterialCardView>(R.id.cardSection1).strokeColor = strokeColor
    }

    private fun setupSection(headerId: Int, recyclerId: Int, data: List<Exercise>) {
        val headerCard = requireView().findViewById<MaterialCardView>(headerId)
        val recyclerView = requireView().findViewById<RecyclerView>(recyclerId).apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = ExerciseAdapter(data, ::openDetail, ::saveExercise)
        }
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

                val isTrackingMode = selectedDate != null

                // Tracking UI visibilità
                btnSets.visibility = if (isTrackingMode) View.VISIBLE else View.GONE
                btnReps.visibility = if (isTrackingMode) View.VISIBLE else View.GONE
                btnPlus.visibility = if (isTrackingMode) View.VISIBLE else View.GONE
                btnMinus.visibility = if (isTrackingMode) View.VISIBLE else View.GONE
                btnConfirm.visibility = if (isTrackingMode) View.VISIBLE else View.GONE
                counterSets.visibility = if (isTrackingMode && ex.isSetsMode) View.VISIBLE else View.GONE
                counterReps.visibility = if (isTrackingMode && !ex.isSetsMode) View.VISIBLE else View.GONE

                // Informative UI visibilità
                itemView.setOnClickListener {
                    if (!isTrackingMode) {
                        onCardClick(ex)
                    }
                }
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
        if (selectedDate == null) {
            Toast.makeText(requireContext(), "Seleziona prima una data per aggiungere l'esercizio", Toast.LENGTH_SHORT).show()
            return
        }
        if (ex.setsCount <= 0 || ex.repsCount <= 0) {
            Toast.makeText(requireContext(), "Serie o ripetizioni non valide", Toast.LENGTH_SHORT).show()
            return
        }

        val data = mapOf(
            "category" to ex.category,
            "nomeEsercizio" to ex.title,
            "numeroSerie" to ex.setsCount,
            "numeroRipetizioni" to ex.repsCount
        )

        if (selectedUser != null) {
            db.collection("schede_del_pt")
                .document(selectedUser!!)
                .collection("esercizi")
                .document(ex.title)
                .set(data)
        } else {
            db.collection("schede_giornaliere")
                .document(selectedDate!!)
                .collection(ex.category)
                .document(ex.muscoloPrincipale)
                .collection("esercizi")
                .document(ex.title)
                .set(data)
        }
    }
}
