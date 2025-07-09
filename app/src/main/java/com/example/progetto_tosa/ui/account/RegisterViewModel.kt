package com.example.progetto_tosa.ui.account

import android.app.Application
import androidx.lifecycle.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class RegisterViewModel(application: Application) : AndroidViewModel(application) {

    // --- Input fields ---
    val firstName        = MutableLiveData<String>()
    val lastName         = MutableLiveData<String>()
    val email            = MutableLiveData<String>()
    val password         = MutableLiveData<String>()
    val birthDate        = MutableLiveData<String>()
    val weight           = MutableLiveData<String>()
    val height           = MutableLiveData<String>()
    val bodyFat          = MutableLiveData<String>()
    val verificationCode = MutableLiveData<String>()
    val isTrainer        = MutableLiveData(false)

    // --- UI state & events ---
    val isLoading                 = MutableLiveData(false)
    private val _errorMessage     = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _showDatePicker        = MutableLiveData<Unit>()
    val showDatePickerEvent: LiveData<Unit> = _showDatePicker

    private val _registrationSuccess   = MutableLiveData<Unit>()
    val registrationSuccessEvent: LiveData<Unit> = _registrationSuccess

    // Firebase
    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()
    private val dateParser = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    /** Chiamato dal layout quando si clicca sul campo data */
    fun onBirthDateClicked() {
        _showDatePicker.value = Unit
    }

    /** Lancia la procedura di registrazione */
    fun onRegister() {
        val fn    = firstName.value.orEmpty().trim()
        val ln    = lastName.value.orEmpty().trim()
        val em    = email.value.orEmpty().trim()
        val pw    = password.value.orEmpty()
        val bdStr = birthDate.value.orEmpty().trim()
        val wtStr = weight.value.orEmpty().trim()
        val htStr = height.value.orEmpty().trim()
        val bfStr = bodyFat.value.orEmpty().trim()
        val vCode = verificationCode.value.orEmpty().trim()
        val pt    = isTrainer.value == true

        // **Controllo: nome e cognome solo lettere**
        val nameRegex = Regex("^[A-Za-zÀ-ÖØ-öø-ÿ]+$")
        if (!nameRegex.matches(fn) || !nameRegex.matches(ln)) {
            _errorMessage.value = "Nome e cognome devono contenere solo lettere"
            return
        }

        // Base validation
        if (fn.isEmpty() || ln.isEmpty() || em.isEmpty()
            || pw.length < 6 || bdStr.isEmpty()
            || wtStr.isEmpty() || htStr.isEmpty() || bfStr.isEmpty()
        ) {
            _errorMessage.value = "Compila tutti i campi e password minimo 6 caratteri"
            return
        }

        // Password strength
        val pwRegex = Regex("^(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{6,}\$")
        if (!pwRegex.matches(pw)) {
            _errorMessage.value = "La password deve avere almeno una maiuscola, un numero e un carattere speciale"
            return
        }

        // Trainer code
        if (pt) {
            val official = "00000"
            if (vCode != official) {
                _errorMessage.value = "Codice non valido, contatta l'amministratore"
                return
            }
        }

        viewModelScope.launch {
            isLoading.value = true
            _errorMessage.value = null
            try {
                // 1) create auth user
                val authResult = auth.createUserWithEmailAndPassword(em, pw).await()
                val uid = authResult.user?.uid
                    ?: throw Exception("UID non disponibile")

                // 2) parse birthDate in Date
                val bdDate = dateParser.parse(bdStr)
                    ?: throw Exception("Formato data errato")

                // 3) prepare Firestore data
                val data = mapOf(
                    "firstName"         to fn,
                    "lastName"          to ln,
                    "email"             to em,
                    "birthday"          to bdDate,
                    "weight"            to wtStr.toDouble(),
                    "height"            to htStr.toInt(),
                    "bodyFat"           to bfStr.toDouble(),
                    "isPersonalTrainer" to pt
                )
                val collection = if (pt) "personal_trainers" else "users"
                db.collection(collection)
                    .document(uid)
                    .set(data)
                    .await()

                _registrationSuccess.value = Unit

            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Errore durante registrazione"
            } finally {
                isLoading.value = false
            }
        }
    }
    fun errorMessageHandled() {
        _errorMessage.value = null
    }
}
