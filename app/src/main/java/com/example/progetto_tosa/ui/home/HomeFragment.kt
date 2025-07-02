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

        // 1) Data
        val today      = Date()
        val displayFmt = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
        val keyFmt     = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val displayDate= displayFmt.format(today)
        val todayId    = keyFmt.format(today)
        binding.bannerDate.text = displayDate
        Log.d(TAG, "DisplayDate=$displayDate, TodayId=$todayId")

        // 2) Login check
        val user = auth.currentUser
        if (user == null) {
            Log.d(TAG, "Nessun utente loggato")
            binding.bannerStatus.visibility = View.GONE
            setAllGone()
            return
        }
        Log.d(TAG, "User UID=${user.uid}")

        // 3) Ruolo PT/Atleta
        val prefs     = requireActivity().getSharedPreferences("user_data", Context.MODE_PRIVATE)
        val isTrainer = prefs.getBoolean("is_trainer", false)
        Log.d(TAG, "isTrainer flag=$isTrainer")
        if (isTrainer) {
            Log.d(TAG, "Accesso come Personal Trainer")
            binding.bannerStatus.visibility = View.GONE
            showButtonsForPT()
            return
        }

        // 4) Atleta UI
        binding.bannerCard.visibility = View.VISIBLE
        binding.bannerStatus.text     = "Controllo in corso…"
        showButtonsForUser()

        // 5) Recupera saved_display_name
        val fullName = prefs.getString("saved_display_name","").orEmpty()
        Log.d(TAG, "saved_display_name='$fullName'")
        if (fullName.isBlank()) {
            Log.e(TAG, "Nome utente mancante dalle prefs")
            binding.bannerStatus.text = "Errore controllo scheda"
            return
        }

        // 6) DEBUG: logga tutti gli ID in schede_del_pt
        db.collection("schede_del_pt")
            .get()
            .addOnSuccessListener { col ->
                Log.d(TAG, "Documenti in schede_del_pt:")
                col.documents.forEach { Log.d(TAG, " • '${it.id}'") }

                // Trova doc con match case‐INSENSITIVE
                val match = col.documents.find { it.id.equals(fullName, ignoreCase = true) }
                if (match == null) {
                    Log.d(TAG, "❌ Nessun documento match per '$fullName'")
                    binding.bannerStatus.text = "Oggi giornata libera!"
                } else {
                    val docId = match.id
                    Log.d(TAG, "✅ Trovato docId='$docId', ora controllo subcollezione $todayId")

                    // 7) Controlla sub-collezione oggi
                    db.collection("schede_del_pt")
                        .document(docId)
                        .collection(todayId)
                        .limit(1)
                        .get()
                        .addOnSuccessListener { snap ->
                            if (snap.isEmpty) {
                                Log.d(TAG, "⛔ Subcollezione '$todayId' vuota per '$docId'")
                                binding.bannerStatus.text = "Oggi giornata libera!"
                            } else {
                                Log.d(TAG, "✔️ Subcollezione '$todayId' contiene ${snap.size()} doc")
                                binding.bannerStatus.text = "Hai una nuova scheda caricata dal tuo PT"
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Errore controllo subcollezione '$todayId'", e)
                            binding.bannerStatus.text = "Errore controllo scheda"
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Errore lettura collection schede_del_pt", e)
                binding.bannerStatus.text = "Errore controllo scheda"
            }
    }

    private fun setAllGone() {
        binding.buttonForTheScheduleIDid.visibility               = View.GONE
        binding.buttonForTheSchedulePersonalTrainerDid.visibility = View.GONE
        binding.buttonForPersonalTrainer.visibility               = View.GONE
        binding.buttonInutile.visibility                          = View.VISIBLE
        binding.buttonInutile.strokeColor = ColorStateList.valueOf(
            ContextCompat.getColor(requireContext(), R.color.orange)
        )
        binding.buttonInutile.text = "Effettua il login per accedere"
        binding.buttonInutile.setOnClickListener {
            startActivity(Intent(requireContext(), LoginActivity::class.java))
        }
    }

    private fun showButtonsForUser() {
        val prefs = requireActivity().getSharedPreferences("user_data", Context.MODE_PRIVATE)
        val fullName = prefs.getString("saved_display_name","").orEmpty()
        Log.d(TAG, "showButtonsForUser: fullName='$fullName'")

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
