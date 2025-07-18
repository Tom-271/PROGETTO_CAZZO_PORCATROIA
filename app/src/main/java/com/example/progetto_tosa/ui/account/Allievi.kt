package com.example.progetto_tosa.ui.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.progetto_tosa.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class Allievi : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_allievi, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewUsers).apply {
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Devi effettuare il login", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        db.collection("users").get()
            .addOnSuccessListener { usersSnap ->
                val usersMap = usersSnap.documents.associate { doc ->
                    val bd = doc.getDate("birthday")?.let { dateFormat.format(it) }.orEmpty()
                    val ht = doc.getLong("height")?.toInt()?.let { "$it cm" }.orEmpty()
                    val wt = doc.getDouble("weight")?.let { "${it} kg" }.orEmpty()
                    doc.id to User(
                        uid = doc.id,
                        nickname = doc.getString("nickname").orEmpty(),
                        firstName = doc.getString("firstName").orEmpty(),
                        lastName = doc.getString("lastName").orEmpty(),
                        email = doc.getString("email").orEmpty(),
                        birthday = bd,
                        height = ht,
                        weight = wt
                    )
                }
                db.collection("atleti_di_${currentUser.uid}").get()
                    .addOnSuccessListener { athSnap ->
                        val list = athSnap.documents.mapNotNull { usersMap[it.id] }
                        recyclerView.adapter = AthletesAdapter(list) { athlete ->
                            val bundle = Bundle().apply { putString("selectedUser", "${athlete.firstName} ${athlete.lastName}") }
                            findNavController().navigate(
                                R.id.action_navigation_allievi_to_navigation_ptSchedule, bundle
                            )
                        }
                    }
                    .addOnFailureListener { e -> Toast.makeText(requireContext(), "Errore: ${e.message}", Toast.LENGTH_SHORT).show() }
            }
            .addOnFailureListener { e -> Toast.makeText(requireContext(), "Errore: ${e.message}", Toast.LENGTH_SHORT).show() }
    }

    data class User(
        val uid: String,
        val nickname: String,
        val firstName: String,
        val lastName: String,
        val email: String,
        val birthday: String,
        val height: String,
        val weight: String
    )

    class AthletesAdapter(
        private val athletes: List<User>,
        private val onNameClick: (User) -> Unit
    ) : RecyclerView.Adapter<AthletesAdapter.VH>() {

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            private val tvTitle: TextView = view.findViewById(R.id.tvAthleteTitle)
            private val btnToggle: ImageButton = view.findViewById(R.id.btnToggle)
            private val llDetails: LinearLayout = view.findViewById(R.id.llDetails)
            private val tvEmail: TextView = view.findViewById(R.id.tvEmail)
            private val tvBirth: TextView = view.findViewById(R.id.tvBirth)
            private val tvHeight: TextView = view.findViewById(R.id.tvHeight)
            private val tvWeight: TextView = view.findViewById(R.id.tvWeight)

            fun bind(u: User) {
                tvTitle.text = "${u.nickname} - ${u.firstName} ${u.lastName}"
                tvTitle.setOnClickListener { onNameClick(u) }
                tvEmail.text = "Email: ${u.email}"
                tvBirth.text = "Data di nascita: ${u.birthday}"
                tvHeight.text = "Altezza: ${u.height}"
                tvWeight.text = "Peso: ${u.weight}"

                btnToggle.setOnClickListener {
                    if (llDetails.visibility == View.VISIBLE) {
                        llDetails.visibility = View.GONE
                        btnToggle.setImageResource(R.drawable.down)
                    } else {
                        llDetails.visibility = View.VISIBLE
                        btnToggle.setImageResource(R.drawable.up)
                    }
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_allievo, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(athletes[position])
        override fun getItemCount() = athletes.size
    }
}