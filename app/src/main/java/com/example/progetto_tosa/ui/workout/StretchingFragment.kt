package com.example.progetto_tosa.ui.workout

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.progetto_tosa.R
import com.example.progetto_tosa.databinding.FragmentStretchingBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class StretchingFragment : Fragment(R.layout.fragment_stretching) {

    private var _binding: FragmentStretchingBinding? = null
    private val binding get() = _binding!!

    private val vm: StretchingViewModel by viewModels()
    private val db = FirebaseFirestore.getInstance()

    private val selectedDate: String? by lazy { arguments?.getString("selectedDate") }
    private val selectedUser: String? by lazy { arguments?.getString("selectedUser") }

    private lateinit var neckAdapter: StretchAdapter
    private lateinit var shouldersAdapter: StretchAdapter
    private lateinit var backAdapter: StretchAdapter
    private lateinit var legsAdapter: StretchAdapter
    private lateinit var armsAdapter: StretchAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentStretchingBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)

        binding.viewModel = vm
        binding.lifecycleOwner = viewLifecycleOwner

        // creo tutte le sezioni
        neckAdapter      = setupSection(binding.cardStretchNeck,      binding.rvStretchNeck,      vm.neck.value!!)
        shouldersAdapter = setupSection(binding.cardStretchShoulders, binding.rvStretchShoulders, vm.shoulders.value!!)
        backAdapter      = setupSection(binding.cardStretchBack,      binding.rvStretchBack,      vm.back.value!!)
        legsAdapter      = setupSection(binding.cardStretchLegs,      binding.rvStretchLegs,      vm.legs.value!!)
        armsAdapter      = setupSection(binding.cardStretchArms,      binding.rvStretchArms,      vm.arms.value!!)

        // osservo i LiveData per ciascuna sezione
        vm.neck.observe(viewLifecycleOwner)      { updateAdapter(neckAdapter, it) }
        vm.shoulders.observe(viewLifecycleOwner){ updateAdapter(shouldersAdapter, it) }
        vm.back.observe(viewLifecycleOwner)      { updateAdapter(backAdapter, it) }
        vm.legs.observe(viewLifecycleOwner)      { updateAdapter(legsAdapter, it) }
        vm.arms.observe(viewLifecycleOwner)      { updateAdapter(armsAdapter, it) }

        // carico i dati salvati
        if (!selectedDate.isNullOrBlank()) {
            vm.loadSavedStretches(selectedDate) { /* al termine potresti eseguire eventuali animazioni */ }
        }
    }

    private fun setupSection(
        header: MaterialCardView,
        rv: RecyclerView,
        data: List<StretchingViewModel.Stretch>
    ): StretchAdapter {
        // bordo in dark mode
        if ((resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK)
            == Configuration.UI_MODE_NIGHT_YES
        ) {
            val px = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 2f, resources.displayMetrics
            ).toInt()
            header.strokeWidth = px
            header.strokeColor = ContextCompat.getColor(requireContext(), android.R.color.black)
        }

        val adapter = StretchAdapter(data.toMutableList(), ::openDetail, ::saveStretch)
        rv.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            this.adapter = adapter
            visibility = GONE
        }
        header.setOnClickListener {
            rv.visibility = if (rv.visibility == VISIBLE) GONE else VISIBLE
        }
        return adapter
    }

    private fun updateAdapter(adapter: StretchAdapter, list: List<StretchingViewModel.Stretch>) {
        adapter.items.clear()
        adapter.items.addAll(list)
        adapter.notifyDataSetChanged()
    }

    private fun openDetail(s: StretchingViewModel.Stretch) {
        ExerciseDetailFragment.newInstance(
            s.title, s.videoUrl, s.description,
            s.subtitle2, s.description2,
            s.detailImage1Res, s.detailImage2Res,
            s.descriptionImage, s.descrizioneTotale
        ).show(childFragmentManager, "stretch_detail")
    }

    private fun saveStretch(s: StretchingViewModel.Stretch) {
        if (selectedDate.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Seleziona prima una data", Toast.LENGTH_SHORT).show()
            return
        }
        if (s.setsCount == 0 || s.repsCount == 0) {
            Toast.makeText(requireContext(), "Imposta almeno 1 serie e 1 ripetizione", Toast.LENGTH_SHORT).show()
            return
        }

        val data = hashMapOf(
            "nomeEsercizio"     to s.title,
            "numeroSerie"       to s.setsCount,
            "numeroRipetizioni" to s.repsCount,
            "type"              to s.type,
            "createdAt"         to FieldValue.serverTimestamp()
        )
        val prefs = requireActivity()
            .getSharedPreferences("user_data", Context.MODE_PRIVATE)
        val currentUser = prefs.getString("saved_display_name", null)

        val collectionRef = when {
            !selectedUser.isNullOrBlank() -> db
                .collection("schede_del_pt").document(selectedUser!!)
                .collection(selectedDate!!).document("stretching")
                .collection("esercizi")
            !currentUser.isNullOrBlank() -> db
                .collection("schede_giornaliere").document(currentUser!!)
                .collection(selectedDate!!).document("stretching")
                .collection("esercizi")
            else -> {
                Toast.makeText(requireContext(), "Impossibile identificare l'utente", Toast.LENGTH_SHORT).show()
                return
            }
        }

        collectionRef.document(s.type)
            .set(data)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Salvato ${s.title}", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Errore durante il salvataggio", Toast.LENGTH_SHORT).show()
            }
    }

    private inner class StretchAdapter(
        val items: MutableList<StretchingViewModel.Stretch>,
        private val onCardClick: (StretchingViewModel.Stretch) -> Unit,
        private val onConfirmClick: (StretchingViewModel.Stretch) -> Unit
    ) : RecyclerView.Adapter<StretchAdapter.VH>() {

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            private val titleTv     = view.findViewById<TextView>(R.id.textViewTitleTop)
            private val inputSets = view.findViewById<android.widget.EditText>(R.id.inputSets)
            private val inputReps = view.findViewById<android.widget.EditText>(R.id.inputReps)
            private val btnConfirm  = view.findViewById<MaterialButton>(R.id.buttonConfirm)
            private val green       = ContextCompat.getColor(view.context, R.color.green)
            private val black       = ContextCompat.getColor(view.context, R.color.black)

            init {
                btnConfirm.setOnClickListener {
                    val pos = adapterPosition.takeIf { it != RecyclerView.NO_POSITION } ?: return@setOnClickListener
                    val ex = items[pos]

                    // Leggi i valori inseriti nei campi
                    ex.setsCount = inputSets.text.toString().toIntOrNull() ?: 0
                    ex.repsCount = inputReps.text.toString().toIntOrNull() ?: 0
                    onConfirmClick(ex)
                }
                itemView.setOnClickListener {
                    adapterPosition.takeIf { it != RecyclerView.NO_POSITION }
                        ?.let { onCardClick(items[it]) }
                }
            }

            private fun toggleMode(isSets: Boolean) {
                adapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.also { pos ->
                    items[pos].isSetsMode = isSets
                    notifyItemChanged(pos)
                }
            }
            private fun adjustCount(delta: Int) {
                /*adapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.also { pos ->
                    val s = items[pos]
                    if (s.isSetsMode) s.setsCount = (s.setsCount + delta).coerceAtLeast(0)
                    else              s.repsCount = (s.repsCount + delta).coerceAtLeast(0)
                    notifyItemChanged(pos)
                }*/

                val pos = adapterPosition.takeIf { it != RecyclerView.NO_POSITION } ?: return
                val ex = items[pos]
                if (ex.isSetsMode) ex.setsCount = maxOf(0, ex.setsCount + delta)
                else ex.repsCount = maxOf(0, ex.repsCount + delta)
                notifyItemChanged(pos)
            }

            fun bind(ex: StretchingViewModel.Stretch) {
                titleTv.text        = ex.title
                inputSets.setText(ex.setsCount.takeIf { it > 0 }?.toString() ?: "")
                inputReps.setText(ex.repsCount.takeIf { it > 0 }?.toString() ?: "")

                val isTracking = !selectedDate.isNullOrBlank()
                listOf(btnConfirm, inputSets, inputReps).forEach {
                    it.visibility = if (isTracking) VISIBLE else GONE
                }
                itemView.setOnClickListener {
                    if (!isTracking) onCardClick(ex)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.cards_exercise, parent, false))

        override fun onBindViewHolder(holder: VH, position: Int) =
            holder.bind(items[position])

        override fun getItemCount(): Int = items.size
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
