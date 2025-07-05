package com.example.progetto_tosa.ui.workout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.progetto_tosa.R
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.button.MaterialButton

class StretchingFragment : Fragment(R.layout.fragment_stretching) {

    data class Stretch(
        val type: String,
        val imageRes: Int,
        val title: String,
        val description: String
    )

    private val neckStretches = listOf(
        Stretch("neck_side",    R.drawable.stretch_pols,    "Side Neck Stretch",   "Inclina la testa verso una spalla, mantieni 20s."),
        Stretch("neck_forward", R.drawable.stretch_pols,    "Forward Neck Stretch", "Porta il mento verso il petto, mantieni 20s.")
    )
    private val shoulderStretches = listOf(
        Stretch("shoulder_cross",   R.drawable.stretch_schoulders, "Cross-Body Shoulder", "Porta un braccio al petto, tieni 20s."),
        Stretch("shoulder_tricep",   R.drawable.stretch_arms,       "Tricep Stretch",      "Piega il gomito dietro la testa, mantieni 20s.")
    )
    private val backStretches = listOf(
        Stretch("cat_cow",    R.drawable.stretch_back,  "Cat-Cow",    "Alterna schiena a gobba e inarcata per 8 ripetizioni."),
        Stretch("child_pose", R.drawable.stretch_back,  "Child’s Pose","Seduto sui talloni, braccia in avanti, mantieni 30s.")
    )
    private val legStretches = listOf(
        Stretch("hamstring", R.drawable.stretch_legs, "Hamstring Stretch", "Gamba distesa, piega il busto in avanti, 20s ciascuna."),
        Stretch("quad",      R.drawable.stretch_legs, "Quad Stretch",      "Tira un piede al gluteo, mantieni 20s.")
    )
    private val armStretches = listOf(
        Stretch("wrist",      R.drawable.stretch_arms, "Wrist Stretch",      "Mano a palmo, tira indietro le dita, 20s."),
        Stretch("bicep_wall", R.drawable.stretch_arms, "Bicep Wall Stretch", "Mano su muro ruotata, mantieni 20s.")
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSection(R.id.cardStretchNeck,      R.id.rvStretchNeck,      neckStretches)
        setupSection(R.id.cardStretchShoulders, R.id.rvStretchShoulders, shoulderStretches)
        setupSection(R.id.cardStretchBack,      R.id.rvStretchBack,      backStretches)
        setupSection(R.id.cardStretchLegs,      R.id.rvStretchLegs,      legStretches)
        setupSection(R.id.cardStretchArms,      R.id.rvStretchArms,      armStretches)
    }

    private fun setupSection(headerId: Int, recyclerId: Int, data: List<Stretch>) {
        val headerCard   = requireView().findViewById<MaterialCardView>(headerId)
        val recyclerView = requireView().findViewById<RecyclerView>(recyclerId).apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter       = StretchAdapter(data) { openDetail(it) }
            visibility    = View.GONE
        }

        headerCard.strokeColor = ContextCompat.getColor(requireContext(), R.color.black)
        headerCard.setOnClickListener {
            recyclerView.visibility =
                if (recyclerView.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }
    }

    private inner class StretchAdapter(
        private val items: List<Stretch>,
        private val onClick: (Stretch) -> Unit
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

            init {
                view.setOnClickListener {
                    adapterPosition.takeIf { it != RecyclerView.NO_POSITION }
                        ?.let { onClick(items[it]) }
                }
            }

            fun bind(s: Stretch) {
                titleTv.text = s.title
                // Nascondi i controlli non necessari
                listOf(btnSets, btnReps, counterSets, counterReps, btnPlus, btnMinus, btnConfirm)
                    .forEach { it.visibility = View.GONE }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.cards_exercise, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size
    }

    private fun openDetail(s: Stretch) {
        ExerciseDetailFragment.newInstance(
            s.title,
            "",              // niente video
            s.description,
            "", "",          // sottotitoli secondari vuoti
            s.imageRes,
            s.imageRes,
            s.imageRes,
            s.description
        ).show((requireActivity() as FragmentActivity).supportFragmentManager,
            "stretch_detail")
    }
}
