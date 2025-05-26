package com.example.progetto_tosa.ui.account

import android.os.Bundle
import android.widget.Button
import android.widget.DatePicker
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.progetto_tosa.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class RegisterActivity : AppCompatActivity() {

    private lateinit var etFirstName: EditText
    private lateinit var etLastName:  EditText
    private lateinit var etEmail:     EditText
    private lateinit var etPassword:  EditText
    private lateinit var datePicker:  DatePicker
    private lateinit var etWeight:    EditText
    private lateinit var etHeight:    EditText
    private lateinit var btnSubmit:   Button

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db   by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Inizializza le view
        etFirstName = findViewById(R.id.etFirstName)
        etLastName  = findViewById(R.id.etLastName)
        etEmail     = findViewById(R.id.etEmailReg)
        etPassword  = findViewById(R.id.etPasswordReg)
        datePicker  = findViewById(R.id.datePickerReg)
        etWeight    = findViewById(R.id.etWeight)
        etHeight    = findViewById(R.id.etHeight)
        btnSubmit   = findViewById(R.id.btnRegisterSubmit)

        btnSubmit.setOnClickListener {
            val firstName = etFirstName.text.toString().trim()
            val lastName  = etLastName.text.toString().trim()
            val email     = etEmail.text.toString().trim()
            val password  = etPassword.text.toString()
            val weightStr = etWeight.text.toString().trim()
            val heightStr = etHeight.text.toString().trim()

            // Validazione campi base
            if (firstName.isEmpty() || lastName.isEmpty() ||
                email.isEmpty() || password.length < 6) {
                Toast.makeText(
                    this,
                    "Completa tutti i campi e usa una password di almeno 6 caratteri",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // Crea utente in Firebase Auth
            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val uid = result.user?.uid ?: return@addOnSuccessListener

                    // Calcola età dalla data di nascita
                    val calendar = Calendar.getInstance().apply {
                        set(
                            datePicker.year,
                            datePicker.month,
                            datePicker.dayOfMonth
                        )
                    }
                    val age = calculateAge(calendar)

                    // Prepara dati da salvare
                    val userData = hashMapOf(
                        "firstName" to firstName,
                        "lastName"  to lastName,
                        "email"     to email,
                        "birthday"  to calendar.time,
                        "age"       to age,
                        "weight"    to weightStr.toDoubleOrNull(),
                        "height"    to heightStr.toIntOrNull()
                    )

                    // Salvataggio su Firestore
                    db.collection("users")
                        .document(uid)
                        .set(userData)
                        .addOnSuccessListener {
                            Toast.makeText(
                                this,
                                "Registrazione avvenuta con successo!",
                                Toast.LENGTH_SHORT
                            ).show()
                            finish() // Chiudi l'activity e torna alla LoginActivity
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(
                                this,
                                "Errore salvataggio dati: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        this,
                        "Errore creazione account: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }

    /**
     * Calcola l'età sulla base della data di nascita fornita
     */
    private fun calculateAge(birthDate: Calendar): Int {
        val today = Calendar.getInstance()
        var age = today.get(Calendar.YEAR) - birthDate.get(Calendar.YEAR)
        if (today.get(Calendar.DAY_OF_YEAR) < birthDate.get(Calendar.DAY_OF_YEAR)) {
            age--
        }
        return age
    }
}