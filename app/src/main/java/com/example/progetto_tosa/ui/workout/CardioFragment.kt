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

    // Esempio di lista cardio (puoi personalizzarla)
    private val cardioExercises = listOf(
        Exercise(
            type = "treadmill",
            imageRes = R.drawable.cardio,
            descriptionImage = R.drawable.cardio,
            title = "TREADMILL",
            videoUrl = "https://youtu.be/…",
            description = "Corsa sul tapis roulant per resistenza aerobica.",
            subtitle2 = "BENEFICI",
            description2 = "- Migliora capacità cardiovascolare\n- Brucia calorie",
            detailImage1Res = R.drawable.cardio,
            detailImage2Res = R.drawable.cardio,
            descrizioneTotale = "15–30 minuti a intensità moderata"
        ),
        Exercise(
            type = "stationary_bike",
            imageRes = R.drawable.cardio,
            descriptionImage = R.drawable.cardio,
            title = "CYCLING (BIKE)",
            videoUrl = "https://youtu.be/…",
            description = "Pedalata su cyclette per allenamento cardio.",
            subtitle2 = "BENEFICI",
            description2 = "- Stimola gambe e glutei\n- Ottimo per cuore",
            detailImage1Res = R.drawable.cardio,
            detailImage2Res = R.drawable.cardio,
            descrizioneTotale = "20–40 minuti a resistenza variabile"
        )
        // … aggiungi altri esercizi cardio …
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUI(view)
    }

    private fun initUI(root: View) {
        applyStrokeColor(root)
        setupSection(R.id.cardioCard, R.id.rvCardio, cardioExercises)
        loadSavedExercises()
    }

    private fun applyStrokeColor(root: View) {
        val night = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val colorRes = if (night == Configuration.UI_MODE_NIGHT_YES) R.color.white else R.color.black
        root.findViewById<MaterialCardView>(R.id.cardioCard).strokeColor =
            ContextCompat.getColor(requireContext(), colorRes)
    }

    private fun setupSection(headerId: Int, recyclerId: Int, data: List<Exercise>) {
        val headerCard = requireView().findViewById<MaterialCardView>(headerId)
        val recyclerView = requireView().findViewById<RecyclerView>(recyclerId).apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = ExerciseAdapter(data, ::openDetail, ::saveExercise)
        }

        val night = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val strokeColor = ContextCompat.getColor(
            requireContext(),
            if (night == Configuration.UI_MODE_NIGHT_YES) R.color.white else R.color.black
        )
        headerCard.strokeColor = strokeColor

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
            "type" to ex.type
        )
        val successMessage = "Esercizio \"${ex.title}\" aggiunto con successo"
        val prefs = requireActivity().getSharedPreferences("user_data", Context.MODE_PRIVATE)
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
                Toast.makeText(requireContext(), "Impossibile identificare l'utente", Toast.LENGTH_SHORT).show()
                return
            }
        }

        collectionRef.document(ex.title)
            .set(data)
            .addOnSuccessListener { Toast.makeText(requireContext(), successMessage, Toast.LENGTH_SHORT).show() }
            .addOnFailureListener { Toast.makeText(requireContext(), "Errore durante il salvataggio", Toast.LENGTH_SHORT).show() }
    }

    private fun loadSavedExercises() {
        if (selectedDate.isNullOrBlank()) return
        val types = cardioExercises.map { it.type }.distinct()
        val loaded = mutableListOf<Pair<String, List<Map<String, Any>>>>()

        types.forEach { t ->
            db.collection("schede_giornaliere")
                .document(selectedDate!!)
                .collection("cardio")
                .document(t)
                .collection("esercizi")
                .get()
                .addOnSuccessListener { snap ->
                    val list = snap.documents.mapNotNull { doc ->
                        mapOf(
                            "title" to doc.id,
                            "sets" to (doc.getLong("numeroSerie")?.toInt() ?: 0),
                            "reps" to (doc.getLong("numeroRipetizioni")?.toInt() ?: 0)
                        )
                    }
                    loaded.add(t to list)
                    if (loaded.size == types.size) {
                        loaded.forEach { (_, listE) ->
                            listE.forEach { entry ->
                                val title = entry["title"] as? String ?: return@forEach
                                val sets = entry["sets"] as? Int ?: return@forEach
                                val reps = entry["reps"] as? Int ?: return@forEach
                                cardioExercises.find { it.title == title }?.apply {
                                    setsCount = sets
                                    repsCount = reps
                                }
                            }
                        }
                        setupSection(R.id.cardioCard, R.id.rvCardio, cardioExercises)
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Errore caricamento $t", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
