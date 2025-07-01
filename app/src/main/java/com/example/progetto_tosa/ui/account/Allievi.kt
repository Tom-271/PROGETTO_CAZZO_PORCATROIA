package com.example.progetto_tosa.ui.account

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.fragment.app.Fragment

import androidx.navigation.fragment.findNavController
import com.example.progetto_tosa.R
import com.google.firebase.firestore.FirebaseFirestore

class Allievi : Fragment(R.layout.fragment_allievi) {

    private val db = FirebaseFirestore.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val listView = view.findViewById<ListView>(R.id.listViewUsers)

        db.collection("users")
            .get()
            .addOnSuccessListener { snap ->
                val items = snap.documents.map { doc ->
                    val first = doc.getString("firstName").orEmpty()
                    val last  = doc.getString("lastName").orEmpty()
                    "$first $last".trim()
                }
                if (items.isEmpty()) {
                    Toast.makeText(requireContext(), "Nessun utente trovato", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_list_item_1,
                    items
                )
                listView.adapter = adapter

                // *** QUI: click sull’item ***
                listView.setOnItemClickListener { _, _, position, _ ->
                    val selected = items[position]
                    val bundle = Bundle().apply {
                        putString("selectedUser", selected)
                    }
                    findNavController()
                        .navigate(R.id.action_navigation_allievi_to_navigation_ptSchedule, bundle)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(),
                    "Errore caricamento: ${e.message}",
                    Toast.LENGTH_SHORT).show()
            }
    }

}
