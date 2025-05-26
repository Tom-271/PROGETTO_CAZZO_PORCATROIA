package com.example.progetto_tosa.ui.account

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.progetto_tosa.R
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvGoRegister: TextView

    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // bind delle view
        etEmail       = findViewById(R.id.etEmailLog)
        etPassword    = findViewById(R.id.etPasswordLog)
        btnLogin      = findViewById(R.id.btnLogin)
        tvGoRegister  = findViewById(R.id.tvGoRegister)

        // click sul pulsante Accedi
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val pw    = etPassword.text.toString()
            if (email.isEmpty() || pw.isEmpty()) {
                Toast.makeText(this, "Inserisci email e password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            auth.signInWithEmailAndPassword(email, pw)
                .addOnSuccessListener {
                    // login OK: chiudo l'activity e torno ad AccountFragment
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this,
                        "Login fallito: ${e.message}",
                        Toast.LENGTH_LONG).show()
                }
        }

        // click sul link Registrati
        tvGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}
