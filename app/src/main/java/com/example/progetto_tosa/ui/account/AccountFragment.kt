package com.example.progetto_tosa.ui.account

import android.animation.ValueAnimator
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
import com.facebook.shimmer.Shimmer
import com.facebook.shimmer.ShimmerFrameLayout
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

class AccountFragment : Fragment() {

    private var _binding: FragmentAccountBinding? = null
    private val binding get() = _binding!!

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db   by lazy { FirebaseFirestore.getInstance() }

    private lateinit var shimmer: Shimmer
    private lateinit var shimmerLayout: ShimmerFrameLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ─── THEME SWITCH VIA SharedPreferences ────────────────────────────────

        val prefs = requireActivity()
            .getSharedPreferences("settings", Context.MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("darkMode", true)

        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )

        binding.switch1.isChecked = isDarkMode
        binding.switch1.text = if (isDarkMode)
            "Disable dark mode" else "Enable dark mode"

        binding.switch1.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("darkMode", isChecked).apply()
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
            binding.switch1.text = if (isChecked)
                "Disable dark mode" else "Enable dark mode"
        }

        // ─── SHIMMER SETUP ──────────────────────────────────────────────────────

        shimmerLayout = binding.shimmerLogin
        shimmer = Shimmer.AlphaHighlightBuilder()
            .setDuration(5000L)
            .setBaseAlpha(1f)
            .setHighlightAlpha(0.6f)
            .setDirection(Shimmer.Direction.LEFT_TO_RIGHT)
            .setRepeatCount(ValueAnimator.INFINITE)
            .build()
        shimmerLayout.setShimmer(shimmer)

        // ─── BUTTONS & NAVIGATION ──────────────────────────────────────────────

        binding.ButtonLogin.setOnClickListener {
            startActivity(Intent(requireContext(), LoginActivity::class.java))
        }
        binding.signOut.setOnClickListener {
            AuthUI.getInstance().signOut(requireContext())
                .addOnCompleteListener { updateUI() }
        }
        binding.UserData.setOnClickListener {
            findNavController().navigate(R.id.action_account_to_UserData)
        }

        // ─── INITIAL UI LOAD ───────────────────────────────────────────────────

        updateUI()
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun updateUI() {
        val user = auth.currentUser
        if (user != null) {
            shimmerLayout.stopShimmer()
            shimmerLayout.visibility = View.GONE
            binding.signOut.visibility     = View.VISIBLE
            binding.TrainerProgram.visibility = View.VISIBLE

            // carica e colora in bindUserData…
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

        } else {
            // Ospite
            shimmerLayout.startShimmer()
            shimmerLayout.visibility   = View.VISIBLE

            binding.signOut.visibility     = View.GONE
            binding.TrainerProgram.visibility = View.GONE   // <-- NASCONDI QUI

            // nascondi i dettagli…
            listOf(
                binding.tvFirstLast,
                binding.tvEmail,
                binding.tvBirthday,
                binding.tvAge,
                binding.tvWeight,
                binding.tvHeight,
                binding.tvPTStatus1,
                binding.tvPTStatus2
            ).forEach { it.visibility = View.GONE }

            // e ripristina colori di default
            val defaultStroke = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.sky))
            binding.iconaUtente.strokeColor = defaultStroke
        }
    }


    private fun bindUserData(doc: DocumentSnapshot) {
        val fn    = doc.getString("firstName").orEmpty()
        val ln    = doc.getString("lastName").orEmpty()
        val email = doc.getString("email")
            ?: auth.currentUser?.email.orEmpty()

        binding.NomeUtente.text =
            if (fn.isNotBlank()) "$fn $ln" else email

        // Nascondo i dettagli extra
        listOf(
            binding.tvFirstLast,
            binding.tvEmail,
            binding.tvBirthday,
            binding.tvAge,
            binding.tvWeight,
            binding.tvHeight
        ).forEach { it.visibility = View.GONE }

        // Se è PT: mostro il TextView e metto il bottone verde
        // fuori dall'if, così sky è visibile in entrambi i rami
        val green = ContextCompat.getColor(requireContext(), R.color.green)
        val sky   = ContextCompat.getColor(requireContext(), R.color.sky)

        if (doc.getBoolean("isPersonalTrainer") == true) {
            binding.tvPTStatus1.visibility = View.VISIBLE
            binding.tvPTStatus2.visibility = View.VISIBLE

            // tinting per il personal trainer
            binding.signOut.backgroundTintList = ColorStateList.valueOf(green)
            binding.signOut.setTextColor(Color.BLACK)
            binding.iconaUtente.strokeColor  = ColorStateList.valueOf(green)
        } else {
            // ripristina sky dove serve
            binding.iconaUtente.strokeColor       = ColorStateList.valueOf(sky)
            binding.signOut.backgroundTintList    = ColorStateList.valueOf(sky)
            binding.signOut.setTextColor(Color.WHITE)  // o quello che preferisci
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::shimmerLayout.isInitialized) shimmerLayout.stopShimmer()
        _binding = null
    }
}
