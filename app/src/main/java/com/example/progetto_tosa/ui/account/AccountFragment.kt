package com.example.progetto_tosa.ui.account

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

        // applica tema salvato
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
        // mostra solo il pulsante corrispondente
        updateThemeButtons(isDarkMode)

        // click sul pulsante SOLE (passa a dark mode)
        binding.btnLightMode.setOnClickListener {
            if (!isDarkMode) {
                isDarkMode = true
                prefs.edit().putBoolean("darkMode", true).apply()
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                updateThemeButtons(isDarkMode)
            }
        }

        // click sul pulsante LUNA (passa a light mode)
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
            AuthUI.getInstance().signOut(requireContext())
                .addOnCompleteListener { updateUI() }
        }
        binding.userData.setOnClickListener {
            findNavController().navigate(R.id.action_account_to_UserData)
        }

        // ─── INITIAL UI LOAD ───────────────────────────────────────────────────
        updateUI()
    }

    private fun updateThemeButtons(isDark: Boolean) {
        if (isDark) {
            // se sono in dark mode, mostro il pulsante luna e nascondo quello sole
            binding.btnDarkMode.visibility = View.VISIBLE
            binding.btnLightMode.visibility = View.GONE
        } else {
            // se sono in light mode, mostro il pulsante sole e nascondo quello luna
            binding.btnLightMode.visibility = View.VISIBLE
            binding.btnDarkMode.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun updateUI() {
        val user = auth.currentUser
        if (user != null) {
            // Utente loggato
            binding.ButtonLogin.visibility    = View.GONE
            binding.signOut.visibility        = View.VISIBLE

            val uid = user.uid
            db.collection("users").document(uid)
                .get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) bindUserData(doc)
                    else db.collection("personal_trainers").document(uid)
                        .get()
                        .addOnSuccessListener { ptDoc ->
                            if (ptDoc.exists()) bindUserData(ptDoc)
                            else binding.NomeUtente.text = "Utente sconosciuto"
                        }
                }
                .addOnFailureListener {
                    binding.TrainerProgram.visibility = View.GONE
                }

        } else {
            // Ospite
            binding.ButtonLogin.visibility    = View.VISIBLE
            binding.signOut.visibility        = View.GONE
            binding.TrainerProgram.visibility = View.GONE
            binding.ruolo.text = "accedi per ulteriori specifiche"

            listOf(
                binding.tvFirstLast,
                binding.tvEmail,
                binding.tvBirthday,
                binding.tvAge,
                binding.tvWeight,
                binding.tvHeight
            ).forEach { it.visibility = View.GONE }

            // bordo arancione default per icona utente
            binding.iconaUtente.strokeColor = ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.orange)
            )
        }
    }

    private fun bindUserData(doc: DocumentSnapshot) {
        val fn    = doc.getString("firstName").orEmpty()
        val ln    = doc.getString("lastName").orEmpty()
        val email = doc.getString("email") ?: auth.currentUser?.email.orEmpty()
        binding.NomeUtente.text =
            if (fn.isNotBlank()) "$fn $ln" else email

        listOf(
            binding.tvFirstLast,
            binding.tvEmail,
            binding.tvBirthday,
            binding.tvAge,
            binding.tvWeight,
            binding.tvHeight
        ).forEach { it.visibility = View.GONE }

        val isPT = doc.getBoolean("isPersonalTrainer") == true
        if (isPT) {
            binding.TrainerProgram.visibility = View.VISIBLE
            val green = ContextCompat.getColor(requireContext(), R.color.green)
            binding.signOut.backgroundTintList = ColorStateList.valueOf(green)
            binding.signOut.setTextColor(Color.BLACK)
            binding.iconaUtente.strokeColor    = ColorStateList.valueOf(green)
            binding.ruolo.text = "Personal Trainer"
        } else {
            binding.TrainerProgram.visibility = View.GONE
            val sky = ContextCompat.getColor(requireContext(), R.color.sky)
            binding.signOut.backgroundTintList = ColorStateList.valueOf(sky)
            binding.signOut.setTextColor(Color.WHITE)
            binding.iconaUtente.strokeColor    = ColorStateList.valueOf(sky)
            binding.ruolo.text = "Atleta"

        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
