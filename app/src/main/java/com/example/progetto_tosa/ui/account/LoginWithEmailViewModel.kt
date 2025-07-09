package com.example.progetto_tosa.ui.account

import android.app.Application
import androidx.lifecycle.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

class LoginWithEmailViewModel(application: Application) : AndroidViewModel(application) {

    val email = MutableLiveData("")
    val password = MutableLiveData("")

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isSignedIn = MutableLiveData(false)
    val isSignedIn: LiveData<Boolean> = _isSignedIn

    private val _navigateEvent = MutableLiveData<Event<Destination>>()
    val navigateEvent: LiveData<Event<Destination>> = _navigateEvent

    val welcomeText = "Benvenuto!"

    private val auth = FirebaseAuth.getInstance()

    fun login() {
        val e = email.value.orEmpty().trim()
        val p = password.value.orEmpty()
        if (e.isBlank() || p.isBlank()) {
            _navigateEvent.value = Event(Destination.SHOW_TOAST("Inserisci email e password"))
            return
        }
        _isLoading.value = true
        auth.signInWithEmailAndPassword(e, p)
            .addOnCompleteListener { task ->
                _isLoading.value = false
                if (task.isSuccessful) {
                    _isSignedIn.value = true
                    _navigateEvent.value = Event(Destination.LOGIN_SUCCESS)
                } else {
                    val msg = when (task.exception) {
                        is FirebaseAuthInvalidUserException        -> "Utente non trovato"
                        is FirebaseAuthInvalidCredentialsException -> "Email o Password errata"
                        else                                       -> task.exception?.localizedMessage ?: "Login fallito"
                    }
                    _navigateEvent.value = Event(Destination.SHOW_TOAST(msg))
                }
            }
    }

    fun signOut() {
        auth.signOut()
        _isSignedIn.value = false
        _navigateEvent.value = Event(Destination.SHOW_TOAST("Disconnesso"))
    }

    fun navigateToRegister() {
        _navigateEvent.value = Event(Destination.GO_REGISTER)
    }

    fun onRegisterComplete() {
        _navigateEvent.value = Event(Destination.GO_MAIN)
    }

    sealed class Destination {
        object LOGIN_SUCCESS : Destination()
        object GO_REGISTER   : Destination()
        object GO_MAIN       : Destination()
        data class SHOW_TOAST(val msg: String) : Destination()
    }

    /** Helper per eventi single-shot */
    class Event<out T>(private val content: T) {
        private var handled = false
        fun getIfNotHandled(): T? = if (handled) null else { handled = true; content }
    }
}
