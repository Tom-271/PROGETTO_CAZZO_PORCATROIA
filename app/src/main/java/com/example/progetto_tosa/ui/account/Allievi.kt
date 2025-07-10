// Fragment per mostrare la lista di tutti gli utenti registrati
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

// Definizione del Fragment che utilizza il layout fragment_allievi
class Allievi : Fragment(R.layout.fragment_allievi) {

    // Istanziamento dell'oggetto Firestore per operazioni sul database
    private val db = FirebaseFirestore.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Recupero del ListView dal layout
        val listView = view.findViewById<ListView>(R.id.listViewUsers)

        // Query per ottenere tutti i documenti nella collezione "users"
        db.collection("users")
            .get()
            .addOnSuccessListener { snap ->
                // Mappo i documenti ottenuti in una lista di nomi completi
                val items = snap.documents.map { doc ->
                    val first = doc.getString("firstName").orEmpty()
                    val last  = doc.getString("lastName").orEmpty()
                    // Concateno nome e cognome, rimuovendo eventuali spazi vuoti
                    "$first $last".trim()
                }
                // Se non ci sono utenti, mostro un messaggio
                if (items.isEmpty()) {
                    Toast.makeText(requireContext(), "Nessun utente trovato", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                // Creo e assegno un ArrayAdapter per popolare il ListView
                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_list_item_1,
                    items
                )
                listView.adapter = adapter

                // Imposto il comportamento al click su un item della lista
                listView.setOnItemClickListener { _, _, position, _ ->
                    // Recupero l'utente selezionato in base alla posizione
                    val selected = items[position]
                    // Preparo un Bundle con il nome utente selezionato
                    val bundle = Bundle().apply {
                        putString("selectedUser", selected)
                    }
                    // Navigo verso il fragment di programmazione PT passando il Bundle
                    findNavController()
                        .navigate(R.id.action_navigation_allievi_to_navigation_ptSchedule, bundle)
                }
            }
            .addOnFailureListener { e ->
                // Gestione degli errori nella lettura dei dati: mostro un Toast con il messaggio di errore
                Toast.makeText(requireContext(),
                    "Errore caricamento: ${e.message}",
                    Toast.LENGTH_SHORT).show()
            }
    }
}
