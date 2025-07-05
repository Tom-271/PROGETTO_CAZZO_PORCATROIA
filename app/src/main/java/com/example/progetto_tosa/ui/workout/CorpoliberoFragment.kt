package com.example.progetto_tosa.ui.workout

import android.content.Context
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
import com.google.firebase.firestore.FirebaseFirestore

class CorpoliberoFragment : Fragment(R.layout.fragment_corpolibero) {

    data class Exercise(
        val category: String = "corpo_libero",
        val type: String,
        val imageRes: Int,
        val title: String,
        val videoUrl: String,
        val description: String,
        val benefit: String,
        var setsCount: Int = 0,
        var repsCount: Int = 0,
        var isSetsMode: Boolean = true
    )

    private val db = FirebaseFirestore.getInstance()
    private val selectedUser: String? by lazy { arguments?.getString("selectedUser") }
    private val selectedDate: String? by lazy { arguments?.getString("selectedDate") }

    private val absExercises = listOf(
        Exercise(
            type = "plank",
            imageRes = R.drawable.libero_addome,
            title = "PLANK",
            videoUrl = "https://youtu.be/…",
            description = "Mantieni il corpo dritto in appoggio sugli avambracci.",
            benefit = "Attiva core e stabilità"
        ),
        Exercise(
            type = "crunch",
            imageRes = R.drawable.libero_addome,
            title = "CRUNCH",
            videoUrl = "https://youtu.be/…",
            description = "Sollevamento busto da terra contraendo gli addominali.",
            benefit = "Rafforza retto addominale"
        )
    )
    private val chestExercises = listOf(
        Exercise(
            type = "push_up",
            imageRes = R.drawable.libero_petto,
            title = "PUSH-UP",
            videoUrl = "https://youtu.be/…",
            description = "Flessione sulle braccia mantenendo il corpo lineare.",
            benefit = "Rafforza petto e tricipiti"
        ),
        Exercise(
            type = "diamond_pushup",
            imageRes = R.drawable.libero_petto,
            title = "DIAMOND PUSH-UP",
            videoUrl = "https://youtu.be/…",
            description = "Mani ravvicinate a diamante per il focus sui tricipiti.",
            benefit = "Isola tricipiti e parte interna del petto"
        )
    )
    private val backExercises = listOf(
        Exercise(
            type = "superman",
            imageRes = R.drawable.libero_schiena,
            title = "SUPERMAN",
            videoUrl = "https://youtu.be/…",
            description = "Sdraiato a pancia in giù, solleva braccia e gambe.",
            benefit = "Rinforza schiena e glutei"
        ),
        Exercise(
            type = "reverse_snowangel",
            imageRes = R.drawable.libero_schiena,
            title = "REVERSE SNOW ANGEL",
            videoUrl = "https://youtu.be/…",
            description = "Sdraiato, allarga e chiudi le braccia dietro la schiena.",
            benefit = "Attiva muscoli dorsali"
        )
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUI(view)
        loadSavedExercises()
    }

    private fun initUI(root: View) {
        setupSection(R.id.cardAbs,       R.id.rvAbs,       absExercises)
        setupSection(R.id.cardSection1,  R.id.rvSection1,  chestExercises)
        setupSection(R.id.cardSection3,  R.id.rvSection3,  backExercises)
    }

    private fun setupSection(headerId: Int, recyclerId: Int, data: List<Exercise>) {
        val headerCard = requireView().findViewById<MaterialCardView>(headerId)
        val recycler  = requireView().findViewById<RecyclerView>(recyclerId).apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter       = ExerciseAdapter(data, ::openDetail, ::saveExercise)
            visibility    = View.GONE
        }
        headerCard.strokeColor = ContextCompat.getColor(requireContext(), R.color.black)
        headerCard.setOnClickListener {
            recycler.visibility = if (recycler.visibility == View.VISIBLE) View.GONE else View.VISIBLE
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
                btnPlus.setOnClickListener { adjustCount(+1) }
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
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.cards_exercise, parent, false)
            return VH(v)
        }
        override fun onBindViewHolder(holder: VH, position: Int) =
            holder.bind(items[position])
        override fun getItemCount(): Int = items.size
    }

    private fun openDetail(ex: Exercise) {
        ExerciseDetailFragment.newInstance(
            ex.title,
            ex.videoUrl,
            ex.description,
            ex.benefit,
            "", // placeholder
            ex.imageRes,
            ex.imageRes,
            ex.imageRes,
            ""  // placeholder
        ).show((requireActivity() as FragmentActivity).supportFragmentManager, "exercise_detail")
    }

    private fun saveExercise(ex: Exercise) {
        if (selectedDate.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Seleziona prima una data", Toast.LENGTH_SHORT).show()
            return
        }
        if (ex.setsCount <= 0 || ex.repsCount <= 0) {
            Toast.makeText(requireContext(), "Imposta almeno 1 serie e 1 ripetizione", Toast.LENGTH_SHORT).show()
            return
        }
        val prefs = requireActivity()
            .getSharedPreferences("user_data", Context.MODE_PRIVATE)
        val user = prefs.getString("saved_display_name", null) ?: selectedUser
        if (user.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Utente non identificato", Toast.LENGTH_SHORT).show()
            return
        }
        val data = hashMapOf(
            "category"            to ex.category,
            "nomeEsercizio"       to ex.title,
            "numeroSerie"         to ex.setsCount,
            "numeroRipetizioni"   to ex.repsCount,
            "type"                to ex.type
        )
        db.collection("schede_giornaliere")
            .document(user)
            .collection(selectedDate!!)
            .document("corpo-libero")
            .collection("esercizi")
            .document(ex.type)
            .set(data)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Salvato ${ex.title}", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Errore salvataggio", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadSavedExercises() {
        val date = selectedDate ?: return
        val prefs = requireActivity().getSharedPreferences("user_data", Context.MODE_PRIVATE)
        val user = prefs.getString("saved_display_name", null) ?: selectedUser
        if (user.isNullOrBlank()) return

        // Abs
        absExercises.forEach { ex ->
            db.collection("schede_giornaliere")
                .document(user)
                .collection(date)
                .document("libero")
                .collection("esercizi")
                .document(ex.type)
                .get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        ex.setsCount = doc.getLong("numeroSerie")?.toInt() ?: 0
                        ex.repsCount = doc.getLong("numeroRipetizioni")?.toInt() ?: 0
                    }
                    setupSection(R.id.cardAbs, R.id.rvAbs, absExercises)
                }
        }

        // Chest
        chestExercises.forEach { ex ->
            db.collection("schede_giornaliere")
                .document(user)
                .collection(date)
                .document("libero")
                .collection("esercizi")
                .document(ex.type)
                .get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        ex.setsCount = doc.getLong("numeroSerie")?.toInt() ?: 0
                        ex.repsCount = doc.getLong("numeroRipetizioni")?.toInt() ?: 0
                    }
                    setupSection(R.id.cardSection1, R.id.rvSection1, chestExercises)
                }
        }

        // Back
        backExercises.forEach { ex ->
            db.collection("schede_giornaliere")
                .document(user)
                .collection(date)
                .document("libero")
                .collection("esercizi")
                .document(ex.type)
                .get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        ex.setsCount = doc.getLong("numeroSerie")?.toInt() ?: 0
                        ex.repsCount = doc.getLong("numeroRipetizioni")?.toInt() ?: 0
                    }
                    setupSection(R.id.cardSection3, R.id.rvSection3, backExercises)
                }
        }
    }
}
