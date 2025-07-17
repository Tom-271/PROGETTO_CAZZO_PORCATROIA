package com.example.progetto_tosa.ui.account

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.progetto_tosa.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Allievi : Fragment(R.layout.fragment_allievi) {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val listView = view.findViewById<ListView>(R.id.listViewUsers)
        val currentUser = auth.currentUser

        if (currentUser == null) {
            Toast.makeText(requireContext(), "Devi effettuare il login", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        // 1. Prima cerca nella collection "users" per ottenere nome e cognome
        db.collection("users")
            .get()
            .addOnSuccessListener { usersSnapshot ->
                val usersMap = usersSnapshot.documents.associate { doc ->
                    doc.id to Pair(
                        doc.getString("firstName").orEmpty(),
                        doc.getString("lastName").orEmpty()
                    )
                }

                // 2. Ora cerca gli atleti del PT corrente
                val ptCollectionName = "atleti_di_${currentUser.uid}"
                db.collection(ptCollectionName)
                    .get()
                    .addOnSuccessListener { athletesSnapshot ->
                        val items = athletesSnapshot.documents.mapNotNull { athleteDoc ->
                            val userId = athleteDoc.id
                            val (firstName, lastName) = usersMap[userId] ?: Pair("", "")
                            "$firstName $lastName".trim().takeIf { it.isNotEmpty() }
                        }

                        if (items.isEmpty()) {
                            Toast.makeText(requireContext(),
                                "Non ci sono ancora atleti che si allenano con te",
                                Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }

                        val adapter = ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_list_item_1,
                            items
                        )
                        listView.adapter = adapter

                        listView.setOnItemClickListener { _, _, position, _ ->
                            val selected = items[position]
                            val bundle = Bundle().apply {
                                putString("selectedUser", selected)
                            }
                            findNavController().navigate(
                                R.id.action_navigation_allievi_to_navigation_ptSchedule,
                                bundle
                            )
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(),
                            "Errore nel caricamento degli atleti: ${e.message}",
                            Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(),
                    "Errore nel caricamento degli utenti: ${e.message}",
                    Toast.LENGTH_SHORT).show()
            }
    }
}