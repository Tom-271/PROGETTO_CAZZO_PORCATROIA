package com.example.progetto_tosa.ui.account

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.progetto_tosa.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore

class LoginWithEmail : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var flLogin: FrameLayout
    private lateinit var pbLoading: ProgressBar
    private lateinit var tvGoRegister: TextView

    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_with_email)

        // bind delle view
        etEmail      = findViewById(R.id.etEmailLog)
        etPassword   = findViewById(R.id.etPasswordLog)
        btnLogin     = findViewById(R.id.btnLogin)
        flLogin      = findViewById(R.id.flLoginButton)
        pbLoading    = findViewById(R.id.pbLoading)
        tvGoRegister = findViewById(R.id.tvGoRegister)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val pw    = etPassword.text.toString()
            if (email.isEmpty() || pw.isEmpty()) {
                Toast.makeText(this, "Inserisci email e password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Disabilita il bottone e mostra loader
            btnLogin.isEnabled   = false
            btnLogin.text        = ""
            pbLoading.visibility = View.VISIBLE
            pbLoading.alpha      = 0f
            pbLoading.animate().alpha(1f).setDuration(200).start()

            // Effettua il login
            auth.signInWithEmailAndPassword(email, pw)
                .addOnCompleteListener { task ->
                    // Ripristina UI
                    pbLoading.animate()
                        .alpha(0f)
                        .setDuration(200)
                        .withEndAction {
                            pbLoading.visibility = View.GONE
                            btnLogin.text        = "ACCEDI"
                            btnLogin.isEnabled   = true
                        }
                        .start()

                    if (task.isSuccessful) {
                        // Animazione di conferma
                        btnLogin.setBackgroundColor(
                            ContextCompat.getColor(this, R.color.teal_700)
                        )
                        btnLogin.animate()
                            .scaleX(1.05f).scaleY(1.05f).setDuration(150)
                            .withEndAction {
                                btnLogin.animate()
                                    .scaleX(1f).scaleY(1f)
                                    .setDuration(100)
                                    .withEndAction { val intent = Intent(this, MainActivity::class.java)
                                        intent.putExtra("navigateTo", "account")
                                        startActivity(intent)
                                        finish() }
                                    .start()
                            }
                            .start()
                    } else {
                        // Messaggio di errore specifico
                        val msg = when (task.exception) {
                            is FirebaseAuthInvalidUserException       -> "Utente non trovato"
                            is FirebaseAuthInvalidCredentialsException -> "Email o Password errata"
                            else                                       -> task.exception?.localizedMessage ?: "Login fallito"
                        }
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                    }
                }
        }

        tvGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}
