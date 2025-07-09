package com.example.progetto_tosa.ui.workout

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.progetto_tosa.R
import com.example.progetto_tosa.databinding.FragmentCorpoliberoBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class CorpoliberoFragment : Fragment(R.layout.fragment_corpolibero) {

    private var _binding: FragmentCorpoliberoBinding? = null
    private val binding get() = _binding!!
    private val vm: CorpoliberoViewModel by viewModels()

    private val selectedUser: String? by lazy { arguments?.getString("selectedUser") }
    private val selectedDate: String? by lazy { arguments?.getString("selectedDate") }

    private lateinit var absAdapter: ExerciseAdapter
    private lateinit var chestAdapter: ExerciseAdapter
    private lateinit var backAdapter: ExerciseAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentCorpoliberoBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)

        // Collego il ViewModel al layout
        binding.viewModel = vm
        binding.lifecycleOwner = viewLifecycleOwner

        // Imposto le tre sezioni
        absAdapter   = setupSection(binding.cardAbs,       binding.rvAbs,       vm.abs.value!!)
        chestAdapter = setupSection(binding.cardSection1,  binding.rvSection1,  vm.chest.value!!)
        backAdapter  = setupSection(binding.cardSection3,  binding.rvSection3,  vm.back.value!!)

        // Osservo le LiveData e aggiorno gli adapter
        vm.abs.observe(viewLifecycleOwner)   { updateAdapter(absAdapter, it) }
        vm.chest.observe(viewLifecycleOwner) { updateAdapter(chestAdapter, it) }
        vm.back.observe(viewLifecycleOwner)  { updateAdapter(backAdapter, it) }

        // Carico eventuali esercizi salvati
        if (!selectedDate.isNullOrBlank()) {
            vm.loadSavedExercises(selectedDate, selectedUser)
        }
    }

    private fun setupSection(
        header: MaterialCardView,
        rv: RecyclerView,
        data: List<CorpoliberoViewModel.Exercise>
    ): ExerciseAdapter {
        // stroke dinamico per dark mode
        val night = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val strokeColor = ContextCompat.getColor(
            requireContext(),
            if (night == Configuration.UI_MODE_NIGHT_YES) R.color.black else R.color.black
        )
        header.strokeColor = strokeColor

        val adapter = ExerciseAdapter(data.toMutableList(), ::openDetail, ::onConfirm)
        rv.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            this.adapter = adapter
            visibility = GONE
        }
        header.setOnClickListener {
            rv.visibility = if (rv.visibility == View.VISIBLE) GONE else View.VISIBLE
        }
        return adapter
    }

    private fun updateAdapter(adapter: ExerciseAdapter, list: List<CorpoliberoViewModel.Exercise>) {
        adapter.items.clear()
        adapter.items.addAll(list)
        adapter.notifyDataSetChanged()
    }

    private fun openDetail(ex: CorpoliberoViewModel.Exercise) {
        ExerciseDetailFragment.newInstance(
            ex.title,
            ex.videoUrl,
            ex.description,
            "BENEFICI",
            ex.benefit,
            ex.detailImage1Res,
            ex.detailImage2Res,
            ex.imageRes,
            ex.descrizioneTotale
        ).show(childFragmentManager, "exercise_detail")
    }

    private fun onConfirm(ex: CorpoliberoViewModel.Exercise) {
        if (selectedDate.isNullOrBlank() || ex.setsCount == 0 || ex.repsCount == 0) {
            Toast.makeText(
                requireContext(),
                "Seleziona data e imposta serie e ripetizioni",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        vm.saveExercise(ex, selectedDate, selectedUser)
        Toast.makeText(
            requireContext(),
            "Esercizio \"${ex.title}\" salvato",
            Toast.LENGTH_SHORT
        ).show()
    }

    private inner class ExerciseAdapter(
        val items: MutableList<CorpoliberoViewModel.Exercise>,
        private val onCardClick: (CorpoliberoViewModel.Exercise) -> Unit,
        private val onConfirmClick: (CorpoliberoViewModel.Exercise) -> Unit
    ) : RecyclerView.Adapter<ExerciseAdapter.VH>() {

        inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val titleTv     = itemView.findViewById<TextView>(R.id.textViewTitleTop)
            private val btnSets     = itemView.findViewById<MaterialButton>(R.id.toggleSets)
            private val btnReps     = itemView.findViewById<MaterialButton>(R.id.toggleReps)
            private val counterSets = itemView.findViewById<TextView>(R.id.counterSets)
            private val counterReps = itemView.findViewById<TextView>(R.id.counterReps)
            private val btnPlus     = itemView.findViewById<FloatingActionButton>(R.id.buttonPlus)
            private val btnMinus    = itemView.findViewById<FloatingActionButton>(R.id.buttonMinus)
            private val btnConfirm  = itemView.findViewById<MaterialButton>(R.id.buttonConfirm)
            private val green       = ContextCompat.getColor(itemView.context, R.color.green)
            private val black       = ContextCompat.getColor(itemView.context, R.color.black)

            init {
                btnSets.setOnClickListener  { toggleMode(true)  }
                btnReps.setOnClickListener  { toggleMode(false) }
                btnPlus.setOnClickListener  { adjustCount(+1)   }
                btnMinus.setOnClickListener { adjustCount(-1)   }
                btnConfirm.setOnClickListener {
                    adapterPosition
                        .takeIf { it != RecyclerView.NO_POSITION }
                        ?.let { onConfirmClick(items[it]) }
                }
                itemView.setOnClickListener {
                    adapterPosition
                        .takeIf { it != RecyclerView.NO_POSITION }
                        ?.let { if (selectedDate.isNullOrBlank()) onCardClick(items[it]) }
                }
            }

            private fun toggleMode(isSets: Boolean) {
                adapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.also { pos ->
                    items[pos].isSetsMode = isSets
                    notifyItemChanged(pos)
                }
            }

            private fun adjustCount(delta: Int) {
                adapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.also { pos ->
                    val ex = items[pos]
                    if (ex.isSetsMode) ex.setsCount = maxOf(0, ex.setsCount + delta)
                    else               ex.repsCount = maxOf(0, ex.repsCount + delta)
                    notifyItemChanged(pos)
                }
            }

            fun bind(ex: CorpoliberoViewModel.Exercise) {
                titleTv.text              = ex.title
                counterSets.text          = ex.setsCount.toString()
                counterReps.text          = ex.repsCount.toString()
                btnSets.isChecked         = ex.isSetsMode
                btnReps.isChecked         = !ex.isSetsMode
                btnSets.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    if (ex.isSetsMode) green else black
                )
                btnReps.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    if (!ex.isSetsMode) green else black
                )

                val isTracking = !selectedDate.isNullOrBlank()
                listOf(btnSets, btnReps, btnPlus, btnMinus, btnConfirm).forEach {
                    it.visibility = if (isTracking) View.VISIBLE else GONE
                }
                counterSets.visibility = if (isTracking && ex.isSetsMode) View.VISIBLE else GONE
                counterReps.visibility = if (isTracking && !ex.isSetsMode) View.VISIBLE else GONE
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.cards_exercise, parent, false)
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) =
            holder.bind(items[position])

        override fun getItemCount(): Int = items.size
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
