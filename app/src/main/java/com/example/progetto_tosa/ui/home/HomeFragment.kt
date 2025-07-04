package com.example.progetto_tosa.ui.home

import android.content.Context
import android.content.Intent
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
import com.example.progetto_tosa.ui.account.LoginFragment
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()

    companion object {
        private const val TAG = "HomeFragment"
        // Lista delle categorie definite dal PT
        private val CATEGORIES = listOf("bodybuilding", "cardio", "corpo_libero", "stretching")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1) Data
        val today       = Date()
        val displayFmt  = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
        val keyFmt      = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        binding.bannerDate.text = displayFmt.format(today)
        val todayId = keyFmt.format(today)
        Log.d(TAG, "TodayId = $todayId")

        // Nascondo inizialmente bannerCard

        // 2) Login
        val user = auth.currentUser
        if (user == null) {
            Log.d(TAG, "Utente non loggato")
            binding.bannerStatus.visibility = View.GONE
            setAllGone()
            return
        }

        // 3) Ruolo
        val prefs     = requireActivity()
            .getSharedPreferences("user_data", Context.MODE_PRIVATE)
        val isTrainer = prefs.getBoolean("is_trainer", false)
        if (isTrainer) {
            Log.d(TAG, "Accesso come PT")
            binding.bannerStatus.visibility = View.GONE
            showButtonsForPT()
            return
        }

        // 4) Atleta “normale”
        showButtonsForUser()

        // 5) Prendo il nome salvato
        val fullName = prefs.getString("saved_display_name","").orEmpty()
        if (fullName.isBlank()) {
            Log.e(TAG, "FullName mancante")
            binding.bannerStatus.text = "Errore controllo scheda"
            return
        }

        // 6) Verifico la presenza di esercizi
        db.collection("schede_del_pt")
            .document(fullName)  // usiamo esattamente il docId trovato in precedenza
            .let { userDocRef ->
                // per ogni categoria creo un Task che legge al massimo 1 esercizio
                val tasks = CATEGORIES.map { category ->
                    userDocRef
                        .collection(todayId)
                        .document(category)
                        .collection("esercizi")
                        .limit(1)
                        .get()
                }
                // quando TUTTI sono completati, controllo se almeno uno ha risultati
                Tasks.whenAllSuccess<QuerySnapshot>(tasks)
                    .addOnSuccessListener { snaps ->
                        val hasAny = snaps.any { it.documents.isNotEmpty() }
                        binding.bannerStatus.text = if (hasAny) {
                            "Sì! Il pt ha caricato una scheda."
                        } else {
                            "No! Oggi giornata libera!"
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Errore controllo esercizi", e)
                        binding.bannerStatus.text = "Errore controllo scheda"
                    }
            }
    }

    private fun setAllGone() {
        binding.buttonForTheScheduleIDid.visibility             = View.GONE
        binding.buttonForTheSchedulePersonalTrainerDid.visibility = View.GONE
        binding.buttonForPersonalTrainer.visibility             = View.GONE
        binding.buttonInutile.visibility                        = View.VISIBLE
        binding.buttonInutile.strokeColor = ColorStateList.valueOf(
            ContextCompat.getColor(requireContext(), R.color.orange)
        )
        binding.buttonInutile.text = "Effettua il login per accedere al servizio"
        binding.buttonInutile.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_home_to_navigation_account)
        }
    }

    private fun showButtonsForUser() {
        val prefs    = requireActivity()
            .getSharedPreferences("user_data", Context.MODE_PRIVATE)
        val fullName = prefs.getString("saved_display_name","").orEmpty()

        binding.buttonForPersonalTrainer.visibility               = View.GONE
        binding.buttonForTheScheduleIDid.visibility               = View.VISIBLE
        binding.buttonForTheSchedulePersonalTrainerDid.visibility = View.VISIBLE

        binding.buttonForTheScheduleIDid.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_home_to_navigation_myautocalendar)
        }
        binding.buttonForTheSchedulePersonalTrainerDid.setOnClickListener {
            if (fullName.isBlank()) {
                Toast.makeText(requireContext(), "Nome utente non disponibile", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            findNavController().navigate(
                R.id.action_navigation_home_to_pt_schedule,
                bundleOf("selectedUser" to fullName)
            )
        }
    }

    private fun showButtonsForPT() {
        binding.buttonForPersonalTrainer.visibility               = View.VISIBLE
        binding.buttonForTheScheduleIDid.visibility               = View.GONE
        binding.buttonForTheSchedulePersonalTrainerDid.visibility = View.GONE
        binding.buttonForPersonalTrainer.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_home_to_navigation_auto_schedule)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
