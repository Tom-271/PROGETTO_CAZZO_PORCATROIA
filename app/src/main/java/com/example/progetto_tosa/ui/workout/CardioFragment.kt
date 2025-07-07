package com.example.progetto_tosa.ui.workout

import android.content.Context
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

class CardioFragment : Fragment(R.layout.fragment_cardio) {

    data class Exercise(
        val category: String = "cardio",
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
    private val selectedUser: String? by lazy { arguments?.getString("selectedUser") }
    private val selectedDate: String? by lazy { arguments?.getString("selectedDate") }

    // 4 esercizi, poi li distribuiremo 2 e 2
    private val cardioExercises = listOf(
        Exercise(
            type = "Corsa",
            imageRes = R.drawable.treadmill1,
            descriptionImage = R.drawable.corsa,
            title = "TAPIS ROULANT",
            videoUrl = "https://www.youtube.com/watch?v=DwzWPvS9DG0",
            description = "Corsa sul tapis roulant per resistenza aerobica.",
            subtitle2 = "BENEFICI",
            description2 = "- Migliora capacità cardiovascolare\n- Brucia calorie",
            detailImage1Res = R.drawable.treadmill1,
            detailImage2Res = R.drawable.treadmill2,
            descrizioneTotale = "15–30 minuti a intensità moderata"
        ),
        Exercise(
            type = "Jumping_jacks",
            imageRes = R.drawable.junping,
            descriptionImage = R.drawable.junping2,
            title = "JUMPING-JACKS",
            videoUrl = "https://www.youtube.com/watch?v=K_98p0I1nD8",
            description = "Salti in accoppiata ad apertura e chiusura di gambe e braccia",
            subtitle2 = "BENEFICI",
            description2 = "- Rafforza articolazioni\n- Stimola mente e corpo",
            detailImage1Res = R.drawable.junping2,
            detailImage2Res = R.drawable.junping3,
            descrizioneTotale = "1 minuto ciascuna rep"
        ),
        Exercise(
            type = "Salto_della_corda",
            imageRes = R.drawable.salto_della_corda,
            descriptionImage = R.drawable.salto_corda,
            title = "JUMP ROPE",
            videoUrl = "https://www.youtube.com/watch?v=jUBirAI-nQU",
            description = "Salto con la corda per coordinazione e cardio veloce.",
            subtitle2 = "BENEFICI",
            description2 = "- Migliora agilità\n- Alto dispendio calorico",
            detailImage1Res = R.drawable.salto_corda,
            detailImage2Res = R.drawable.duecorda,
            descrizioneTotale = "5–10 minuti di round da 1 minuto"
        ),
        Exercise(
            type = "high_knees",
            imageRes = R.drawable.knees2,
            descriptionImage = R.drawable.salto_corda,
            title = "HIGH KNEES",
            videoUrl = "https://www.youtube.com/watch?app=desktop&v=hzLR_WrtWKQ",
            description = "Corsa sul posto portando le ginocchia alte.",
            subtitle2 = "BENEFICI",
            description2 = "- Aumenta frequenza cardiaca\n- Attiva core e gambe",
            detailImage1Res = R.drawable.salto_corda,
            detailImage2Res = R.drawable.salto_corda,
            descrizioneTotale = "30–60 secondi x 3 set"
        )
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUI(view)
        loadSavedExercises()
    }

    private fun initUI(root: View) {
        // prima sezione (Corsa): primi 2 esercizi
        setupSection(
            headerId   = R.id.cardioCard1,
            recyclerId = R.id.rvCardio1,
            data       = cardioExercises.subList(0, 2)
        )
        // seconda sezione (Corda): ultimi 2 esercizi
        setupSection(
            headerId   = R.id.cardioCard2,
            recyclerId = R.id.rvCardio2,
            data       = cardioExercises.subList(2, 4)
        )
    }

    private fun setupSection(headerId: Int, recyclerId: Int, data: List<Exercise>) {
        val headerCard = requireView().findViewById<MaterialCardView>(headerId)
        val recyclerView = requireView().findViewById<RecyclerView>(recyclerId).apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter       = ExerciseAdapter(data, ::openDetail, ::saveExercise)
            visibility    = View.GONE
        }

        headerCard.strokeColor = ContextCompat.getColor(
            requireContext(), R.color.black
        )

        headerCard.setOnClickListener {
            recyclerView.visibility =
                if (recyclerView.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }
    }

    private inner class ExerciseAdapter(
        private val items: List<Exercise>,
        private val onCardClick: (Exercise) -> Unit,
        private val onConfirmClick: (Exercise) -> Unit
    ) : RecyclerView.Adapter<ExerciseAdapter.VH>() {

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
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
                btnConfirm.setOnClickListener {
                    adapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.let {
                        onConfirmClick(items[it])
                    }
                }
                btnSets.setOnClickListener { toggleMode(true) }
                btnReps.setOnClickListener { toggleMode(false) }
                btnPlus.setOnClickListener { adjustCount(1) }
                btnMinus.setOnClickListener { adjustCount(-1) }
            }

            private fun toggleMode(isSets: Boolean) {
                adapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.let { pos ->
                    items[pos].isSetsMode = isSets
                    notifyItemChanged(pos)
                }
            }

            private fun adjustCount(delta: Int) {
                adapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.let { pos ->
                    val ex = items[pos]
                    if (ex.isSetsMode) ex.setsCount = maxOf(0, ex.setsCount + delta)
                    else               ex.repsCount = maxOf(0, ex.repsCount + delta)
                    notifyItemChanged(pos)
                }
            }

            fun bind(ex: Exercise) {
                titleTv.text              = ex.title
                counterSets.text          = ex.setsCount.toString()
                counterReps.text          = ex.repsCount.toString()
                btnSets.isChecked         = ex.isSetsMode
                btnReps.isChecked         = !ex.isSetsMode
                btnSets.backgroundTintList = ColorStateList.valueOf(if (ex.isSetsMode) green else black)
                btnReps.backgroundTintList = ColorStateList.valueOf(if (ex.isSetsMode) black else green)

                val isTracking = !selectedDate.isNullOrBlank()
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

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.cards_exercise, parent, false)
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size
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
            Toast.makeText(requireContext(),
                "Seleziona prima una data per aggiungere l'esercizio",
                Toast.LENGTH_SHORT).show()
            return
        }
        if (ex.setsCount <= 0 || ex.repsCount <= 0) {
            Toast.makeText(requireContext(),
                "Imposta almeno 1 serie e 1 ripetizione",
                Toast.LENGTH_SHORT).show()
            return
        }
        val data = hashMapOf(
            "category" to ex.category,
            "nomeEsercizio" to ex.title,
            "numeroSerie" to ex.setsCount,
            "numeroRipetizioni" to ex.repsCount,
            "type" to ex.type
        )
        val prefs = requireActivity()
            .getSharedPreferences("user_data", Context.MODE_PRIVATE)
        val currentUserName = prefs.getString("saved_display_name", null)

