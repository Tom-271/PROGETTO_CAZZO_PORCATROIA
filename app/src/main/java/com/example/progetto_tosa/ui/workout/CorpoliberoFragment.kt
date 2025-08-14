// CorpoliberoFragment.kt
package com.example.progetto_tosa.ui.workout

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.widget.EditText
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

        binding.viewModel = vm
        binding.lifecycleOwner = viewLifecycleOwner

        // sections
        absAdapter   = setupSection(binding.cardAbs,      binding.rvAbs,      vm.abs.value!!)
        chestAdapter = setupSection(binding.cardSection1, binding.rvSection1, vm.chest.value!!)
        backAdapter  = setupSection(binding.cardSection3, binding.rvSection3, vm.back.value!!)

        // observe
        vm.abs.observe(viewLifecycleOwner)   { updateAdapter(absAdapter, it) }
        vm.chest.observe(viewLifecycleOwner) { updateAdapter(chestAdapter, it) }
        vm.back.observe(viewLifecycleOwner)  { updateAdapter(backAdapter, it) }

        //seedCorpoLiberoToFirestore()

        vm.loadAnagraficaCorpoLiberoFromFirestore()

        // load saved reps/sets (user-centric path)
        if (!selectedDate.isNullOrBlank()) {
            vm.loadSavedExercises(selectedDate, selectedUser)
        }
    }

    private fun setupSection(
        header: MaterialCardView,
        rv: RecyclerView,
        data: List<CorpoliberoViewModel.Exercise>
    ): ExerciseAdapter {
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
            Toast.makeText(requireContext(), "Seleziona data e imposta serie e ripetizioni", Toast.LENGTH_SHORT).show()
            return
        }
        vm.saveExercise(ex, selectedDate, selectedUser)
        Toast.makeText(requireContext(), "Esercizio \"${ex.title}\" salvato", Toast.LENGTH_SHORT).show()
    }

    private inner class ExerciseAdapter(
        val items: MutableList<CorpoliberoViewModel.Exercise>,
        private val onCardClick: (CorpoliberoViewModel.Exercise) -> Unit,
        private val onConfirmClick: (CorpoliberoViewModel.Exercise) -> Unit
    ) : RecyclerView.Adapter<ExerciseAdapter.VH>() {

        inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val titleTv   = itemView.findViewById<TextView>(R.id.textViewTitleTop)
            private val inputSets = itemView.findViewById<EditText>(R.id.inputSets)
            private val inputReps = itemView.findViewById<EditText>(R.id.inputReps)
            private val btnConfirm= itemView.findViewById<MaterialButton>(R.id.buttonConfirm)

            init {
                btnConfirm.setOnClickListener {
                    val pos = adapterPosition.takeIf { it != RecyclerView.NO_POSITION } ?: return@setOnClickListener
                    val ex = items[pos]
                    ex.setsCount = inputSets.text.toString().toIntOrNull() ?: 0
                    ex.repsCount = inputReps.text.toString().toIntOrNull() ?: 0
                    onConfirmClick(ex)
                }
                itemView.setOnClickListener {
                    adapterPosition
                        .takeIf { it != RecyclerView.NO_POSITION }
                        ?.let { if (selectedDate.isNullOrBlank()) onCardClick(items[it]) }
                }
            }

            fun bind(ex: CorpoliberoViewModel.Exercise) {
                titleTv.text = ex.title
                inputSets.setText(ex.setsCount.takeIf { it > 0 }?.toString() ?: "")
                inputReps.setText(ex.repsCount.takeIf { it > 0 }?.toString() ?: "")

                val isTracking = !selectedDate.isNullOrBlank()
                // FIX: include inputReps (before c’era btnConfirm ripetuto)
                listOf(btnConfirm, inputSets, inputReps).forEach {
                    it.visibility = if (isTracking) View.VISIBLE else GONE
                }

                itemView.setOnClickListener { if (!isTracking) onCardClick(ex) }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.cards_exercise, parent, false)
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])
        override fun getItemCount(): Int = items.size
    }

    //--- drawable helpers (se non li hai già in questo file) ---
    private fun resIdToName(resId: Int): String =
        requireContext().resources.getResourceEntryName(resId)

    private fun nameToResId(name: String): Int =
        requireContext().resources.getIdentifier(name, "drawable", requireContext().packageName)

    //--- SEED: scrive tutti gli esercizi di corpo-libero nell'anagrafica ---
// Struttura: esercizi / corpo-libero (doc) / voci (subcoll) / {slug_esercizio} (doc)
    private fun seedCorpoLiberoToFirestore() {
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val batch = db.batch()

        // unisci le tre liste correnti dal ViewModel
        val all = listOf(
            vm.abs.value.orEmpty(),
            vm.chest.value.orEmpty(),
            vm.back.value.orEmpty()
        ).flatten()

        all.forEach { ex ->
            val docId = ex.title.lowercase()
                .replace(" ", "_")
                .replace("[^a-z0-9_]+".toRegex(), "")

            val docRef = db.collection("esercizi")
                .document(ex.category)        // "corpo-libero"
                .collection("voci")
                .document(docId)

            // deduci il gruppo in base alla lista di provenienza (fallback su type)
            val gruppo = when {
                vm.abs.value.orEmpty().any { it.title == ex.title }   -> "abs"
                vm.chest.value.orEmpty().any { it.title == ex.title } -> "chest"
                vm.back.value.orEmpty().any { it.title == ex.title }  -> "back"
                else -> "abs"
            }

            val data = hashMapOf(
                "category"            to ex.category,          // "corpo-libero"
                "type"                to ex.type,              // es. "plank"
                "title"               to ex.title,
                "videoUrl"            to ex.videoUrl,
                "description"         to ex.description,
                "benefit"             to ex.benefit,
                "detailImage1Name"    to resIdToName(ex.detailImage1Res),
                "detailImage2Name"    to resIdToName(ex.detailImage2Res),
                "imageResName"        to resIdToName(ex.imageRes),
                "descrizioneTotale"   to ex.descrizioneTotale,
                "gruppo"              to gruppo,               // "abs" | "chest" | "back"
                "createdAt"           to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
            batch.set(docRef, data)
        }

        batch.commit()
            .addOnSuccessListener {
                android.widget.Toast.makeText(requireContext(), "Seed corpo-libero completato ✅", android.widget.Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                android.widget.Toast.makeText(requireContext(), "Errore seed: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
