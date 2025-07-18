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
    val nickname         = MutableLiveData<String>()
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

    fun onBirthDateClicked() {
        _showDatePicker.value = Unit
    }

    fun onRegister() {
        val fn    = firstName.value.orEmpty().trim()
        val ln    = lastName.value.orEmpty().trim()
        val nk    = nickname.value.orEmpty().trim()
        val em    = email.value.orEmpty().trim()
        val pw    = password.value.orEmpty()
        val bdStr = birthDate.value.orEmpty().trim()
        val wtStr = weight.value.orEmpty().trim()
        val htStr = height.value.orEmpty().trim()
        val bfStr = bodyFat.value.orEmpty().trim()
        val vCode = verificationCode.value.orEmpty().trim()
        val pt    = isTrainer.value == true

        // Validazioni di base
        val nameRegex = Regex("^[A-Za-zÀ-ÖØ-öø-ÿ]+$")
        if (!nameRegex.matches(fn) || !nameRegex.matches(ln)) {
            _errorMessage.value = "Nome e cognome devono contenere solo lettere"
            return
        }
        if (fn.isEmpty() || ln.isEmpty() || nk.isEmpty() || em.isEmpty()
            || pw.length < 6 || bdStr.isEmpty()
            || wtStr.isEmpty() || htStr.isEmpty() || bfStr.isEmpty()
        ) {
            _errorMessage.value = "Compila tutti i campi (incluso nickname) e password minimo 6 caratteri"
            return
        }
        val pwRegex = Regex("^(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{6,}\$")
        if (!pwRegex.matches(pw)) {
            _errorMessage.value = "La password deve avere almeno una maiuscola, un numero e un carattere speciale"
            return
        }
        if (pt && vCode != "00000") {
            _errorMessage.value = "Codice trainer non valido"
            return
        }

        viewModelScope.launch {
            isLoading.value = true
            _errorMessage.value = null
            try {
                // 1) Creo l’utente in Firebase Auth (ora sono autenticato)
                val authResult = auth.createUserWithEmailAndPassword(em, pw).await()
                val uid = authResult.user?.uid ?: throw Exception("UID non disponibile")

                // 2) Verifico duplicati di nickname
                val usersWithSameNick = db.collection("users")
                    .whereEqualTo("nickname", nk).get().await()
                val trainersWithSameNick = db.collection("personal_trainers")
                    .whereEqualTo("nickname", nk).get().await()

                if (!usersWithSameNick.isEmpty || !trainersWithSameNick.isEmpty) {
                    _errorMessage.value = "Nickname già in uso"
                    return@launch
                }

                // 3) Parse data di nascita
                val bdDate = dateParser.parse(bdStr)
                    ?: throw Exception("Formato data errato")

                // 4) Preparo dati per Firestore
                val data = mapOf(
                    "firstName"         to fn,
                    "lastName"          to ln,
                    "nickname"          to nk,
                    "email"             to em,
                    "birthday"          to bdDate,
                    "weight"            to wtStr.toDouble(),
                    "height"            to htStr.toInt(),
                    "bodyFat"           to bfStr.toDouble(),
                    "isPersonalTrainer" to pt
                )
                val collection = if (pt) "personal_trainers" else "users"

                // 5) Salvo i dati in Firestore
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