        val collectionRef = when {
            !selectedUser.isNullOrBlank() ->
                db.collection("schede_del_pt")
                    .document(selectedUser!!)
                    .collection(selectedDate!!)
                    .document("cardio")
                    .collection("esercizi")
            !currentUserName.isNullOrBlank() ->
                db.collection("schede_giornaliere")
                    .document(currentUserName!!)
                    .collection(selectedDate!!)
                    .document("cardio")
                    .collection("esercizi")
            else -> {
                Toast.makeText(requireContext(),
                    "Impossibile identificare l'utente",
                    Toast.LENGTH_SHORT).show()
                return
            }
        }

        collectionRef.document(ex.title)
            .set(data)
            .addOnSuccessListener {
                Toast.makeText(requireContext(),
                    "Esercizio \"${ex.title}\" aggiunto con successo",
                    Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(),
                    "Errore durante il salvataggio",
                    Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadSavedExercises() {
        if (selectedDate.isNullOrBlank()) return

        // Ricarica dati per tutti e 4 gli esercizi
        cardioExercises.forEachIndexed { idx, exercise ->
            val headerId   = if (idx < 2) R.id.cardioCard1 else R.id.cardioCard2
            val recyclerId = if (idx < 2) R.id.rvCardio1   else R.id.rvCardio2

            db.collection("schede_giornaliere")
                .document(selectedDate!!)
                .collection("cardio")
                .document(exercise.type)
                .collection("esercizi")
                .get()
                .addOnSuccessListener { snap ->
                    snap.documents.forEach { doc ->
                        val title = doc.id
                        val sets  = doc.getLong("numeroSerie")?.toInt() ?: 0
                        val reps  = doc.getLong("numeroRipetizioni")?.toInt() ?: 0
                        if (exercise.title == title) {
                            exercise.setsCount = sets
                            exercise.repsCount = reps
                        }
                    }
                    // Reinizializza la sezione coinvolta
                    setupSection(headerId, recyclerId,
                        cardioExercises.subList(
                            if (idx < 2) 0 else 2,
                            if (idx < 2) 2 else 4
                        )
                    )
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(),
                        "Errore caricamento esercizi",
                        Toast.LENGTH_SHORT).show()
                }
        }
    }
}
