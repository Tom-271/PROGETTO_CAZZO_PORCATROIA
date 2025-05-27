package com.example.progetto_tosa.ui.account

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.progetto_tosa.R
import com.example.progetto_tosa.databinding.FragmentAccountBinding
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AccountFragment : Fragment() {

    private var _binding: FragmentAccountBinding? = null
    private val binding get() = _binding!!

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db   by lazy { FirebaseFirestore.getInstance() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- TEMA ---
        val prefs = requireActivity().getSharedPreferences("settings", 0)
        val isDarkMode = prefs.getBoolean("darkMode", true)
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
        binding.switch1.isChecked = isDarkMode
        binding.switch1.text = if (isDarkMode) "Disable dark mode" else "Enable dark mode"
        val textColor = if (isDarkMode)
            resources.getColor(android.R.color.white, null)
        else
            resources.getColor(android.R.color.black, null)
        binding.NomeUtente.setTextColor(textColor)
        binding.switch1.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("darkMode", isChecked).apply()
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
            binding.switch1.text = if (isChecked) "Disable dark mode" else "Enable dark mode"
        }

        // --- Pulsante Login ---
        binding.ButtonLogin.setOnClickListener {
            startActivity(Intent(requireContext(), LoginActivity::class.java))
        }

        // --- Pulsante Logout ---
        binding.signOut.setOnClickListener {
            AuthUI.getInstance()
                .signOut(requireContext())
                .addOnCompleteListener {
                    updateUI()
                }
        }

        // --- Navigazione UserData ---
        binding.UserData.setOnClickListener {
            findNavController().navigate(R.id.action_account_to_UserData)
        }
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun updateUI() {
        val user = auth.currentUser
        if (user != null) {
            // Leggi il documento Firestore
            db.collection("users").document(user.uid)
                .get()
                .addOnSuccessListener { doc ->
                    // Recupera i singoli campi
                    val fn = doc.getString("firstName") ?: ""
                    val ln = doc.getString("lastName") ?: ""
                    val email = doc.getString("email") ?: user.email
                    val birthday = doc.getTimestamp("birthday")?.toDate()
                    val age = doc.getLong("age")?.toInt()
                    val weight = doc.getDouble("weight")
                    val height = doc.getLong("height")?.toInt()

                    // Popola le TextView
                    binding.NomeUtente.text = if (fn.isNotBlank()) "$fn $ln" else email
                    binding.tvFirstLast.visibility = View.GONE

                }
            binding.signOut.visibility      = View.VISIBLE
            binding.ButtonLogin.visibility  = View.GONE
        } else {
            // utente non loggato
            binding.NomeUtente.text         = "Ospite"
            binding.signOut.visibility      = View.GONE
            binding.ButtonLogin.visibility  = View.VISIBLE

            // nascondi i campi extra
            listOf(
                binding.tvFirstLast,
                binding.tvEmail,
                binding.tvBirthday,
                binding.tvAge,
                binding.tvWeight,
                binding.tvHeight
            ).forEach { it.visibility = View.GONE }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
