package com.example.progetto_tosa.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import android.widget.TextView
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.progetto_tosa.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.example.progetto_tosa.databinding.FragmentMyAutoScheduleBinding

class MyAutoScheduleFragment : Fragment() {

    // ---------- view reference ----------
    private lateinit var subtitleBodyBuilding: TextView
    private lateinit var subtitleStretching: TextView
    private lateinit var subtitleCardio: TextView
    private lateinit var subtitleCorpoLibero: TextView
    private lateinit var overlayLayout: LinearLayout
    private lateinit var overlayButton: Button
    private lateinit var btnBodybuilding: Button
    private lateinit var btnStretching: Button
    private lateinit var btnCardio: Button
    private lateinit var btnCorpoLibero: Button

    private lateinit var bodybuildingDetailsContainer: LinearLayout
    private lateinit var stretchingDetailsContainer: LinearLayout
    private lateinit var cardioDetailsContainer: LinearLayout
    private lateinit var corpoLiberoDetailsContainer: LinearLayout

    // ---------- firestore ----------
    private val db = FirebaseFirestore.getInstance()
    private val activeListeners = mutableListOf<ListenerRegistration>()
    private var categoriesCompleted = 0
    private var totalGlobalExercises = 0

    // ---------- costanti ----------
    private val bodybuildingMuscoli = listOf("petto", "gambe", "spalle", "dorso", "bicipiti", "tricipiti")
    private val stretchingMuscoli   = listOf("stretch1", "stretch2")
    private val cardioMuscoli       = listOf("cardio1", "cardio2")
    private val corpoLiberoMuscoli  = listOf("libero1", "libero2")


    private var _binding: FragmentMyAutoScheduleBinding? = null
    private val binding get() = _binding!!

    // ---------- lifecycle ----------
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentMyAutoScheduleBinding.inflate(inflater, container, false)
        val view = binding.root

        // sottotitoli
        subtitleBodyBuilding = binding.subtitleBodyBuilding
        subtitleStretching   = binding.subtitleStretching
        subtitleCardio       = binding.subtitleCardio
        subtitleCorpoLibero  = binding.subtitleCorpoLibero

        // container dettagli
        bodybuildingDetailsContainer = binding.bodybuildingDetailsContainer
        stretchingDetailsContainer   = binding.stretchingDetailsContainer
        cardioDetailsContainer       = binding.cardioDetailsContainer
        corpoLiberoDetailsContainer  = binding.corpoliberoDetailsContainer

        // overlay
        overlayLayout = binding.overlayLayout
        overlayButton = binding.overlayButton

        // bottoni
        btnBodybuilding = binding.btnBodybuilding
        btnStretching   = binding.btnStretching
        btnCardio       = binding.btnCardio
        btnCorpoLibero  = binding.btnCorpoLibero

        return view
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Espansione/chiusura + lazy-loading degli esercizi
        btnBodybuilding.setOnClickListener {            //questi sono i listener che gestiscono la comparsa degli esercizi sotto le cards
            toggleContainer(bodybuildingDetailsContainer) {
                loadAllExercisesOnce("bodybuilding", bodybuildingMuscoli, bodybuildingDetailsContainer)
            }
        }

        btnStretching.setOnClickListener {
            toggleContainer(stretchingDetailsContainer) {
                loadAllExercisesOnce("stretching", stretchingMuscoli, stretchingDetailsContainer)
            }
        }

        btnCardio.setOnClickListener {
            toggleContainer(cardioDetailsContainer) {
                loadAllExercisesOnce("cardio", cardioMuscoli, cardioDetailsContainer)
            }
        }

        btnCorpoLibero.setOnClickListener {
            toggleContainer(corpoLiberoDetailsContainer) {
                loadAllExercisesOnce("corpo_libero", corpoLiberoMuscoli, corpoLiberoDetailsContainer)
            }
        }

