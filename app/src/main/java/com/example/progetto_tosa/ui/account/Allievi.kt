package com.example.progetto_tosa.ui.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
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
import java.util.Locale

class Allievi : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_allievi, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recycler = view.findViewById<RecyclerView>(R.id.recyclerViewUsers).apply {
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            setHasFixedSize(false)
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
                    val bf = doc.getDouble("bodyFat")
                    doc.id to User(
                        uid = doc.id,
                        nickname = doc.getString("nickname").orEmpty(),
                        firstName = doc.getString("firstName").orEmpty(),
                        lastName = doc.getString("lastName").orEmpty(),
                        email = doc.getString("email").orEmpty(),
                        birthday = bd,
                        height = ht,
                        weight = wt,
                        bodyFat = bf,
                        targetFat = doc.getDouble("targetFatMass"),
                        targetLean = doc.getDouble("targetLeanMass")
                    )
                }

                val colName = "atleti_di_${currentUser.uid}"
                db.collection(colName).get()
                    .addOnSuccessListener { athSnap ->
                        val list = athSnap.documents.mapNotNull { usersMap[it.id] }
                        if (list.isEmpty()) {
                            Toast.makeText(requireContext(), "Non hai ancora atleti.", Toast.LENGTH_SHORT).show()
                        }
                        recycler.adapter = AthletesAdapter(
                            db = db,
                            athletes = list,
                            onAthleteClick = { user ->
                                val bundle = Bundle().apply {
                                    putString("selectedUser", "${user.firstName} ${user.lastName}")
                                    putString("athleteUid", user.uid)
                                }
                                findNavController().navigate(
                                    R.id.action_navigation_allievi_to_navigation_ptSchedule,
                                    bundle
                                )
                            }
                        )
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Errore atleti: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Errore utenti: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    data class User(
        val uid: String,
        val nickname: String,
        val firstName: String,
        val lastName: String,
        val email: String,
        val birthday: String,
        val height: String,
        val weight: String,
        val bodyFat: Double?,
        val targetFat: Double?,
        val targetLean: Double?
    )

    class AthletesAdapter(
        private val db: FirebaseFirestore,
        private val athletes: List<User>,
        private val onAthleteClick: (User) -> Unit
    ) : RecyclerView.Adapter<AthletesAdapter.VH>() {

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            private val tvTitle: TextView = view.findViewById(R.id.tvAthleteTitle)
            private val btnToggle: ImageButton = view.findViewById(R.id.btnToggle)
            private val llDetails: LinearLayout = view.findViewById(R.id.llDetails)
            private val tvEmail: TextView = view.findViewById(R.id.tvEmail)
            private val tvBirth: TextView = view.findViewById(R.id.tvBirth)
            private val tvHeight: TextView = view.findViewById(R.id.tvHeight)
            private val tvWeight: TextView = view.findViewById(R.id.tvWeight)
            private val tvBodyFat: TextView = view.findViewById(R.id.tvBodyFat)
            private val etTargetFat: EditText = view.findViewById(R.id.etTargetFat)
            private val etTargetLean: EditText = view.findViewById(R.id.etTargetLean)
            private val btnSave: View = view.findViewById(R.id.btnSaveTargets)

            private var originalFat: String? = null
            private var originalLean: String? = null
            private var currentUid: String? = null

            fun bind(u: User) {
                currentUid = u.uid
                val fullName = "${u.firstName} ${u.lastName}".trim()
                tvTitle.text = "${u.nickname} - $fullName".trim().replace(Regex("^ - "), "")
                tvEmail.text = "Email: ${u.email}"
                tvBirth.text = "Data di nascita: ${u.birthday}"
                tvHeight.text = "Altezza: ${u.height}"
                tvWeight.text = "Peso attuale: ${u.weight}"
                tvBodyFat.text = "Body Fat attuale: ${u.bodyFat?.let { "${it} %" } ?: "-"}"

                tvTitle.setOnClickListener { onAthleteClick(u) }
                itemView.setOnClickListener { onAthleteClick(u) }

                val fatText = u.targetFat?.toString() ?: ""
                val leanText = u.targetLean?.toString() ?: ""
                if (etTargetFat.text.toString() != fatText) etTargetFat.setText(fatText)
                if (etTargetLean.text.toString() != leanText) etTargetLean.setText(leanText)
                originalFat = fatText
                originalLean = leanText

                makeEditable(etTargetFat)
                makeEditable(etTargetLean)
                btnSave.visibility = View.VISIBLE
                btnSave.isEnabled = true

                btnSave.setOnClickListener { save(u) }

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

            private fun makeEditable(et: EditText) {
                et.isEnabled = true
                et.isFocusable = true
                et.isFocusableInTouchMode = true
                et.isCursorVisible = true
            }

            private fun save(u: User) {
                if (currentUid != u.uid) return

                val fatStr = etTargetFat.text.toString().trim()
                val leanStr = etTargetLean.text.toString().trim()

                if (fatStr == originalFat && leanStr == originalLean) {
                    Toast.makeText(itemView.context, "Nessuna modifica", Toast.LENGTH_SHORT).show()
                    return
                }

                val fatVal = fatStr.ifEmpty { null }?.toDoubleOrNull()
                val leanVal = leanStr.ifEmpty { null }?.toDoubleOrNull()

                if (fatStr.isNotEmpty() && (fatVal == null || fatVal < 0 || fatVal > 60)) {
                    etTargetFat.error = "0-60"
                    return
                }
                if (leanStr.isNotEmpty() && (leanVal == null || leanVal <= 0 || leanVal > 250)) {
                    etTargetLean.error = "1-250"
                    return
                }

                btnSave.isEnabled = false

                db.collection("users").document(u.uid)
                    .update(
                        mapOf(
                            "targetFatMass" to fatVal,
                            "targetWeight" to leanVal
                        )
                    )
                    .addOnSuccessListener {
                        originalFat = fatStr
                        originalLean = leanStr
                        Toast.makeText(itemView.context, "Obiettivi salvati", Toast.LENGTH_SHORT).show()
                        btnSave.isEnabled = true
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(itemView.context, "Errore: ${e.message}", Toast.LENGTH_SHORT).show()
                        btnSave.isEnabled = true
                    }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_allievo, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(athletes[position])
        override fun getItemCount(): Int = athletes.size
    }
}
