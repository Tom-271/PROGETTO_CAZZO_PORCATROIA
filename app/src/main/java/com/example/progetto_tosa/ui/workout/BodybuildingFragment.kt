package com.example.progetto_tosa.ui.workout

import android.content.res.Configuration
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class BodybuildingFragment : Fragment(R.layout.fragment_bodybuilding) {

    // modello dati per un esercizio con due contatori (sets/reps) e modalità corrente
    data class Exercise(
        val imageRes: Int,
        val descriptionImage: Int,
        val title: String,
        val videoUrl: String,
        val description: String,
        val subtitle2: String,
        val description2: String,
        val detailImage1Res: Int,
        val detailImage2Res: Int,
        val descrizioneTotale: String,
        var setsCount: Int = 0,
        var repsCount: Int = 0,
        var isSetsMode: Boolean = true
    )

    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()

    // flag che indica se l'utente autenticato è Personal Trainer
    private var userIsPT: Boolean = false

    // dati di esempio
    private val sharedImage = R.drawable.pancadescrizione
    private val section1 = listOf(
        Exercise(
            imageRes           = sharedImage,
            descriptionImage   = sharedImage,
            title              = "PANCA PIANA",
            videoUrl           = "https://www.youtube.com/watch?v=nclAIgM4NJE",
            description        = "La panca piana è un esercizio fondamentale per lo sviluppo della forza e della massa muscolare del petto. Ideale per migliorare la spinta e la stabilità della parte superiore del corpo.",
            subtitle2          = "MUSCOLI COINVOLTI",
            description2       = "- Grande pettorale\n- Deltoide anteriore\n- Tricipite brachiale",
            detailImage1Res    = sharedImage,
            detailImage2Res    = sharedImage,
            descrizioneTotale  = "Per l’ipertrofia del pettorale si consigliano 3–4 serie da 8–12 ripetizioni per esercizio, con pause di 60–90 s e due sedute settimanali."
        ),
        Exercise(
            imageRes           = sharedImage,
            descriptionImage   = R.drawable.petto,
            title              = "SPINTE",
            videoUrl           = "https://youtu.be/VIDEO_ID_SPINTE",
            description        = "Spinte con manubri su panca inclinata.",
            subtitle2          = "MUSCOLI COINVOLTI",
            description2       = "- Grande pettorale\n- Deltoide anteriore\n- Tricipite brachiale",
            detailImage1Res    = sharedImage,
            detailImage2Res    = sharedImage,
            descrizioneTotale  = "Per l’ipertrofia variare angoli e impugnature per sollecitare tutte le fibre."
        ),
        Exercise(
            imageRes           = sharedImage,
            descriptionImage   = sharedImage,
            title              = "CROCI AI CAVI",
            videoUrl           = "https://youtu.be/VIDEO_ID_CROCI",
            description        = "Croci ai cavi per isolare il petto.",
            subtitle2          = "MUSCOLI COINVOLTI",
            description2       = "- Grande pettorale\n- Deltoide anteriore\n- Tricipite brachiale",
            detailImage1Res    = sharedImage,
            detailImage2Res    = sharedImage,
            descrizioneTotale  = "Mantenere tensione costante durante tutto il movimento."
        ),
        Exercise(
            imageRes           = sharedImage,
            descriptionImage   = sharedImage,
            title              = "CHEST PRESS",
            videoUrl           = "https://youtu.be/VIDEO_ID_CHESTPRESS",
            description        = "Macchina Chest Press per spinta controllata.",
            subtitle2          = "MUSCOLI COINVOLTI",
            description2       = "- Grande pettorale\n- Deltoide anteriore\n- Tricipite brachiale",
            detailImage1Res    = sharedImage,
            detailImage2Res    = sharedImage,
            descrizioneTotale  = "Regolare il sedile per mantenere gomiti allineati al petto."
        ),
        Exercise(
            imageRes           = sharedImage,
            descriptionImage   = sharedImage,
            title              = "PECTORAL MACHINE",
            videoUrl           = "https://youtu.be/VIDEO_ID_PECTORAL",
            description        = "Esercizio guidato su Pectoral Machine.",
            subtitle2          = "MUSCOLI COINVOLTI",
            description2       = "- Grande pettorale\n- Deltoide anteriore\n- Tricipite brachiale",
            detailImage1Res    = sharedImage,
            detailImage2Res    = sharedImage,
            descrizioneTotale  = "Mantenere schiena dritta e torace in fuori."
        )
    )
    private val section2 = listOf(section1[0])
    private val section3 = listOf(section1[0])
    private val section4 = listOf(section1[0])
    private val section5 = listOf(section1[0])

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // se utente loggato, prelevo isPersonalTrainer da Firestore
        auth.currentUser?.uid?.let { uid ->
            db.collection("users").document(uid)
                .get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        userIsPT = doc.getBoolean("isPersonalTrainer") == true
                        initUI(view)
                    } else {
                        db.collection("personal_trainers").document(uid)
                            .get()
                            .addOnSuccessListener { ptDoc ->
                                userIsPT = ptDoc.exists()
                                initUI(view)
                            }
                            .addOnFailureListener {
                                userIsPT = false
                                initUI(view)
                            }
                    }
                }
                .addOnFailureListener {
                    userIsPT = false
                    initUI(view)
                }
        } ?: run {
            // non loggato
            userIsPT = false
            initUI(view)
        }
    }

    private fun initUI(root: View) {
        applyStrokeColor(root)
        setupSection(R.id.cardSection1, R.id.rvSection1, section1)
        setupSection(R.id.cardSection2, R.id.rvSection2, section2)
        setupSection(R.id.cardSection3, R.id.rvSection3, section3)
        setupSection(R.id.cardSection4, R.id.rvSection4, section4)
        setupSection(R.id.cardSection5, R.id.rvSection5, section5)
    }

    private fun applyStrokeColor(root: View) {
        val night = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val strokeColorRes = if (night == Configuration.UI_MODE_NIGHT_YES) R.color.white else R.color.black
        val strokeColor = ContextCompat.getColor(requireContext(), strokeColorRes)
        listOf(
            R.id.cardSection1, R.id.cardSection2, R.id.cardSection3,
            R.id.cardSection4, R.id.cardSection5
        ).forEach { id ->
            root.findViewById<MaterialCardView>(id).setStrokeColor(strokeColor)
        }
    }

    private fun setupSection(
        headerId: Int,
        recyclerId: Int,
        data: List<Exercise>
    ) {
        val headerCard   = requireView().findViewById<MaterialCardView>(headerId)
        val recyclerView = requireView().findViewById<RecyclerView>(recyclerId)

        headerCard.setOnClickListener {
            recyclerView.visibility =
                if (recyclerView.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        recyclerView.adapter = ExerciseAdapter(
            items           = data,
            userIsPT        = userIsPT,
            onCardClick     = { ex -> openDetail(ex) },
            onConfirmClick  = { ex -> saveExercise(ex) }
        )
    }

    private inner class ExerciseAdapter(
        private val items: List<Exercise>,
        private val userIsPT: Boolean,
        private val onCardClick: (Exercise) -> Unit,
        private val onConfirmClick: (Exercise) -> Unit
    ) : RecyclerView.Adapter<ExerciseAdapter.VH>() {

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            private val card        : MaterialCardView     = view.findViewById(R.id.cardExercise)
            private val titleTv     : TextView             = view.findViewById(R.id.textViewTitleTop)
            private val btnSets     : MaterialButton       = view.findViewById(R.id.toggleSets)
            private val btnReps     : MaterialButton       = view.findViewById(R.id.toggleReps)
            private val counterSets : TextView             = view.findViewById(R.id.counterSets)
            private val counterReps : TextView             = view.findViewById(R.id.counterReps)
            private val btnMinus    : FloatingActionButton = view.findViewById(R.id.buttonMinus)
            private val btnPlus     : FloatingActionButton = view.findViewById(R.id.buttonPlus)
            private val btnConfirm  : MaterialButton       = view.findViewById(R.id.buttonConfirm)

            private val colorGreen  = ContextCompat.getColor(view.context, R.color.green)
            private val colorOrange = ContextCompat.getColor(view.context, R.color.orange)
            private val colorBlack  = ContextCompat.getColor(view.context, R.color.black)

            init {
                card.setOnClickListener    { items.getOrNull(adapterPosition)?.let(onCardClick) }
                btnConfirm.setOnClickListener { items.getOrNull(adapterPosition)?.let(onConfirmClick) }

                btnSets.setOnClickListener {
                    val ex = items[adapterPosition]
                    ex.isSetsMode = true
                    bind(ex)
                }
                btnReps.setOnClickListener {
                    val ex = items[adapterPosition]
                    ex.isSetsMode = false
                    bind(ex)
                }
                btnPlus.setOnClickListener {
                    val ex = items[adapterPosition]
                    if (ex.isSetsMode) ex.setsCount++ else ex.repsCount++
                    bind(ex)
                }
                btnMinus.setOnClickListener {
                    val ex = items[adapterPosition]
                    if (ex.isSetsMode && ex.setsCount > 0) ex.setsCount--
                    else if (!ex.isSetsMode && ex.repsCount > 0) ex.repsCount--
                    bind(ex)
                }
            }

            private fun highlightToggles(ex: Exercise) {
                counterSets.visibility = if (ex.isSetsMode) View.VISIBLE else View.GONE
                counterReps.visibility = if (ex.isSetsMode) View.GONE else View.VISIBLE
                btnSets.isChecked = ex.isSetsMode
                btnReps.isChecked = !ex.isSetsMode

                if (ex.isSetsMode) {
                    btnSets.backgroundTintList = ColorStateList.valueOf(colorGreen)
                    btnReps.backgroundTintList = ColorStateList.valueOf(colorBlack)
                } else {
                    btnReps.backgroundTintList = ColorStateList.valueOf(colorGreen)
                    btnSets.backgroundTintList = ColorStateList.valueOf(colorBlack)
                }
            }

            fun bind(ex: Exercise) {                          //è colui che apporta le modifiche grafiche con i dati creati da kotlin
                titleTv.text        = ex.title
                counterSets.text    = ex.setsCount.toString()
                counterReps.text    = ex.repsCount.toString()
                highlightToggles(ex)                        //la chiamiamo ad ogni onclicklistener di più meno ecc
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
            ex.subtitle2,
            ex.description2,
            ex.detailImage1Res,
            ex.detailImage2Res,
            ex.descriptionImage,
            ex.descrizioneTotale
        ).show((requireActivity() as FragmentActivity)
            .supportFragmentManager, "exercise_detail")
    }

    private fun saveExercise(ex: Exercise) {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(requireContext(),
                "Devi essere loggato per salvare.", Toast.LENGTH_SHORT).show()
            return
        }
        val data = hashMapOf(
            "nomeEsercizio"     to ex.title,
            "numeroSerie"       to ex.setsCount,
            "numeroRipetizioni" to ex.repsCount
        )
        if (ex.setsCount > 0 && ex.repsCount > 0) {
            db.collection("scheda_creata_autonomamente")
                .add(data)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(),
                        "Esercizio salvato", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(),
                        "Errore nel salvataggio", Toast.LENGTH_LONG).show()
                }
        } else {
            Toast.makeText(requireContext(),
                "Numero serie e/o ripetizioni non valido", Toast.LENGTH_SHORT).show()
        }
    }
}
