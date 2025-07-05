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
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore

class StretchingFragment : Fragment(R.layout.fragment_stretching) {

    data class Stretch(
        val category: String = "stretching",
        val type: String,
        val imageRes: Int,
        val title: String,
        val description: String,
        var setsCount: Int = 0,
        var repsCount: Int = 0,
        var isSetsMode: Boolean = true
    )

    private val db = FirebaseFirestore.getInstance()
    private val selectedUser: String? by lazy { arguments?.getString("selectedUser") }
    private val selectedDate: String? by lazy { arguments?.getString("selectedDate") }

    private val neckList     = mutableListOf(
        Stretch(type="neck_side",    imageRes=R.drawable.stretch_pols,    title="Side Neck Stretch",    description="Inclina la testa verso una spalla, mantieni 20s."),
        Stretch(type="neck_forward", imageRes=R.drawable.stretch_pols,    title="Forward Neck Stretch", description="Porta il mento verso il petto, mantieni 20s.")
    )
    private val shoulderList = mutableListOf(
        Stretch(type="shoulder_cross", imageRes=R.drawable.stretch_schoulders, title="Cross-Body Shoulder", description="Porta un braccio al petto, mantieni 20s."),
        Stretch(type="shoulder_tricep",imageRes=R.drawable.stretch_arms,       title="Tricep Stretch",      description="Piega il gomito dietro la testa, mantieni 20s.")
    )
    private val backList     = mutableListOf(
        Stretch(type="cat_cow",    imageRes=R.drawable.stretch_back, title="Cat-Cow",     description="Alterna gobba e inarcata, 8 ripetizioni."),
        Stretch(type="child_pose", imageRes=R.drawable.stretch_back, title="Child’s Pose",description="Seduto sui talloni, braccia in avanti, 30s.")
    )
    private val legsList     = mutableListOf(
        Stretch(type="hamstring", imageRes=R.drawable.stretch_legs, title="Hamstring Stretch", description="Gamba distesa, busto in avanti, 20s."),
        Stretch(type="quad",      imageRes=R.drawable.stretch_legs, title="Quad Stretch",      description="Tira un piede al gluteo, mantieni 20s.")
    )
    private val armsList     = mutableListOf(
        Stretch(type="wrist",      imageRes=R.drawable.stretch_arms, title="Wrist Stretch",      description="Tira indietro le dita, 20s."),
        Stretch(type="bicep_wall", imageRes=R.drawable.stretch_arms, title="Bicep Wall Stretch", description="Mano al muro, mantieni 20s.")
    )

    private lateinit var neckAdapter: StretchAdapter
    private lateinit var shoulderAdapter: StretchAdapter
    private lateinit var backAdapter: StretchAdapter
    private lateinit var legsAdapter: StretchAdapter
    private lateinit var armsAdapter: StretchAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        neckAdapter     = setupSection(view, R.id.cardStretchNeck,      R.id.rvStretchNeck,      neckList)
        shoulderAdapter = setupSection(view, R.id.cardStretchShoulders, R.id.rvStretchShoulders, shoulderList)
        backAdapter     = setupSection(view, R.id.cardStretchBack,      R.id.rvStretchBack,      backList)
        legsAdapter     = setupSection(view, R.id.cardStretchLegs,      R.id.rvStretchLegs,      legsList)
        armsAdapter     = setupSection(view, R.id.cardStretchArms,      R.id.rvStretchArms,      armsList)

