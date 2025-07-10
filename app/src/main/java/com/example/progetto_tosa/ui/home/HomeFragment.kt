package com.example.progetto_tosa.ui.home

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.progetto_tosa.R
import com.example.progetto_tosa.databinding.FragmentHomeBinding
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    //binding al layout generato per il fragment
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!! //accede in modo sicuro al binding

    //inizializzazione dei servizi di autenticazione e database firestore
    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()

    companion object {
        private const val TAG = "HomeFragment" //tag per i log
        //definizione delle categorie di esercizi gestite dal pt
        private val CATEGORIES = listOf("bodybuilding", "cardio", "corpo_libero", "stretching")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        //infla il layout e inizializza il binding
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root //restituisce la root della view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //1) gestione della data di oggi
        val today       = Date() //ottiene la data corrente
        val displayFmt  = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()) //formato per visualizzazione
        val keyFmt      = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) //formato per chiavi firestore
        binding.bannerDate.text = displayFmt.format(today) //imposta la data nel banner
        val todayId = keyFmt.format(today) //chiave data per firestore
        Log.d(TAG, "TodayId = $todayId") //log della chiave data

        //2) verifica dello stato di login dell'utente
        val user = auth.currentUser //recupera l'utente autenticato
        if (user == null) {
            Log.d(TAG, "Utente non loggato") //log se non loggato
            binding.bannerStatus.visibility = View.GONE //nasconde lo status
            setAllGone() //mostra ui per utente non loggato
            return //esce dal metodo
        }

        //3) verifica del ruolo dell'utente (trainer o atleta)
        val prefs     = requireActivity()
            .getSharedPreferences("user_data", Context.MODE_PRIVATE) //sharedpreferences per dati utente
        val isTrainer = prefs.getBoolean("is_trainer", false) //legge il flag is_trainer
        if (isTrainer) {
            Log.d(TAG, "Accesso come PT") //log se pt
            binding.bannerStatus.visibility = View.GONE //nasconde lo status
            showButtonsForPT() //mostra pulsanti per pt
            return //esce dal metodo
        }

        //4) se l'utente è un "atleta normale"
        showButtonsForUser() //mostra pulsanti per utente

        //5) recupero del nome completo salvato
        val fullName = prefs.getString("saved_display_name",""
        ).orEmpty() //nome visualizzato
        if (fullName.isBlank()) {
            Log.e(TAG, "FullName mancante") //log errore se nome mancante
            binding.bannerStatus.text = "Errore controllo scheda" //messaggio di errore
            return //esce dal metodo
        }

        //6) verifica presenza esercizi per oggi
        db.collection("schede_del_pt")
            .document(fullName) //seleziona il documento utente
            .let { userDocRef ->
                //crea un task per ogni categoria per leggere almeno 1 esercizio
                val tasks = CATEGORIES.map { category ->
                    userDocRef
                        .collection(todayId)
                        .document(category)
                        .collection("esercizi")
                        .limit(1)
                        .get() //recupera dati
                }
                //quando tutti i task sono completati
                Tasks.whenAllSuccess<QuerySnapshot>(tasks)
                    .addOnSuccessListener { snaps ->
                        //controlla se almeno un task ha restituito documenti
                        val hasAny = snaps.any { it.documents.isNotEmpty() }
                        binding.bannerStatus.text = if (hasAny) {
                            "Sì! il pt ha caricato una scheda." //se ci sono esercizi
                        } else {
                            "No! oggi giornata libera!" //se non ci sono esercizi
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Errore controllo esercizi", e) //log errore
                        binding.bannerStatus.text = "Errore controllo scheda" //messaggio di errore
                    }
            }
    }

    private fun setAllGone() {
        //gestisce la ui quando l'utente non è loggato
        binding.buttonForTheScheduleIDid.visibility             = View.GONE //nasconde pulsanti
        binding.buttonForTheSchedulePersonalTrainerDid.visibility = View.GONE
        binding.buttonForPersonalTrainer.visibility             = View.GONE
        binding.buttonInutile.visibility                        = View.VISIBLE //mostra pulsante di login
        binding.buttonInutile.strokeColor = ColorStateList.valueOf(
            ContextCompat.getColor(requireContext(), R.color.orange)
        ) //imposta colore bordo pulsante
        binding.buttonInutile.text = "Effettua il login per accedere al servizio" //testo pulsante
        binding.buttonInutile.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_home_to_navigation_account)
        } //naviga a login
        binding.buttonForPersonalTrainer.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_home_to_navigation_myautocalendar)
        }
    }

    private fun showButtonsForUser() {
        //mostra pulsanti a un utente normale
        val prefs    = requireActivity()
            .getSharedPreferences("user_data", Context.MODE_PRIVATE) //sharedpreferences
        val fullName = prefs.getString("saved_display_name",""
        ).orEmpty() //nome utente

        binding.buttonForPersonalTrainer.visibility               = View.GONE //nasconde pulsante pt
        binding.buttonForTheScheduleIDid.visibility               = View.VISIBLE //mostra pulsante visualizza scheda
        binding.buttonForTheSchedulePersonalTrainerDid.visibility = View.VISIBLE //mostra pulsante calendario pt

        binding.buttonForTheScheduleIDid.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_home_to_navigation_myautocalendar)
        } //naviga a calendario personale
        binding.buttonForTheSchedulePersonalTrainerDid.setOnClickListener {
            if (fullName.isBlank()) {
                Toast.makeText(requireContext(), "Nome utente non disponibile", Toast.LENGTH_SHORT).show()
                return@setOnClickListener //se nome mancante, mostra toast ed esce
            }
            findNavController().navigate(
                R.id.action_navigation_home_to_pt_schedule,
                bundleOf("selectedUser" to fullName)
            ) //naviga a calendario pt con argomento
        }
    }

    private fun showButtonsForPT() {
        //mostra pulsanti a un pt
        binding.buttonForPersonalTrainer.visibility               = View.VISIBLE //mostra pulsante programma pt
        binding.buttonForTheScheduleIDid.visibility               = View.GONE //nasconde altri pulsanti
        binding.buttonForTheSchedulePersonalTrainerDid.visibility = View.GONE


        binding.buttonForPersonalTrainer.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_home_to_navigation_myautocalendar)
        } //naviga a auto-schedule
    }
    override fun onDestroyView() {
        super.onDestroyView()
        //pulisce il binding per evitare memory leak
        _binding = null
    }
}
