package com.example.progetto_tosa.ui.progression

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.progetto_tosa.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProgressionFragment : Fragment(R.layout.fragment_progression) {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private lateinit var tvWeightGoalValue: TextView
    private lateinit var tvBodyFatGoalValue: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvWeightGoalValue = view.findViewById(R.id.tvWeightGoalValue)
        tvBodyFatGoalValue = view.findViewById(R.id.tvBodyFatGoalValue)

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Devi effettuare il login", Toast.LENGTH_SHORT).show()
            return
        }

        // Carica il documento dell'utente
        db.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { snap ->

                val targetLean = snap.getDouble("targetLeanMass")
                val targetFat = snap.getDouble("targetFatMass")

                tvWeightGoalValue.text = targetLean?.let { String.format("%.1f kg", it) } ?: "—"
                tvBodyFatGoalValue.text = targetFat?.let { String.format("%.1f %%", it) } ?: "—"
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Errore: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
