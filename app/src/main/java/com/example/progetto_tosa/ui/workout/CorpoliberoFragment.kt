package com.example.progetto_tosa.ui.workout

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.view.LayoutInflater
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
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore

class CorpoliberoFragment : Fragment(R.layout.fragment_corpolibero) {

    data class Exercise(
        val category: String = "corpo-libero",
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
    // If this fragment was invoked for a PT, selectedUser holds PT's uid
    private val selectedUser: String? by lazy { arguments?.getString("selectedUser") }
    private val selectedDate: String?  by lazy { arguments?.getString("selectedDate")  }

    private val absExercises   = mutableListOf(
        Exercise(type="plank",  imageRes=R.drawable.libero_addome, title="PLANK",  videoUrl="", description="Mantieni il corpo dritto…", benefit="Attiva core"),
        Exercise(type="crunch", imageRes=R.drawable.libero_addome, title="CRUNCH", videoUrl="", description="Sollevamento busto…", benefit="Rafforza addominali")
    )
    private val chestExercises = mutableListOf(
        Exercise(type="push_up",   imageRes=R.drawable.libero_petto, title="PUSH-UP",         videoUrl="", description="Flessione bodyweight…",   benefit="Petto & tricipiti"),
        Exercise(type="diamond",   imageRes=R.drawable.libero_petto, title="DIAMOND PUSH-UP", videoUrl="", description="Mani a diamante…",        benefit="Focalizza tricipiti")
    )
    private val backExercises  = mutableListOf(
        Exercise(type="superman",  imageRes=R.drawable.libero_schiena, title="SUPERMAN",         videoUrl="", description="Solleva braccia e gambe…", benefit="Schiena & glutei"),
        Exercise(type="reverse",   imageRes=R.drawable.libero_schiena, title="REVERSE SNOW ANGEL",videoUrl="", description="Apri/chiudi le braccia…", benefit="Dorsali")
    )

    private lateinit var absAdapter: ExerciseAdapter
    private lateinit var chestAdapter: ExerciseAdapter
    private lateinit var backAdapter: ExerciseAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        absAdapter   = setupSection(view, R.id.cardAbs,      R.id.rvAbs,      absExercises)
        chestAdapter = setupSection(view, R.id.cardSection1, R.id.rvSection1, chestExercises)
        backAdapter  = setupSection(view, R.id.cardSection3, R.id.rvSection3, backExercises)
        loadSavedExercises()
    }

    private fun setupSection(
        root: View,
        headerId: Int,
        recyclerId: Int,
        data: MutableList<Exercise>
    ): ExerciseAdapter {
        val header = root.findViewById<MaterialCardView>(headerId)
        val rv     = root.findViewById<RecyclerView>(recyclerId)
        val adapter = ExerciseAdapter(data, ::openDetail, ::saveExercise)

        rv.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            this.adapter = adapter
            visibility = View.GONE
        }
        header.strokeColor = ContextCompat.getColor(requireContext(), R.color.black)
        header.setOnClickListener {
            rv.visibility = if (rv.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }
        return adapter
    }

