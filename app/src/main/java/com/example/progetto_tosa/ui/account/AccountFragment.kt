package com.example.progetto_tosa.ui.account

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.progetto_tosa.R
import com.example.progetto_tosa.databinding.FragmentAccountBinding
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

class AccountFragment : Fragment() {

    companion object {
        private const val TAG = "AccountFragment"
    }

    private var _binding: FragmentAccountBinding? = null
    private val binding get() = _binding!!

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db   by lazy { FirebaseFirestore.getInstance() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ─── THEME TOGGLE VIA SharedPreferences ──────────────────────────────
        val prefs = requireActivity()
            .getSharedPreferences("settings", Context.MODE_PRIVATE)
        var isDarkMode = prefs.getBoolean("darkMode", true)
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
        updateThemeButtons(isDarkMode)
        binding.btnLightMode.setOnClickListener {
            if (!isDarkMode) {
                isDarkMode = true
                prefs.edit().putBoolean("darkMode", true).apply()
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                updateThemeButtons(isDarkMode)
            }
        }
        binding.btnDarkMode.setOnClickListener {
            if (isDarkMode) {
                isDarkMode = false
                prefs.edit().putBoolean("darkMode", false).apply()
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                updateThemeButtons(isDarkMode)
            }
        }

        // ─── BUTTONS & NAVIGATION ──────────────────────────────────────────────
        binding.ButtonLogin.setOnClickListener {
            startActivity(Intent(requireContext(), LoginActivity::class.java))
        }
        binding.signOut.setOnClickListener {
            Toast.makeText(
                requireContext(),
                "Ci vediamo al prossimo allenamento!",
                Toast.LENGTH_SHORT
            ).show()
            AuthUI.getInstance().signOut(requireContext())
                .addOnCompleteListener { updateUI() }
        }
        binding.userData.setOnClickListener {
            findNavController().navigate(R.id.action_account_to_UserData)
        }

        updateUI()
    }

    private fun updateThemeButtons(isDark: Boolean) {
        binding.btnDarkMode.visibility  = if (isDark) View.VISIBLE else View.GONE
        binding.btnLightMode.visibility = if (isDark) View.GONE    else View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun updateUI() {
        val user = auth.currentUser
        if (user != null) {
            binding.ButtonLogin.visibility    = View.GONE
            binding.signOut.visibility        = View.VISIBLE

            val uid = user.uid
            db.collection("users").document(uid)
                .get()
                .addOnSuccessListener { doc ->
                    Log.d(TAG, "Firestore users/$uid → ${doc.data}")
                    if (doc.exists()) {
                        bindUserData(doc)
                    } else {
                        // fallback personal trainer
                        db.collection("personal_trainers").document(uid)
                            .get()
                            .addOnSuccessListener { ptDoc ->
                                Log.d(TAG, "Firestore personal_trainers/$uid → ${ptDoc.data}")
                                if (ptDoc.exists()) bindUserData(ptDoc)
                                else binding.NomeUtente.text = "Utente sconosciuto"
                            }
                            .addOnFailureListener {
                                Log.e(TAG, "PT fetch error", it)
                                binding.NomeUtente.text = "Errore caricamento dati"
                            }
                    }
                }
                .addOnFailureListener {
                    Log.e(TAG, "users fetch error", it)
                    Toast.makeText(
                        requireContext(),
                        "Errore di rete: riprova più tardi",
                        Toast.LENGTH_SHORT
                    ).show()
                }

        } else {
            // reset UI per utente non loggato
            binding.ButtonLogin.visibility    = View.VISIBLE
            binding.signOut.visibility        = View.GONE
            binding.TrainerProgram.visibility = View.GONE
            binding.ruolo.text = "Accedi per ulteriori dettagli"

            listOf(
                binding.tvFirstLast,
                binding.tvEmail,
                binding.tvWeight,
                binding.tvHeight
            ).forEach { it.visibility = View.GONE }

            binding.NomeUtente.text = "NOME UTENTE"
            binding.NomeUtente.setTextColor(
                ContextCompat.getColorStateList(requireContext(), R.color.sky)
            )
            binding.iconaUtente.strokeColor = ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.sky)
            )
            binding.iconaUtente.setImageResource(R.drawable.account_principal)
        }
    }

    private fun bindUserData(doc: DocumentSnapshot) {
        // CAMPO ITALIANO
        val nome    = doc.getString("nome").orEmpty()
        val cognome = doc.getString("cognome").orEmpty()
        val mail    = doc.getString("mail") ?: auth.currentUser?.email.orEmpty()
        val peso    = doc.getDouble("peso") ?: 0.0
        val altezza = doc.getDouble("altezza") ?: 0.0

        // intestazione
        binding.NomeUtente.text =
            if (nome.isNotBlank()) "$nome $cognome" else mail

        // popolo
        binding.tvFirstLast.apply {
            text = if (nome.isNotBlank()) "$nome $cognome" else "—"
            visibility = View.VISIBLE
        }
        binding.tvEmail.apply {
            text = mail
            visibility = View.VISIBLE
        }
        binding.tvWeight.apply {
            text = if (peso > 0) "${"%.1f".format(peso)} kg" else "—"
            visibility = View.VISIBLE
        }
        binding.tvHeight.apply {
            text = if (altezza > 0) "${"%.1f".format(altezza)} cm" else "—"
            visibility = View.VISIBLE
        }

        // styling
        if (doc.getBoolean("isPersonalTrainer") == true) applyTrainerStyle()
        else applyAthleteStyle()
    }

    private fun applyTrainerStyle() {
        binding.TrainerProgram.visibility = View.VISIBLE
        val green = ContextCompat.getColor(requireContext(), R.color.green)
        binding.signOut.backgroundTintList = ColorStateList.valueOf(green)
        binding.signOut.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
        binding.iconaUtente.strokeColor    = ColorStateList.valueOf(green)
        binding.ruolo.text                 = "Personal Trainer"
        binding.iconaUtente.setImageResource(R.drawable.personal)
        binding.NomeUtente.setTextColor(
            ContextCompat.getColorStateList(requireContext(), R.color.perNomePersonal)
        )
    }

    private fun applyAthleteStyle() {
        binding.TrainerProgram.visibility = View.GONE
        val orange = ContextCompat.getColor(requireContext(), R.color.orange)
        binding.signOut.backgroundTintList = ColorStateList.valueOf(orange)
        binding.signOut.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        binding.iconaUtente.strokeColor    = ColorStateList.valueOf(orange)
        binding.ruolo.text                 = "Atleta"
        binding.iconaUtente.setImageResource(R.drawable.atleta)
        binding.NomeUtente.setTextColor(
            ContextCompat.getColorStateList(requireContext(), R.color.perNomeAtleta)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
