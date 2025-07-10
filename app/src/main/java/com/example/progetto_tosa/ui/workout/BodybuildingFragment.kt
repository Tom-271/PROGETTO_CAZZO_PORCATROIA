package com.example.progetto_tosa.ui.workout

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.progetto_tosa.R
import com.example.progetto_tosa.databinding.FragmentBodybuildingBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Fragment per la selezione e il salvataggio di esercizi di Bodybuilding.
 * Mostra diverse sezioni di esercizi, permette di visualizzare dettagli e di salvare
 * l'esercizio nella scheda giornaliera o del Personal Trainer.
 */
class BodybuildingFragment : Fragment(R.layout.fragment_bodybuilding) {

    // Binding per il layout fragment_bodybuilding.xml
    private var _binding: FragmentBodybuildingBinding? = null
    private val binding get() = _binding!!

    // ViewModel che gestisce i dati e la logica degli esercizi
    private val vm: BodybuildingViewModel by viewModels()

    // Accesso diretto a Firestore per salvare gli esercizi (evita di esporre vm.db)
    private val db = FirebaseFirestore.getInstance()

    // Opzionali: utente selezionato (PT) e data selezionata dalla schermata precedente
    private val selectedUser: String? by lazy { arguments?.getString("selectedUser") }
    private val selectedDate: String? by lazy { arguments?.getString("selectedDate") }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentBodybuildingBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)
        // Collego il ViewModel per DataBinding
        binding.viewModel = vm
        binding.lifecycleOwner = viewLifecycleOwner

        // Inizializza UI (riempi sezioni statiche)
        initUI()
        // Osservo le modifiche nella sezione1 (esercizi salvati)
        observeData()

        // Se è stata passata una data, carica gli esercizi già salvati
        if (!selectedDate.isNullOrBlank()) {
            vm.loadSavedExercises(selectedDate) { setupSection1() }
        }
    }

    // Inizializza le sezioni fisse: applica colori e configura RecyclerView.

    private fun initUI() {
        applyStrokeColor(binding.cardSection1)
        setupSection(binding.cardSection2, binding.rvSection2, vm.section2.value!!)
        setupSection(binding.cardSection3, binding.rvSection3, vm.section3.value!!)
        setupSection(binding.cardSection4, binding.rvSection4, vm.section4.value!!)
        setupSection(binding.cardSection5, binding.rvSection5, vm.section5.value!!)
    }

    // Osserva la LiveData della sezione1 (Bodybuilding) e aggiorna la UI.

    private fun observeData() {
        vm.section1.observe(viewLifecycleOwner) { list ->
            setupSection1(list)
        }
    }

    // Configura dinamicamente la sezione1 con i dati passati o quelli salvati.

    private fun setupSection1(list: List<BodybuildingViewModel.Exercise> = vm.section1.value!!) {
        setupSection(binding.cardSection1, binding.rvSection1, list)
    }

    // Applica il colore del bordo al MaterialCardView in base al tema (day/night).

    private fun applyStrokeColor(card: MaterialCardView) {
        val night = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val colorRes = if (night == Configuration.UI_MODE_NIGHT_YES) R.color.black else R.color.black
        card.strokeColor = ContextCompat.getColor(requireContext(), colorRes)
    }

    /**
     * Imposta un RecyclerView con GridLayoutManager a 2 colonne
     * e associa l'adapter che gestisce click e salvataggio.
     * Il click sull'header mostra/nasconde la lista.
     */
    private fun setupSection(
        headerCard: MaterialCardView,
        recyclerView: RecyclerView,
        data: List<BodybuildingViewModel.Exercise>
    ) {
        recyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = ExerciseAdapter(data, ::openDetail, ::saveExercise)
        }
        headerCard.setOnClickListener {
            recyclerView.visibility = if (recyclerView.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }
    }

    // Mostra un dialog di dettaglio con titolo, video e descrizioni dell'esercizio.

    private fun openDetail(ex: BodybuildingViewModel.Exercise) {
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
        ).show(childFragmentManager, "exercise_detail")
    }

    /**
     * Salva l'esercizio selezionato in Firestore nella raccolta corretta:
     * - Schede del PT se selectedUser è valorizzato
     * - Schede giornaliere dell'utente altrimenti
     * Controlla serie/ripetizioni e mostra messaggi di errore o conferma.
     */
    private fun saveExercise(ex: BodybuildingViewModel.Exercise) {
        if (selectedDate.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Seleziona prima una data per aggiungere l'esercizio", Toast.LENGTH_SHORT).show()
            return
        }
        if (ex.setsCount <= 0 || ex.repsCount <= 0) {
            Toast.makeText(requireContext(), "Imposta almeno 1 serie e 1 ripetizione", Toast.LENGTH_SHORT).show()
            return
        }

        // Costruisco i dati da salvare
        val data = hashMapOf(
            "category" to ex.category,
            "nomeEsercizio" to ex.title,
            "numeroSerie" to ex.setsCount,
            "numeroRipetizioni" to ex.repsCount,
            "muscoloPrincipale" to ex.muscoloPrincipale,
            "createdAt" to FieldValue.serverTimestamp()
        )
        val successMessage = "Esercizio \"${ex.title}\" aggiunto con successo"

        // Recupero nome utente salvato in SharedPreferences
        val prefs = requireActivity().getSharedPreferences("user_data", Context.MODE_PRIVATE)
        val currentUserName = prefs.getString("saved_display_name", null)

        // Determino il path della collezione in Firestore
        val collectionRef = when {
            !selectedUser.isNullOrBlank() -> db
                .collection("schede_del_pt").document(selectedUser!!)
                .collection(selectedDate!!).document(ex.category)
                .collection("esercizi")
            !currentUserName.isNullOrBlank() -> db
                .collection("schede_giornaliere").document(currentUserName!!)
                .collection(selectedDate!!).document(ex.category)
                .collection("esercizi")
            else -> {
                Toast.makeText(requireContext(), "Impossibile identificare l'utente", Toast.LENGTH_SHORT).show()
                return
            }
        }

        // Salvataggio effettivo e listener di conferma/errore
        collectionRef.document(ex.title)
            .set(data)
            .addOnSuccessListener { Toast.makeText(requireContext(), successMessage, Toast.LENGTH_SHORT).show() }
            .addOnFailureListener { Toast.makeText(requireContext(), "Errore durante il salvataggio", Toast.LENGTH_SHORT).show() }
    }

    // Adapter per visualizzare le card degli esercizi con toggle di serie/ripetizioni
    // e azione di conferma salvataggio.

    private inner class ExerciseAdapter(
        private val items: List<BodybuildingViewModel.Exercise>,
        private val onCardClick: (BodybuildingViewModel.Exercise) -> Unit,
        private val onConfirmClick: (BodybuildingViewModel.Exercise) -> Unit
    ) : RecyclerView.Adapter<ExerciseAdapter.VH>() {

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            private val titleTv = view.findViewById<TextView>(R.id.textViewTitleTop)
            private val btnSets = view.findViewById<MaterialButton>(R.id.toggleSets)
            private val btnReps = view.findViewById<MaterialButton>(R.id.toggleReps)
            private val counterSets = view.findViewById<TextView>(R.id.counterSets)
            private val counterReps = view.findViewById<TextView>(R.id.counterReps)
            private val btnMinus = view.findViewById<FloatingActionButton>(R.id.buttonMinus)
            private val btnPlus = view.findViewById<FloatingActionButton>(R.id.buttonPlus)
            private val btnConfirm = view.findViewById<MaterialButton>(R.id.buttonConfirm)
            private val green = ContextCompat.getColor(view.context, R.color.green)
            private val black = ContextCompat.getColor(view.context, R.color.black)

            init {
                // Listener per il pulsante di conferma salvataggio
                btnConfirm.setOnClickListener {
                    val pos = adapterPosition.takeIf { it != RecyclerView.NO_POSITION } ?: return@setOnClickListener
                    onConfirmClick(items[pos])
                }
                // Toggle tra conteggio serie e ripetizioni
                btnSets.setOnClickListener { toggleMode(true) }
                btnReps.setOnClickListener { toggleMode(false) }
                // Incremento/decremento del contatore
                btnPlus.setOnClickListener { adjustCount(1) }
                btnMinus.setOnClickListener { adjustCount(-1) }
            }

            // Cambia la modalità tra serie e ripetizioni, aggiorna item

            private fun toggleMode(isSets: Boolean) {
                val pos = adapterPosition.takeIf { it != RecyclerView.NO_POSITION } ?: return
                items[pos].isSetsMode = isSets
                notifyItemChanged(pos)
            }

            // Modifica il conteggio (serie o ripetizioni) con un delta
            private fun adjustCount(delta: Int) {
                val pos = adapterPosition.takeIf { it != RecyclerView.NO_POSITION } ?: return
                val ex = items[pos]
                if (ex.isSetsMode) ex.setsCount = maxOf(0, ex.setsCount + delta)
                else ex.repsCount = maxOf(0, ex.repsCount + delta)
                notifyItemChanged(pos)
            }

            // Collega i dati dell'esercizio alla UI e gestisce visibilità controll
            fun bind(ex: BodybuildingViewModel.Exercise) {
                titleTv.text = ex.title
                counterSets.text = ex.setsCount.toString()
                counterReps.text = ex.repsCount.toString()
                btnSets.isChecked = ex.isSetsMode
                btnReps.isChecked = !ex.isSetsMode
                btnSets.backgroundTintList = android.content.res.ColorStateList.valueOf(if (ex.isSetsMode) green else black)
                btnReps.backgroundTintList = android.content.res.ColorStateList.valueOf(if (ex.isSetsMode) black else green)

                // Mostra i controlli solo se la data è stata selezionata
                val isTracking = selectedDate != null
                listOf(btnSets, btnReps, btnPlus, btnMinus, btnConfirm).forEach {
                    it.visibility = if (isTracking) View.VISIBLE else View.GONE
                }
                counterSets.visibility = if (isTracking && ex.isSetsMode) View.VISIBLE else View.GONE
                counterReps.visibility = if (isTracking && !ex.isSetsMode) View.VISIBLE else View.GONE

                // Se non in modalità tracking, al click apro il dettaglio
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Previene memory leak liberando il binding
    }
}