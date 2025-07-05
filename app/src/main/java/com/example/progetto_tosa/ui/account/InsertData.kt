package com.example.progetto_tosa.ui.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.progetto_tosa.databinding.FragmentInsertDataBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class InsertData : Fragment() {

    private var _binding: FragmentInsertDataBinding? = null
    private val binding get() = _binding!!

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db   by lazy { FirebaseFirestore.getInstance() }

    private var isEditing = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInsertDataBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonSave.setOnClickListener {
            val user = auth.currentUser
            if (user == null) {
                Toast.makeText(requireContext(),
                    "Devi essere loggato per salvare i dati",
                    Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isEditing) {
                // Passa in modalità modifica
                toggleEditing(true)
                return@setOnClickListener
            }

            // Sei in modifica: raccogli i valori
            val fn = binding.editTextFirstName.text.toString().trim()
            val ln = binding.editTextLastName.text.toString().trim()
            val em = binding.editTextEmail.text.toString().trim()
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val birth = try {
                sdf.parse(binding.editTextBirthDate.text.toString())
            } catch (e: Exception) { null }
            val weight = binding.editTextWeight.text.toString().toDoubleOrNull()
            val height = binding.editTextHeight.text.toString().toIntOrNull()
            val bf     = binding.editTextBF.text.toString().toDoubleOrNull()

            if (fn.isEmpty() || ln.isEmpty() || em.isEmpty()
                || weight == null || height == null || bf == null
            ) {
                Toast.makeText(requireContext(),
                    "Compila tutti i campi correttamente",
                    Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val data = mapOf(
                "firstName" to fn,
                "lastName"  to ln,
                "email"     to em,
                "birthday"  to birth,
                "weight"    to weight,
                "height"    to height,
                "bodyFat"   to bf
            )

            // **Ora usiamo update() per modificare i campi esistenti**
            db.collection("users")
                .document(user.uid)
                .update(data)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(),
                        "Dati aggiornati con successo", Toast.LENGTH_SHORT).show()
                    toggleEditing(false)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(),
                        "Errore aggiornamento: ${e.message}",
                        Toast.LENGTH_LONG).show()
                }
        }

        // Popola subito i campi da Firestore
        updateUI()
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun updateUI() {
        val user = auth.currentUser
        if (user == null) {
            toggleEditing(false)
            return
        }

        db.collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { doc ->
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

                doc.getString("firstName")?.let {
                    binding.editTextFirstName.setText(it)
                }
                doc.getString("lastName")?.let {
                    binding.editTextLastName.setText(it)
                }
                doc.getString("email")?.let {
                    binding.editTextEmail.setText(it)
                }
                doc.getTimestamp("birthday")?.toDate()?.let {
                    binding.editTextBirthDate.setText(sdf.format(it))
                }
                doc.getDouble("weight")?.let {
                    binding.editTextWeight.setText(it.toString())
                }
                doc.getLong("height")?.toInt()?.let {
                    binding.editTextHeight.setText(it.toString())
                }
                doc.getDouble("bodyFat")?.let {
                    binding.editTextBF.setText(it.toString())
                }

                // Una volta caricati, blocca la modifica
                toggleEditing(false)
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(),
                    "Errore caricamento dati: ${e.message}",
                    Toast.LENGTH_LONG).show()
            }
    }

    private fun toggleEditing(enabled: Boolean) {
        isEditing = enabled
        binding.editTextFirstName.isEnabled = enabled
        binding.editTextLastName .isEnabled = enabled
        binding.editTextEmail    .isEnabled = enabled
        binding.editTextBirthDate.isEnabled = enabled
        binding.editTextWeight   .isEnabled = enabled
        binding.editTextHeight   .isEnabled = enabled
        binding.editTextBF       .isEnabled = enabled
        binding.buttonSave.text = if (enabled) "Salva" else "Modifica"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