        loadSavedStretches()
    }

    private fun setupSection(
        root: View,
        headerId: Int,
        recyclerId: Int,
        data: MutableList<Stretch>
    ): StretchAdapter {
        val header = root.findViewById<MaterialCardView>(headerId)
        val rv     = root.findViewById<RecyclerView>(recyclerId)
        val adapter = StretchAdapter(data, ::openDetail, ::saveStretch)

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

    private inner class StretchAdapter(
        private val items: MutableList<Stretch>,
        private val onItemClick: (Stretch) -> Unit,
        private val onConfirmClick: (Stretch) -> Unit
    ) : RecyclerView.Adapter<StretchAdapter.VH>() {

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
                    if (selectedDate.isNullOrBlank()) onItemClick(items[pos])
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

            fun bind(s: Stretch) {
                titleTv.text        = s.title
                counterSets.text    = s.setsCount.toString()
                counterReps.text    = s.repsCount.toString()
                btnSets.isChecked   = s.isSetsMode
                btnReps.isChecked   = !s.isSetsMode
                btnSets.backgroundTintList = ColorStateList.valueOf(if (s.isSetsMode) green else black)
                btnReps.backgroundTintList = ColorStateList.valueOf(if (!s.isSetsMode) green else black)

                val isTracking = !selectedDate.isNullOrBlank()
                listOf(btnSets, btnReps, btnPlus, btnMinus, btnConfirm).forEach {
                    it.visibility = if (isTracking) View.VISIBLE else View.GONE
                }
                counterSets.visibility = if (isTracking && s.isSetsMode) View.VISIBLE else View.GONE
                counterReps.visibility = if (isTracking && !s.isSetsMode) View.VISIBLE else View.GONE
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.cards_exercise, parent, false))

        override fun onBindViewHolder(holder: VH, position: Int) =
            holder.bind(items[position])

        override fun getItemCount(): Int = items.size
    }

    private fun openDetail(s: Stretch) {
        ExerciseDetailFragment.newInstance(
            s.title, "", s.description,
            "", "", s.imageRes, s.imageRes, s.imageRes, s.description
        ).show((requireActivity() as FragmentActivity).supportFragmentManager, "stretch_detail")
    }

    // Decides PT vs user path
    private fun getStretchRef(): CollectionReference {
        val date = selectedDate ?: throw IllegalStateException("Data mancante")
        return if (selectedUser != null) {
            db.collection("schede_del_pt")
                .document(selectedUser!!)
                .collection(date)
                .document("stretching")
                .collection("esercizi")
        } else {
            val user = requireActivity()
                .getSharedPreferences("user_data", Context.MODE_PRIVATE)
                .getString("saved_display_name", null)
                ?: throw IllegalStateException("Utente mancante")
            db.collection("schede_giornaliere")
                .document(user)
                .collection(date)
                .document("stretching")
                .collection("esercizi")
        }
    }

    private fun saveStretch(s: Stretch) {
        val date = selectedDate ?: run {
            Toast.makeText(requireContext(), "Seleziona una data", Toast.LENGTH_SHORT).show()
            return
        }
        if (s.setsCount == 0 || s.repsCount == 0) {
            Toast.makeText(requireContext(), "Imposta almeno 1 serie e 1 ripetizione", Toast.LENGTH_SHORT).show()
            return
        }
        getStretchRef().document(s.type)
            .set(mapOf(
                "category"            to s.category,
                "nomeEsercizio"       to s.title,
                "numeroSerie"         to s.setsCount,
                "numeroRipetizioni"   to s.repsCount,
                "type"                to s.type
            ))
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Salvato ${s.title}", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Errore salvataggio", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadSavedStretches() {
        getStretchRef().get()
            .addOnSuccessListener { snap ->
                snap.documents.forEach { doc ->
                    val type = doc.id
                    val sets = doc.getLong("numeroSerie")?.toInt() ?: 0
                    val reps = doc.getLong("numeroRipetizioni")?.toInt() ?: 0
                    neckList.find     { it.type == type }?.apply { setsCount=sets; repsCount=reps }
                    shoulderList.find { it.type == type }?.apply { setsCount=sets; repsCount=reps }
                    backList.find     { it.type == type }?.apply { setsCount=sets; repsCount=reps }
                    legsList.find     { it.type == type }?.apply { setsCount=sets; repsCount=reps }
                    armsList.find     { it.type == type }?.apply { setsCount=sets; repsCount=reps }
                }
                neckAdapter.notifyDataSetChanged()
                shoulderAdapter.notifyDataSetChanged()
                backAdapter.notifyDataSetChanged()
                legsAdapter.notifyDataSetChanged()
                armsAdapter.notifyDataSetChanged()
            }
    }
}
