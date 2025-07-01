package com.example.progetto_tosa.ui.account

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.progetto_tosa.R
import com.example.progetto_tosa.databinding.FragmentSettingsBinding
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // mostra/nascondi pulsanti
        updateLoginLogoutButtons()

        // login
        binding.ButtonLogin.setOnClickListener {
            startActivity(Intent(requireContext(), LoginActivity::class.java))
        }

        // logout
        binding.signOut.setOnClickListener {
            Toast.makeText(requireContext(), "Ci vediamo al prossimo allenamento!", Toast.LENGTH_SHORT).show()
            AuthUI.getInstance().signOut(requireContext()).addOnCompleteListener {
                clearSavedUserData()
                updateLoginLogoutButtons()
            }
        }
    }

    private fun updateLoginLogoutButtons() {
        val isLoggedIn = auth.currentUser != null
        binding.ButtonLogin.visibility = if (isLoggedIn) View.GONE else View.VISIBLE
        binding.signOut.visibility = if (isLoggedIn) View.VISIBLE else View.GONE
    }

    private fun clearSavedUserData() {
        requireActivity()
            .getSharedPreferences("user_data", Context.MODE_PRIVATE)
            .edit()
            .remove("saved_display_name")
            .remove("is_trainer")
            .apply()
    }

    override fun onResume() {
        super.onResume()
        updateLoginLogoutButtons()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
