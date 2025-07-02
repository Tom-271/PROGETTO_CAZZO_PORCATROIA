package com.example.progetto_tosa.ui.home

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.core.os.bundleOf
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.progetto_tosa.R
import com.example.progetto_tosa.databinding.FragmentHomeBinding
import com.example.progetto_tosa.ui.account.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()

    companion object {
        private const val TAG = "HomeFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Data odierna
        val today    = Date()
        val displayF = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
        binding.bannerDate.text = displayF.format(today)

        // ID data in Firestore: "yyyy-MM-dd"
        val keyF    = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayId = keyF.format(today)
        Log.d(TAG, "TodayId = $todayId")

        // Chi è loggato?
        val user = auth.currentUser
        if (user == null) {
            // nessuno loggato: tutto nascosto
            binding.bannerStatus.visibility = View.GONE
            setAllGone()
            return
        }

        // Leggo la flag is_trainer dalle prefs
        val prefs     = requireActivity()
            .getSharedPreferences("user_data", Context.MODE_PRIVATE)
        val isTrainer = prefs.getBoolean("is_trainer", false)
        if (isTrainer) {
            // se è PT non mostro la sezione stato e mostro i bottoni PT
            binding.bannerStatus.visibility = View.GONE
            showButtonsForPT()
            return
        }

        // se arrivo qui: è un atleta → mostro sezione stato e bottoni user
        binding.bannerCard.visibility = View.VISIBLE
        binding.bannerStatus.text       = "Controllo in corso…"
        showButtonsForUser()

        // Recupero displayName salvato in prefs (come in AccountFragment)
        val savedName   = prefs.getString("saved_display_name", null)
        val fullNameRaw = savedName.takeUnless { it.isNullOrBlank() }
            ?: user.displayName.orEmpty()
        val fullName    = fullNameRaw.trim()
        if (fullName.isBlank()) {
            Log.e(TAG, "Nome completo utente mancante")
            binding.bannerStatus.text = "Errore: nome utente non disponibile"
            return
        }
        Log.d(TAG, "Verifico schede_del_pt/$fullName/$todayId")

        // Query Firestore: schede_del_pt/{fullName}/{todayId}
        db.collection("schede_del_pt")
            .document(fullName)
            .collection(todayId)
            .limit(1)
            .get()
            .addOnSuccessListener { snap ->
                binding.bannerStatus.text = if (snap.isEmpty)
                    "Oggi giornata libera!"
                else
                    "Hai una nuova scheda caricata dal tuo PT"
                Log.d(TAG, "Firestore response: isEmpty=${snap.isEmpty}")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Errore nella query schede_del_pt/$fullName/$todayId", e)
                binding.bannerStatus.text = "Errore controllo scheda"
            }
    }

    private fun setAllGone() {
        binding.buttonForTheScheduleIDid.visibility               = View.GONE
        binding.buttonForTheSchedulePersonalTrainerDid.visibility = View.GONE
        binding.buttonForPersonalTrainer.visibility               = View.GONE
        binding.buttonInutile.visibility                           = View.VISIBLE
        binding.buttonInutile.strokeColor = ColorStateList.valueOf(
            ContextCompat.getColor(requireContext(), R.color.orange)
        )
        binding.buttonInutile.text = "Effettua il login per accedere"
        binding.buttonInutile.setOnClickListener {
            startActivity(Intent(requireContext(), LoginActivity::class.java))
        }
    }

    private fun showButtonsForUser() {
        binding.buttonForPersonalTrainer.visibility = View.GONE
        binding.buttonForTheScheduleIDid.visibility = View.VISIBLE
        binding.buttonForTheSchedulePersonalTrainerDid.visibility = View.VISIBLE

        // 1) Bottone “Scopri gli esercizi”
        binding.buttonForTheScheduleIDid.setOnClickListener {
            findNavController().navigate(
                R.id.action_navigation_home_to_navigation_myautocalendar
            )
        }

        // 2) Bottone “Scopri il tuo piano” → apri PTcalendar **passando** selectedUser
        binding.buttonForTheSchedulePersonalTrainerDid.setOnClickListener {
            // recupera nome completo da prefs (come fai già in HomeFragment)
            val prefs = requireActivity()
                .getSharedPreferences("user_data", Context.MODE_PRIVATE)
            val fullName = prefs.getString("saved_display_name", "")
                ?: ""

            // se è vuoto, fai un fallback (magari uid) o un toast
            if (fullName.isBlank()) {
                Toast.makeText(requireContext(),
                    "Nome utente non disponibile", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // nav con bundle
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
