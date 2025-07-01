package com.example.progetto_tosa.ui.account

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.content.edit
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
    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireActivity().getSharedPreferences("settings", Context.MODE_PRIVATE)
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

        binding.settings.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_account_to_navigation_settings)
        }

        binding.UserJourney.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_account_to_navigation_allievi)
        }

        binding.userData.setOnClickListener {
            findNavController().navigate(R.id.action_account_to_UserData)
        }

        binding.UserProgram.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_account_to_navigation_home)
        }

        preloadUserDataFromPreferences() // 👈 mostro subito i dati salvati
        updateUI()
    }

    override fun onResume() {
        super.onResume()
        preloadUserDataFromPreferences()
        updateUI()
    }

    private fun updateThemeButtons(isDark: Boolean) {
        binding.btnDarkMode.visibility = if (isDark) View.VISIBLE else View.GONE
        binding.btnLightMode.visibility = if (!isDark) View.VISIBLE else View.GONE
    }

    private fun preloadUserDataFromPreferences() {
        val prefs = requireActivity().getSharedPreferences("user_data", Context.MODE_PRIVATE)
        val savedName = prefs.getString("saved_display_name", null)
        val isTrainer = prefs.getBoolean("is_trainer", false)
        val user = FirebaseAuth.getInstance().currentUser

        if (!savedName.isNullOrBlank()) {
            binding.NomeUtente.text = savedName

            if (isTrainer) {
                binding.ruolo.text = "Personal Trainer"
                binding.iconaUtente.setImageResource(R.drawable.personal)
                binding.iconaUtente.strokeColor = ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.green)
                )
                binding.NomeUtente.setTextColor(
                    ContextCompat.getColorStateList(requireContext(), R.color.perNomePersonal)
                )
            } else {
                binding.ruolo.text = "Atleta"
                binding.iconaUtente.setImageResource(R.drawable.atleta)
                binding.iconaUtente.strokeColor = ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.orange)
                )
                binding.NomeUtente.setTextColor(
                    ContextCompat.getColorStateList(requireContext(), R.color.perNomeAtleta)
                )
            }
        } else {
            binding.NomeUtente.text = "Effettua il login"
            binding.ruolo.text = ""
            binding.iconaUtente.setImageResource(R.drawable.account_principal)
        }
    }


    private fun updateUI() {
        val user = auth.currentUser

        if (user == null) {
            // utente non loggato → mostro dati salvati
            applySavedUserDataOrFallback()
            return
        }

        val uid = user.uid

        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    bindUserData(doc)
                } else {
                    db.collection("personal_trainers").document(uid)
                        .get()
                        .addOnSuccessListener { ptDoc ->
                            if (ptDoc.exists()) {
                                bindUserData(ptDoc)
                            } else {
                                applySavedUserDataOrFallback()
                            }
                        }
                }
            }
            .addOnFailureListener {
                applySavedUserDataOrFallback()
            }
    }



    private fun applySavedUserDataOrFallback() {
        val prefs = requireActivity().getSharedPreferences("user_data", Context.MODE_PRIVATE)
        val savedName = prefs.getString("saved_display_name", null)
        val isTrainer = prefs.getBoolean("is_trainer", false)

        if (!savedName.isNullOrBlank()) {
            binding.NomeUtente.text = savedName

            if (isTrainer) {
                binding.TrainerProgram.visibility = View.VISIBLE
                val green = ContextCompat.getColor(requireContext(), R.color.green)
                binding.iconaUtente.strokeColor = ColorStateList.valueOf(green)
                binding.ruolo.text = "Personal Trainer"
                binding.iconaUtente.setImageResource(R.drawable.personal)
                binding.NomeUtente.setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.perNomePersonal))
            } else {
                binding.TrainerProgram.visibility = View.GONE
                val orange = ContextCompat.getColor(requireContext(), R.color.orange)
                binding.iconaUtente.strokeColor = ColorStateList.valueOf(orange)
                binding.ruolo.text = "Atleta"
                binding.iconaUtente.setImageResource(R.drawable.atleta)
                binding.NomeUtente.setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.perNomeAtleta))
            }

        } else {
            binding.NomeUtente.text = "Utente sconosciuto"
            binding.iconaUtente.setImageResource(R.drawable.account_principal)
            binding.ruolo.text = ""
        }
    }

    private fun bindUserData(doc: DocumentSnapshot) {
        val name = doc.getString("firstName").orEmpty()
        val surname = doc.getString("lastName").orEmpty()
        val email = doc.getString("email") ?: auth.currentUser?.email.orEmpty()
        val displayName = if (name.isNotBlank()) "$name $surname" else email

        binding.NomeUtente.text = displayName
        requireActivity().getSharedPreferences("user_data", Context.MODE_PRIVATE).edit {
            putString("saved_display_name", displayName)
            putBoolean("is_trainer", doc.getBoolean("isPersonalTrainer") == true)
        }

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
            binding.iconaUtente.strokeColor = ColorStateList.valueOf(green)
            binding.ruolo.text = "Personal Trainer"
            binding.iconaUtente.setImageResource(R.drawable.personal)
            binding.NomeUtente.setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.perNomePersonal))
        } else {
            binding.TrainerProgram.visibility = View.GONE
            val orange = ContextCompat.getColor(requireContext(), R.color.orange)
            binding.iconaUtente.strokeColor = ColorStateList.valueOf(orange)
            binding.ruolo.text = "Atleta"
            binding.iconaUtente.setImageResource(R.drawable.atleta)
            binding.NomeUtente.setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.perNomeAtleta))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
