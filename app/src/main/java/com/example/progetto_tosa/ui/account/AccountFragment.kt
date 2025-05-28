package com.example.progetto_tosa.ui.account

import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.progetto_tosa.R
import com.example.progetto_tosa.databinding.FragmentAccountBinding
import com.facebook.shimmer.Shimmer
import com.facebook.shimmer.ShimmerFrameLayout
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AccountFragment : Fragment() {

    private var _binding: FragmentAccountBinding? = null
    private val binding get() = _binding!!
    private val settingsViewModel: SettingsViewModel by activityViewModels()

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db   by lazy { FirebaseFirestore.getInstance() }

    // Shimmer e layout
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

        // Riferimento al ShimmerFrameLayout
        shimmerLayout = binding.shimmerLogin

        // 1) Costruzione del shimmer
        shimmer = Shimmer.AlphaHighlightBuilder()
            .setDuration(5000L)               // ciclo di 5s come nel CSS
            .setBaseAlpha(1f)                 // opacità di base del pulsante
            .setHighlightAlpha(0.6f)          // opacità dell'area di luce
            .setDirection(Shimmer.Direction.LEFT_TO_RIGHT)
            .setRepeatCount(ValueAnimator.INFINITE)
            .build()

        // 2) Assegna il shimmer al layout
        shimmerLayout.setShimmer(shimmer)

        // Pulsanti e navigazioni
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

        // Dark mode per il testo
        settingsViewModel.isDarkMode.observe(viewLifecycleOwner) { isDark ->
            binding.NomeUtente.setTextColor(
                if (isDark) Color.WHITE else Color.BLACK
            )
        }
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun updateUI() {
        val user = auth.currentUser
        if (user != null) {
            // Utente loggato:
            shimmerLayout.stopShimmer()
            shimmerLayout.visibility = View.GONE
            binding.signOut.visibility = View.VISIBLE

            // Carica dati Firestore
            db.collection("users").document(user.uid)
                .get()
                .addOnSuccessListener { doc ->
                    val fn = doc.getString("firstName") ?: ""
                    val ln = doc.getString("lastName")  ?: ""
                    val email = doc.getString("email") ?: user.email
                    binding.NomeUtente.text =
                        if (fn.isNotBlank()) "$fn $ln" else email
                }

            // Nascondi campi extra
            listOf(
                binding.tvFirstLast,
                binding.tvEmail,
                binding.tvBirthday,
                binding.tvAge,
                binding.tvWeight,
                binding.tvHeight
            ).forEach { it.visibility = View.GONE }

        } else {
            // Ospite:
            binding.NomeUtente.text        = "Ospite"
            binding.signOut.visibility     = View.GONE
            shimmerLayout.visibility       = View.VISIBLE
            shimmerLayout.startShimmer()

            // Nascondi campi extra
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
        // Ferma shimmer per sicurezza
        if (::shimmerLayout.isInitialized) {
            shimmerLayout.stopShimmer()
        }
        _binding = null
    }
}
