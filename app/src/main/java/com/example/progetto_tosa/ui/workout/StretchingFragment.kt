// StretchingFragment.kt
package com.example.progetto_tosa.ui.workout

import android.app.AlertDialog
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.NumberPicker
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
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

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

        // sections
        neckAdapter      = setupSection(binding.cardStretchNeck,      binding.rvStretchNeck,      vm.neck.value!!)
        shouldersAdapter = setupSection(binding.cardStretchShoulders, binding.rvStretchShoulders, vm.shoulders.value!!)
        backAdapter      = setupSection(binding.cardStretchBack,      binding.rvStretchBack,      vm.back.value!!)
        legsAdapter      = setupSection(binding.cardStretchLegs,      binding.rvStretchLegs,      vm.legs.value!!)
        armsAdapter      = setupSection(binding.cardStretchArms,      binding.rvStretchArms,      vm.arms.value!!)

        // observers
        vm.neck.observe(viewLifecycleOwner)      { updateAdapter(neckAdapter, it) }
        vm.shoulders.observe(viewLifecycleOwner) { updateAdapter(shouldersAdapter, it) }
        vm.back.observe(viewLifecycleOwner)      { updateAdapter(backAdapter, it) }
        vm.legs.observe(viewLifecycleOwner)      { updateAdapter(legsAdapter, it) }
        vm.arms.observe(viewLifecycleOwner)      { updateAdapter(armsAdapter, it) }

        // 1) load stretching catalog
        vm.loadAnagraficaStretchingFromFirestore()

        // 2) load saved sets (same path as save)
        if (!selectedDate.isNullOrBlank()) {
            val prefs = requireActivity().getSharedPreferences("user_data", Context.MODE_PRIVATE)
            val currentUser = prefs.getString("saved_display_name", null)
            vm.loadSavedStretches(selectedDate, selectedUser, currentUser) { /* no-op */ }
        }
        // 3) optional seed
        // seedAllStretchesToFirestore()
    }

    private fun setupSection(
        header: MaterialCardView,
        rv: RecyclerView,
        data: List<StretchingViewModel.Stretch>
    ): StretchAdapter {
        // subtle border in dark mode
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
        if (s.setsCount == 0) {
            Toast.makeText(requireContext(), "Imposta almeno 1 serie", Toast.LENGTH_SHORT).show()
            return
        }

        val data = hashMapOf(
            "nomeEsercizio" to s.title,
            "numeroSerie"   to s.setsCount,
            "type"          to s.type,
            "createdAt"     to FieldValue.serverTimestamp()
        ).apply {
            s.durata?.takeIf { it.isNotBlank() }?.let { put("durata", it) }
        }
        // Nota: la durata non viene salvata (binding-only). Aggiungila qui se/quando la vorrai persistere.

        val prefs = requireActivity().getSharedPreferences("user_data", Context.MODE_PRIVATE)
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

        collectionRef.document(s.title)
            .set(data)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Salvato ${s.title}", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Errore durante il salvataggio", Toast.LENGTH_SHORT).show()
            }
    }

    // helper: resId -> nome drawable (seed)
    private fun resIdToName(resId: Int): String =
        requireContext().resources.getResourceEntryName(resId)

    /** SEED: scrive tutti gli stretching hardcoded in esercizi/stretching/voci/{docId} */
    private fun seedAllStretchesToFirestore() {
        val batch = db.batch()
        val all = listOf(
            vm.neck.value.orEmpty(),
            vm.shoulders.value.orEmpty(),
            vm.back.value.orEmpty(),
            vm.legs.value.orEmpty(),
            vm.arms.value.orEmpty()
        ).flatten()

        all.forEach { s ->
            val docId = s.title.lowercase()
                .replace(" ", "_")
                .replace("[^a-z0-9_]+".toRegex(), "")
            val docRef = db.collection("esercizi")
                .document("stretching")
                .collection("voci")
                .document(docId)

            val data = hashMapOf(
                "category"             to "stretching",
                "type"                 to s.type,
                "title"                to s.title,
                "videoUrl"             to s.videoUrl,
                "description"          to s.description,
                "subtitle2"            to s.subtitle2,
                "description2"         to s.description2,
                "descriptionImageName" to resIdToName(s.descriptionImage),
                "detailImage1Name"     to resIdToName(s.detailImage1Res),
                "detailImage2Name"     to resIdToName(s.detailImage2Res),
                "descrizioneTotale"    to s.descrizioneTotale,
                "createdAt"            to FieldValue.serverTimestamp()
            )
            batch.set(docRef, data)
        }

        batch.commit()
            .addOnSuccessListener { Toast.makeText(requireContext(), "Seed stretching ✅", Toast.LENGTH_SHORT).show() }
            .addOnFailureListener { e -> Toast.makeText(requireContext(), "Errore seed: ${e.message}", Toast.LENGTH_LONG).show() }
    }

    // === Time picker (riutilizzabile): opzionale init per precompilare i valori ===
    private fun showTimePicker(
        initial: Pair<Int, Int>? = null,
        onTimeSelected: (String) -> Unit
    ) {
        val pickerMin = NumberPicker(requireContext()).apply {
            minValue = 0; maxValue = 59; value = initial?.first ?: 0
        }
        val pickerSec = NumberPicker(requireContext()).apply {
            minValue = 0; maxValue = 59; value = initial?.second ?: 0
        }

        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            val pad = (16 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
            addView(pickerMin)
            addView(pickerSec)
        }

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.imposta_tempo)
            .setView(layout)
            .setPositiveButton(R.string.ok) { _, _ ->
                val total = pickerMin.value * 60 + pickerSec.value
                val mm = total / 60
                val ss = total % 60
                val formatted = String.format(Locale.getDefault(), "%02d:%02d", mm, ss)
                onTimeSelected(formatted)
            }
            .setNegativeButton(R.string.annulla, null)
            .show()
    }

    // ---- Adapter ----
    private inner class StretchAdapter(
        val items: MutableList<StretchingViewModel.Stretch>,
        private val onCardClick: (StretchingViewModel.Stretch) -> Unit,
        private val onConfirmClick: (StretchingViewModel.Stretch) -> Unit
    ) : RecyclerView.Adapter<StretchAdapter.VH>() {

        // title->"MM:SS" picked locally (kept across binds)
        private val durationByTitle = mutableMapOf<String, String>()

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            private val titleTv   = view.findViewById<TextView>(R.id.textViewTitleTop)
            private val inputSets = view.findViewById<android.widget.EditText>(R.id.inputSets)
            private val inputDuration = view.findViewById<TextView>(R.id.cardDurationInput)
            private val btnConfirm= view.findViewById<MaterialButton>(R.id.buttonConfirm)

            init {
                btnConfirm.setOnClickListener {
                    val pos = adapterPosition.takeIf { it != RecyclerView.NO_POSITION } ?: return@setOnClickListener
                    val ex = items[pos]
                    ex.setsCount = inputSets.text.toString().toIntOrNull() ?: 0
                    // If you want to save duration later, read it here:
                    // val pickedDuration = durationByTitle[ex.title]
                    onConfirmClick(ex)
                }
                itemView.setOnClickListener {
                    adapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.let { onCardClick(items[it]) }
                }
            }

            fun bind(ex: StretchingViewModel.Stretch) {
                titleTv.text = ex.title
                inputSets.setText(ex.setsCount.takeIf { it > 0 }?.toString() ?: "")

                // === duration binding (no immediate save) ===
                // === duration binding ===
                val current = ex.durata ?: durationByTitle[ex.title]
                inputDuration.text = current ?: "Aggiungi"

                inputDuration.isFocusable = false
                inputDuration.isClickable = true

                inputDuration.setOnClickListener {
                    val init = parseDuration(current) // Pair(min,sec) or null
                    this@StretchingFragment.showTimePicker(init) { formatted ->
                        inputDuration.text = formatted
                        durationByTitle[ex.title] = formatted
                        ex.durata = formatted
                    }
                }
                // === end duration binding ===

                val isTracking = !selectedDate.isNullOrBlank()
                listOf(btnConfirm, inputSets).forEach {
                    it.visibility = if (isTracking) VISIBLE else GONE
                }
                itemView.setOnClickListener { if (!isTracking) onCardClick(ex) }
            }

            private fun parseDuration(value: String?): Pair<Int, Int>? {
                if (value.isNullOrBlank()) return null
                val parts = value.split(":")
                if (parts.size != 2) return null
                val mm = parts[0].toIntOrNull() ?: return null
                val ss = parts[1].toIntOrNull() ?: return null
                return mm.coerceIn(0, 59) to ss.coerceIn(0, 59)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.cards_exercise_duration, parent, false))

        override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])
        override fun getItemCount(): Int = items.size
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}