        // ----- listener conteggio esercizi ----- sono i rimandi alla funzione, la stessa funzione ma con input diversi
        listenToExerciseCount("bodybuilding",   bodybuildingMuscoli, subtitleBodyBuilding)
        listenToExerciseCount("stretching",     stretchingMuscoli,   subtitleStretching)
        listenToExerciseCount("cardio",         cardioMuscoli,       subtitleCardio)
        listenToExerciseCount("corpo_libero",   corpoLiberoMuscoli,  subtitleCorpoLibero)

    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Stoppa tutti i listener Firestore per evitare memory leak
        activeListeners.forEach { it.remove() }
        activeListeners.clear()
    }

    // ---------- toggle ----------
    private inline fun toggleContainer(container: LinearLayout, onOpen: () -> Unit) {
        if (container.visibility == View.GONE) {
            container.visibility = View.VISIBLE
            onOpen()          // carica la lista solo al primo expand
        } else {
            container.visibility = View.GONE
        }
    }

    // ---------- conteggio ----------     //funzione dedita al conteggio degli esercizi su firebase
    private fun listenToExerciseCount(
        category: String,
        muscoli: List<String>,
        subtitleView: TextView
    ) {
        var totalCount = 0      //counters esercizi
        var completed  = 0
        var totalExercises = 0

        muscoli.forEach { muscolo ->
            db.collection("scheda_creata_autonomamente")
                .document(category)
                .collection(muscolo)
                .addSnapshotListener { snapshot, error ->           //snapshot è funzione di firebase che consente di accedere ai suoi annidamenti
                    completed++                                     //snapshot utilissima per accedere costantemente a firebase senza necessità di refresh manuale ogni volta
                    if (error == null) {                            //se errore nulla allora conta, se la dimensione è nulla passi 0 al counter nella somma
                        totalCount += snapshot?.size() ?: 0
                    }
                    totalExercises += totalCount

                    if (completed == muscoli.size) {                //dopo aver controllato tutti gli elementi, quindi tutti gli esercizi per tipologia, controllo che siano tutti a 0, basta 1 eserczio dentro una sezione per farmi scomparire il button per la creazione di un ashceda
                        totalGlobalExercises += totalCount
                        categoriesCompleted++
                        checkCounterButton(totalGlobalExercises)

                        subtitleView.text = "$totalCount exercises"     //se ho finito di iterare su tutto l'array che ho creato prima, trascrivo il risultato nel sottotitolo
                    }
                }
        }
    }

    private fun checkCounterButton(total: Int){

        if (total == 0){                                //se tutti i counter sono a 0 allora compaiono la scritta, la view trasparente, e il button per invitarmi ad assemblare una scheda
            overlayLayout.visibility = View.VISIBLE     //compaiono
            overlayButton.visibility = View.VISIBLE
            btnBodybuilding.isClickable = false
            btnStretching.isClickable = false
            btnCardio.isClickable = false
            btnCorpoLibero.isClickable = false          //attivo o disattivo i pulsanti per una questione grafica, seppur la scheda sia vuota, senza questi i bottoni farebbero apparirre una piccola spaziatura, ossia un tentativo di far comparire un container vuoto, piu preciso cosi

            overlayButton.setOnClickListener {
                findNavController().navigate(R.id.fragment_workout)
            }

        }
        else
        {
            overlayLayout.visibility = View.GONE        //scompaiono
            overlayButton.visibility = View.GONE
            btnBodybuilding.isClickable = true
            btnStretching.isClickable = true
            btnCardio.isClickable = true
            btnCorpoLibero.isClickable = true
        }
    }

    // ---------- caricamento lista ----------
    private fun loadAllExercisesOnce(       //anche questa funzione viene chiamata 4 volte, tante quanto ogni singolo elemento pesi, cardio ecc
        category: String,
        listaElementi: List<String>,
        container: LinearLayout             //qui ne definiamo i parametri che gli vengono passati in chiamata
    ) {
        // se hai già figli hai già caricato
        if (container.childCount > 0) return        //esce se il contenitore è gia stato riempito
        val white   = ContextCompat.getColor(requireContext(), android.R.color.white)
        val greyBg  = ContextCompat.getColor(requireContext(), R.color.transparent)
        val dark_gray   = ContextCompat.getColor(requireContext(), R.color.dark_gray)
        val light_gray  = ContextCompat.getColor(requireContext(), R.color.light_gray)
        val sky  = ContextCompat.getColor(requireContext(), R.color.sky)

        listaElementi.forEach { muscolo ->

            db.collection("scheda_creata_autonomamente")
                .document(category)     //accede al documento
                .collection(muscolo)    //alla collezione con lòo stesso nome del muscolo nella lista
                .get()                  //ne prende il contenuto (tutti gli esercizi
                .addOnSuccessListener { query ->
                    if (query.isEmpty) return@addOnSuccessListener      //se vuoto esco, non ho esercizi (penso sia un if che non verrà mai eseguito visto che la categoria es petto viene creata letteralmente solo qundo ne agigungo l'esercizio corrispondetne, ma mettiamola non si sa mai

                    /* divisore */
                    val divider = View(requireContext()).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, 3  // ← aumentato lo spessore
                        ).apply {
                            setMargins(40, 50, 40, 0)
                        }
                        setBackgroundColor(dark_gray)
                    }
                    container.addView(divider)

                    /* header esercizio */
                    val header = TextView(requireContext()).apply { //creiamo letteralmetne il contenitore dei nostri dati
                        text = muscolo.uppercase()
                        setTypeface(null, Typeface.BOLD)
                        setTextColor(sky)
                        setPadding(40, 30, 0, 0)
                        textSize = 20f                              //puro xlm ma applicato da codice
                    }
                    container.addView(header)                       //aggiungiamolo

                    query.forEach { doc ->                          //accediamo al contenuto del doc su firebase
                        val nome = doc.getString("nome") ?: doc.id
                        val serie = doc.getLong("numeroSerie")?.toString() ?: "0"
                        val rep = doc.getLong("numeroRipetizioni")?.toString() ?: "0"

                        val tv = TextView(requireContext()).apply {
                            text = nome                             //prendiamo ogni esercizio e lo mettiamo nel contenitore con le seguenti specifiche estetiche
                            setTextColor(white)
                            setPadding(18, 0, 8, 4)
                            textSize = 16f
                            setBackgroundColor(greyBg)              //aggiunto sfondo grigio
                        }

                        val horizontalLayout = LinearLayout(requireContext()).apply {
                            orientation = LinearLayout.HORIZONTAL
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply {
                                setMargins(80, 24, 8, 30)
                            }
                            setBackgroundColor(greyBg)              //aggiunto sfondo grigio
                        }

                        val testoserierep = TextView(requireContext()).apply {
                            text = "○ Ripetizioni: $rep, Serie: $serie "
                            setTextColor(light_gray)
                            textSize = 16f
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply {
                                setMargins(0, 0, 0, 24) // 👈 aggiunge margin-bottom di 24dp
                            }
                        }


                        val bottone = Button(requireContext()).apply {
                            background = ContextCompat.getDrawable(context, R.drawable.tick)
                            layoutParams = LinearLayout.LayoutParams(50, 50).apply {                //dimensione fissa per farlo perfettamente tondo
                                setMargins(90, 0, 0, 24)
                            }
                            setOnClickListener {
                                doc.reference.delete().addOnSuccessListener {
                                    container.removeView(horizontalLayout)
                                    val index = container.indexOfChild(tv)
                                    val nextView = container.getChildAt(index + 1)
                                    if (nextView !is LinearLayout) {
                                        Toast.makeText(requireContext(), "Esercizio $nome completato!", Toast.LENGTH_SHORT).show()
                                        container.removeView(tv)
                                    }
                                }
                            }
                        }

                        horizontalLayout.addView(testoserierep)
                        horizontalLayout.addView(bottone)

                        container.addView(tv)                                                       //ecco cosa metto dentro al contenitore
                        container.addView(horizontalLayout)
                    }
                }
        }
    }
}