    private inner class ExerciseAdapter(
        private val items: MutableList<Exercise>,
        private val onCardClick: (Exercise) -> Unit,
        private val onConfirmClick: (Exercise) -> Unit
    ) : RecyclerView.Adapter<ExerciseAdapter.VH>() {

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            private val titleTv     = view.findViewById<TextView>(R.id.textViewTitleTop)
            private val btnSets     = view.findViewById<MaterialButton>(R.id.toggleSets)
            private val btnReps     = view.findViewById<MaterialButton>(R.id.toggleReps)
            private val counterSets = view.findViewById<TextView>(R.id.counterSets)
            private val counterReps = view.findViewById<TextView>(R.id.counterReps)
            private val btnPlus     = view.findViewById<FloatingActionButton>(R.id.buttonPlus)
            private val btnMinus    = view.findViewById<FloatingActionButton>(R.id.buttonMinus)
            private val btnConfirm  = view.findViewById<MaterialButton>(R.id.buttonConfirm)
            private val green       = ContextCompat.getColor(view.context, R.color.green)
            private val black       = ContextCompat.getColor(view.context, R.color.black)

            init {
                btnSets.setOnClickListener  { toggleMode(true)  }
                btnReps.setOnClickListener  { toggleMode(false) }
                btnPlus.setOnClickListener  { adjustCount(+1)   }
                btnMinus.setOnClickListener { adjustCount(-1)   }
                btnConfirm.setOnClickListener {
                    val pos = adapterPosition.takeIf { it != RecyclerView.NO_POSITION } ?: return@setOnClickListener
                    onConfirmClick(items[pos])
                }
                itemView.setOnClickListener {
                    val pos = adapterPosition.takeIf { it != RecyclerView.NO_POSITION } ?: return@setOnClickListener
                    if (selectedDate.isNullOrBlank()) onCardClick(items[pos])
                }
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
                else               ex.repsCount = maxOf(0, ex.repsCount + delta)
                notifyItemChanged(pos)
            }

            fun bind(ex: Exercise) {
                titleTv.text = ex.title
                counterSets.text = ex.setsCount.toString()
                counterReps.text = ex.repsCount.toString()
                btnSets.isChecked = ex.isSetsMode
                btnReps.isChecked = !ex.isSetsMode
                btnSets.backgroundTintList = ColorStateList.valueOf(if (ex.isSetsMode) green else black)
                btnReps.backgroundTintList = ColorStateList.valueOf(if (!ex.isSetsMode) green else black)

                val isTracking = !selectedDate.isNullOrBlank()
                listOf(btnSets, btnReps, btnPlus, btnMinus, btnConfirm).forEach {
                    it.visibility = if (isTracking) View.VISIBLE else View.GONE
                }
                counterSets.visibility = if (isTracking && ex.isSetsMode) View.VISIBLE else View.GONE
                counterReps.visibility = if (isTracking && !ex.isSetsMode) View.VISIBLE else View.GONE
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.cards_exercise, parent, false))

        override fun onBindViewHolder(holder: VH, position: Int) =
            holder.bind(items[position])

        override fun getItemCount(): Int = items.size
    }

    private fun openDetail(ex: Exercise) {
        ExerciseDetailFragment.newInstance(
            ex.title, ex.videoUrl, ex.description, ex.benefit,
            "", ex.imageRes, ex.imageRes, ex.imageRes, ""
        ).show((requireActivity() as FragmentActivity).supportFragmentManager, "exercise_detail")
    }

    /** Returns the correct collectionRef for saving/loading */
    private fun getExercisesRef(): CollectionReference {
        val date = selectedDate ?: throw IllegalStateException("Data mancante")
        return if (selectedUser != null) {
            // PT schedule
            db.collection("schede_del_pt")
                .document(selectedUser!!)
                .collection(date)
                .document("corpo-libero")
                .collection("esercizi")
        } else {
            // User's own schedule
            val user = requireActivity()
                .getSharedPreferences("user_data", Context.MODE_PRIVATE)
                .getString("saved_display_name", null)
                ?: throw IllegalStateException("Utente mancante")
            db.collection("schede_giornaliere")
                .document(user)
                .collection(date)
                .document("corpo-libero")
                .collection("esercizi")
        }
    }

    private fun saveExercise(ex: Exercise) {
        if (selectedDate.isNullOrBlank() || ex.setsCount == 0 || ex.repsCount == 0) {
            Toast.makeText(requireContext(),
                "Seleziona data e imposta serie/ripetizioni", Toast.LENGTH_SHORT).show()
            return
        }
        getExercisesRef().document(ex.type)
            .set(mapOf(
                "category"          to ex.category,
                "nomeEsercizio"     to ex.title,
                "numeroSerie"       to ex.setsCount,
                "numeroRipetizioni" to ex.repsCount,
                "type"              to ex.type
            ))
            .addOnSuccessListener {
                Toast.makeText(requireContext(),
                    "Esercizio \"${ex.title}\" salvato", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(),
                    "Errore durante il salvataggio", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadSavedExercises() {
        getExercisesRef().get()
            .addOnSuccessListener { snap ->
                // update counts in all lists
                snap.documents.forEach { doc ->
                    val type = doc.id
                    val sets = doc.getLong("numeroSerie")?.toInt() ?: 0
                    val reps = doc.getLong("numeroRipetizioni")?.toInt() ?: 0
                    absExercises.find  { it.type == type }?.apply { setsCount=sets; repsCount=reps }
                    chestExercises.find{ it.type == type }?.apply { setsCount=sets; repsCount=reps }
                    backExercises.find { it.type == type }?.apply { setsCount=sets; repsCount=reps }
                }
                absAdapter.notifyDataSetChanged()
                chestAdapter.notifyDataSetChanged()
                backAdapter.notifyDataSetChanged()
            }
    }
}
