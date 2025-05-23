package com.example.progetto_tosa.ui.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.example.progetto_tosa.databinding.FragmentAccountBinding
import com.example.progetto_tosa.R

class AccountFragment : Fragment() {

    private var _binding: FragmentAccountBinding? = null
    private val binding get() = _binding!!

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

        val prefs = requireActivity().getSharedPreferences("settings", 0)
        val isDarkMode = prefs.getBoolean("darkMode", true) // true = default: dark mode

        // imposta il tema all'avvio
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )

        // aggiorna lo stato dello switch e il testo
        binding.switch1.isChecked = isDarkMode
        binding.switch1.text = if (isDarkMode) "Disable dark mode" else "Enable dark mode"

        // cambia il colore della TextView in base al tema
        val textColor = if (isDarkMode) {
            resources.getColor(android.R.color.white, null)
        } else {
            resources.getColor(android.R.color.black, null)
        }
        binding.NomeUtente.setTextColor(textColor)

        // listener sullo switch per cambiare tema e salvare preferenza
        binding.switch1.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("darkMode", isChecked).apply()
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
            binding.switch1.text = if (isChecked) "Disable dark mode" else "Enable dark mode"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
