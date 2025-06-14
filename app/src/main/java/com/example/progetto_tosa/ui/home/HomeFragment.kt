package com.example.progetto_tosa.ui.home

import android.R
import android.os.Bundle
import android.view.LayoutInflater
import android.content.Intent
import com.example.progetto_tosa.ui.account.LoginActivity
import android.view.View
import androidx.core.content.ContextCompat
import android.content.res.ColorStateList
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.progetto_tosa.databinding.FragmentHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

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

        val user = auth.currentUser
        if (user == null) {
            setAllGone()
            return
        }

        val uid = user.uid

        // 🔍 Verifica se è un utente normale
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    showButtonsForUser()        //chiamiamo la funzione per mostrare i bottoni dell'utente normale
                } else {
                    // 🔍 Se non è un utente, verifica se è un PT
                    db.collection("personal_trainers").document(uid).get()
                        .addOnSuccessListener { ptDoc ->
                            if (ptDoc.exists()) {
                                showButtonsForPT()
                            } else {
                                setAllGone()
                            }
                        }
                        .addOnFailureListener {
                            setAllGone()
                        }
                }
            }
            .addOnFailureListener {
                setAllGone()
            }
    }

    private fun setAllGone() {
        binding.buttonForTheScheduleIDid.visibility = View.GONE
        binding.buttonForTheSchedulePersonalTrainerDid.visibility = View.GONE
        binding.buttonForPersonalTrainer.visibility = View.GONE
        binding.buttonInutile.visibility = View.VISIBLE
        binding.buttonInutile.strokeColor = ColorStateList.valueOf(
            ContextCompat.getColor(requireContext(), R.color.holo_orange_dark)
        )
        binding.buttonInutile.text = "Per accedere al servizio, effettua il login"

        // 🔁 click per tornare alla login
        binding.buttonInutile.setOnClickListener {
            val intent = Intent(requireContext(), LoginActivity::class.java)
            startActivity(intent)
        }
    }

    private fun showButtonsForUser() {
        binding.buttonForPersonalTrainer.visibility = View.GONE
        binding.buttonForTheScheduleIDid.visibility = View.VISIBLE
        binding.buttonForTheSchedulePersonalTrainerDid.visibility = View.VISIBLE

        binding.buttonForTheScheduleIDid.text = "Le mie schede"
        binding.buttonForTheSchedulePersonalTrainerDid.text = "Le schede del mio PT"
    }

    private fun showButtonsForPT() {
        binding.buttonForPersonalTrainer.visibility = View.VISIBLE
        binding.buttonForTheScheduleIDid.visibility = View.GONE
        binding.buttonForTheSchedulePersonalTrainerDid.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
