// CardioFragment.kt
package com.example.progetto_tosa.ui.workout

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
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
import com.example.progetto_tosa.databinding.FragmentCardioBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class CardioFragment : Fragment(R.layout.fragment_cardio) {

    private var _binding: FragmentCardioBinding? = null
    private val binding get() = _binding!!

    private val vm: CardioViewModel by viewModels()
    private val db = FirebaseFirestore.getInstance()

    private val selectedUser: String? by lazy { arguments?.getString("selectedUser") }
    private val selectedDate: String? by lazy { arguments?.getString("selectedDate") }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentCardioBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)

        binding.viewModel = vm
        binding.lifecycleOwner = viewLifecycleOwner

        initUI()
        observeData()

        // 1) anagrafica -> UI (carica da esercizi/cardio/voci)
        vm.loadAnagraficaCardioFromFirestore()

        // 2) set/rep salvati
        vm.loadSavedExercises(selectedDate, selectedUser, getCurrentUserName())

        // 3) opzionale: crea/riempi "esercizi/cardio/voci" UNA SOLA VOLTA
        // seedAllExercisesToFirestore()
    }

    private fun getCurrentUserName(): String? {
        val prefs = requireActivity().getSharedPreferences("user_data", Context.MODE_PRIVATE)
        return prefs.getString("saved_display_name", null)
    }

    private fun initUI() {
        listOf(binding.cardioCard1, binding.cardioCard2).forEach { card ->
            val night = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            val colorRes = if (night == Configuration.UI_MODE_NIGHT_YES) R.color.dark_gray else R.color.white
            card.strokeColor = ContextCompat.getColor(requireContext(), colorRes)
            val rv = if (card === binding.cardioCard1) binding.rvCardio1 else binding.rvCardio2
            card.setOnClickListener {
                rv.visibility = if (rv.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            }
        }
    }

    private fun observeData() {
        vm.exercises.observe(viewLifecycleOwner) { list ->
            val half = (list.size + 1) / 2
            setupSection(list.take(half), binding.rvCardio1)
            setupSection(list.drop(half), binding.rvCardio2)
        }
    }

    // --- helper per il seed: resId -> nome drawable ---
    private fun resIdToName(resId: Int): String =
        requireContext().resources.getResourceEntryName(resId)

    // --- SEED (opzionale) ---
    private fun seedAllExercisesToFirestore() {
        val db = FirebaseFirestore.getInstance()
        val batch = db.batch()
        val all = vm.exercises.value.orEmpty()

        all.forEach { ex ->
            val docId = ex.title.lowercase()
                .replace(" ", "_")
                .replace("[^a-z0-9_]+".toRegex(), "")

            val docRef = db.collection("esercizi")
                .document(ex.category)      // "cardio"
                .collection("voci")
                .document(docId)

            val data = hashMapOf(
                "category" to ex.category,
                "type" to ex.type,
                "title" to ex.title,
                "videoUrl" to ex.videoUrl,
                "description" to ex.description,
                "subtitle2" to ex.subtitle2,
                "description2" to ex.description2,
                "descriptionImageName" to resIdToName(ex.descriptionImage),
                "detailImage1Name" to resIdToName(ex.detailImage1Res),
                "detailImage2Name" to resIdToName(ex.detailImage2Res),
                "descrizioneTotale" to ex.descrizioneTotale,
                "createdAt" to FieldValue.serverTimestamp()
            )
            batch.set(docRef, data)
        }

        batch.commit()
            .addOnSuccessListener { Toast.makeText(requireContext(), "Seed completato ✅", Toast.LENGTH_SHORT).show() }
            .addOnFailureListener { e -> Toast.makeText(requireContext(), "Errore seed: ${e.message}", Toast.LENGTH_LONG).show() }
    }

    private fun setupSection(data: List<CardioViewModel.Exercise>, recyclerView: RecyclerView) {
        recyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = ExerciseAdapter(data, ::openDetail, ::saveExercise)
        }
    }

    private fun openDetail(ex: CardioViewModel.Exercise) {
        ExerciseDetailFragment.newInstance(
            ex.title, ex.videoUrl, ex.description,
            ex.subtitle2, ex.description2,
            ex.detailImage1Res, ex.detailImage2Res,
            ex.descriptionImage, ex.descrizioneTotale
        ).show(childFragmentManager, "exercise_detail")
    }

    private fun saveExercise(ex: CardioViewModel.Exercise) {
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
            "type" to ex.type,
            "createdAt" to FieldValue.serverTimestamp()
        )

        val currentUserName = getCurrentUserName()
        val collectionRef = when {
            !selectedUser.isNullOrBlank() ->
                db.collection("schede_del_pt").document(selectedUser!!)
                    .collection(selectedDate!!).document("cardio")
                    .collection("esercizi")
            !currentUserName.isNullOrBlank() ->
                db.collection("schede_giornaliere").document(currentUserName!!)
                    .collection(selectedDate!!).document("cardio")
                    .collection("esercizi")
            else -> {
                Toast.makeText(requireContext(), "Impossibile identificare l'utente", Toast.LENGTH_SHORT).show()
                return
            }
        }

        // DOC ID = TITOLO (coerente col loader nel ViewModel)
        collectionRef.document(ex.title)
            .set(data)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Esercizio \"${ex.title}\" aggiunto con successo", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Errore durante il salvataggio", Toast.LENGTH_SHORT).show()
            }
    }

    private inner class ExerciseAdapter(
        private val items: List<CardioViewModel.Exercise>,
        private val onCardClick: (CardioViewModel.Exercise) -> Unit,
        private val onConfirmClick: (CardioViewModel.Exercise) -> Unit
    ) : RecyclerView.Adapter<ExerciseAdapter.VH>() {

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            private val titleTv = view.findViewById<TextView>(R.id.textViewTitleTop)
            private val inputSets = view.findViewById<EditText>(R.id.inputSets)
            private val inputReps = view.findViewById<EditText>(R.id.inputReps)
            private val btnConfirm = view.findViewById<MaterialButton>(R.id.buttonConfirm)

            init {
                btnConfirm.setOnClickListener {
                    val pos = adapterPosition.takeIf { it != RecyclerView.NO_POSITION } ?: return@setOnClickListener
                    val ex = items[pos]
                    ex.setsCount = inputSets.text.toString().toIntOrNull() ?: 0
                    ex.repsCount = inputReps.text.toString().toIntOrNull() ?: 0
                    onConfirmClick(ex)
                }
            }

            fun bind(ex: CardioViewModel.Exercise) {
                titleTv.text = ex.title
                inputSets.setText(ex.setsCount.takeIf { it > 0 }?.toString() ?: "")
                inputReps.setText(ex.repsCount.takeIf { it > 0 }?.toString() ?: "")

                val isTracking = !selectedDate.isNullOrBlank()
                listOf(inputSets, inputReps, btnConfirm).forEach {
                    it.visibility = if (isTracking) View.VISIBLE else View.GONE
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
