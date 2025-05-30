package com.example.progetto_tosa.ui.account

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.progetto_tosa.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class RegisterActivity : AppCompatActivity() {

    private lateinit var etFirstName: EditText
    private lateinit var etLastName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etBirthDate: EditText
    private lateinit var etWeight: EditText
    private lateinit var etHeight: EditText
    private lateinit var etBodyFat: EditText
    private lateinit var etCodiceVerifica: EditText
    private lateinit var btnSubmit: MaterialButton
    private lateinit var rgChoices: RadioGroup
    private lateinit var btnSi: RadioButton
    private lateinit var btnNo: RadioButton

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // find views
        etFirstName = findViewById(R.id.etFirstName)
        etLastName = findViewById(R.id.etLastName)
        etEmail = findViewById(R.id.etEmailReg)
        etPassword = findViewById(R.id.etPasswordReg)
        etBirthDate = findViewById(R.id.etBirthDate)
        etWeight = findViewById(R.id.etWeight)
        etHeight = findViewById(R.id.etHeight)
        etBodyFat = findViewById(R.id.etBodyFat)
        etCodiceVerifica = findViewById(R.id.etCodiceVerifica)
        rgChoices = findViewById(R.id.rgChoices)
        btnSi = findViewById(R.id.rbSI)
        btnNo = findViewById(R.id.rbNO)
        btnSubmit = findViewById(R.id.btnRegisterSubmit)

        // date picker
        etBirthDate.apply {
            isFocusable = false
            isClickable = true
            setOnClickListener { showDatePicker() }
        }

        // hide codice verifica field initially
        etCodiceVerifica.visibility = View.GONE

        // toggle visibility based on radio selection
        rgChoices.setOnCheckedChangeListener { _, checkedId ->
            etCodiceVerifica.visibility = if (checkedId == R.id.rbSI) View.VISIBLE else View.GONE
        }

        btnSubmit.setOnClickListener {
            val fn = etFirstName.text.toString().trim()
            val ln = etLastName.text.toString().trim()
            val em = etEmail.text.toString().trim()
            val pw = etPassword.text.toString()
            val bd = etBirthDate.text.toString().trim()
            val wt = etWeight.text.toString().trim()
            val ht = etHeight.text.toString().trim()
            val bf = etBodyFat.text.toString().trim()
            val verifica = etCodiceVerifica.text.toString().trim()
            val isPT = btnSi.isChecked
            val codiceUff = "00000"


            // basic validation
            if (fn.isEmpty() || ln.isEmpty() || em.isEmpty() ||
                pw.length < 6 || bd.isEmpty() ||
                wt.isEmpty() || ht.isEmpty() || bf.isEmpty()
            ) {
                Toast.makeText(this, "Compila tutti i campi e password minimo 6 caratteri", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // verify codice if PT
            if (isPT) {
                if (verifica.isEmpty()) {
                    Toast.makeText(this, "Inserisci codice di verifica", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                if (verifica != codiceUff) {
                    Toast.makeText(this, "Codicenon valido, se non lo ricordi/possiedi contatta l'Amministratore", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
            }

            // create user
            auth.createUserWithEmailAndPassword(em, pw)
                .addOnSuccessListener { res ->
                    val uid = res.user?.uid ?: return@addOnSuccessListener
                    val collectionName = if (isPT) "personal_trainers" else "users"

                    // prepare data
                    val parts = bd.split("/")
                    calendar.set(parts[2].toInt(), parts[1].toInt() - 1, parts[0].toInt())
                    val age = calculateAge(calendar)
                    val data = hashMapOf(
                        "firstName" to fn,
                        "lastName" to ln,
                        "email" to em,
                        "birthday" to calendar.time,
                        "age" to age,
                        "weight" to wt.toDouble(),
                        "height" to ht.toInt(),
                        "bodyFat" to bf.toDouble(),
                        "isPersonalTrainer" to isPT
                    )

                    db.collection(collectionName).document(uid)
                        .set(data)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Registrazione avvenuta con successo!", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Errore salvataggio: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Errore creazione account: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun showDatePicker() {
        val y = calendar.get(Calendar.YEAR)
        val m = calendar.get(Calendar.MONTH)
        val d = calendar.get(Calendar.DAY_OF_MONTH)
        DatePickerDialog(this, { _, yy, mm, dd ->
            calendar.set(yy, mm, dd)
            etBirthDate.setText(dateFormat.format(calendar.time))
        }, y, m, d).show()
    }

    private fun calculateAge(birthDate: Calendar): Int {
        val today = Calendar.getInstance()
        var age = today.get(Calendar.YEAR) - birthDate.get(Calendar.YEAR)
        if (today.get(Calendar.DAY_OF_YEAR) < birthDate.get(Calendar.DAY_OF_YEAR)) age--
        return age
    }
}
