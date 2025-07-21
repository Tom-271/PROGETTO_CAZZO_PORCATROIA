package com.example.progetto_tosa.ui.account

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Build
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.AndroidViewModel
import androidx.navigation.NavController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.progetto_tosa.R
import com.example.progetto_tosa.workers.MyWorker
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import java.util.concurrent.TimeUnit

class AccountViewModel(application: Application) : AndroidViewModel(application) {

    private var alreadyInitialized = false

    // Riferimento all'autenticazione Firebase
    private val auth = FirebaseAuth.getInstance()
    // Riferimento al Firestore
    private val db   = FirebaseFirestore.getInstance()
    // Context ottenuto dall'application per operazioni Android
    private val ctx: Context get() = getApplication()

    // --- Proprietà osservabili per DataBinding ---
    // Nome visualizzato (o invito al login)
    val displayName     = ObservableField("Effettua il login")
    // Testo che mostra il ruolo (Personal Trainer o Atleta)
    val roleText        = ObservableField("")
    // Colore del bordo dell'icona in base al ruolo/stato
    val iconStrokeColor = ObservableField(
        ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.yellow))
    )
    // Flag se l'utente è trainer
    val isTrainer       = ObservableBoolean(false)
    // Flag se l'utente è attualmente loggato
    val isLoggedIn      = ObservableBoolean(auth.currentUser != null)
    // Flag se il promemoria è abilitato (da SharedPreferences)
    val reminderEnabled = ObservableBoolean(isReminderEnabled())
    // Mostra o nasconde le impostazioni
    val showSettings    = ObservableBoolean(false)

    /**
     * Calcola quale drawable usare per l'icona in base a login e ruolo
     */
    val iconResValue: Int
        get() = when {
            isLoggedIn.get() && isTrainer.get() -> R.drawable.personal
            isLoggedIn.get()                    -> R.drawable.atleta
            else                                -> R.drawable.account_principal
        }

    init {
        // Aggiorna UI con dati utente e pianifica notifiche
        preloadFromPrefs()
        updateUI()
        scheduleNotifications()
    }

    /**
     * Naviga alla schermata di login
     */
    fun onLoginClick(nav: NavController) {
        nav.navigate(R.id.action_navigation_account_to_navigation_login)
    }

    /**
     * Effettua il logout, resetta dati e UI
     */
    fun onSignOut(nav: NavController) {
        Toast.makeText(ctx, "Ci vediamo al prossimo allenamento!", Toast.LENGTH_SHORT).show()
        AuthUI.getInstance().signOut(ctx).addOnCompleteListener {
            clearSavedUserData()
            isLoggedIn.set(false)
            isTrainer.set(false)
            reminderEnabled.set(false)
            showSettings.set(false)
            displayName.set("Effettua il login")
            roleText.set("")
            iconStrokeColor.set(
                ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.yellow))
            )
        }
    }

    /**
     * Gestisce il cambiamento dello switch per promemoria
     */
    fun onSwitchReminderChanged(enabled: Boolean) {
        reminderEnabled.set(enabled)
        setReminderEnabled(enabled)
        if (enabled) checkAndRequestNotificationPermission()
        else {
            // Disabilita notifiche pianificate
            WorkManager.getInstance(ctx).cancelUniqueWork("morningNotification")
            WorkManager.getInstance(ctx).cancelUniqueWork("eveningNotification")
            Toast.makeText(ctx, "Promemoria disattivato", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Mostra o nasconde le impostazioni utente
     */
    fun onSettingsClick() {
        showSettings.set(!showSettings.get())
    }

    // --- Gestione SharedPreferences per promemoria e dati utente ---
    private fun isReminderEnabled(): Boolean =
        ctx.getSharedPreferences("settings", Context.MODE_PRIVATE)
            .getBoolean("reminder_enabled", false)

    private fun setReminderEnabled(on: Boolean) {
        ctx.getSharedPreferences("settings", Context.MODE_PRIVATE)
            .edit().putBoolean("reminder_enabled", on).apply()
    }

    private fun clearSavedUserData() {
        ctx.getSharedPreferences("user_data", Context.MODE_PRIVATE)
            .edit().clear().apply()
    }

    // --- Recupero dati utente da Firestore e aggiornamento UI ---
    fun updateUI() {
        if (alreadyInitialized) return // evita ricariche multiple

        val user = auth.currentUser
        if (user == null) {
            preloadFromPrefs()
            alreadyInitialized = true
            return
        }

        val uid = user.uid
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    bindData(
                        doc.getString("firstName"),
                        doc.getString("lastName"),
                        doc.getBoolean("isPersonalTrainer") == true
                    )
                } else {
                    fetchPT(uid)
                }
                alreadyInitialized = true
            }
            .addOnFailureListener {
                preloadFromPrefs()
                alreadyInitialized = true
            }
    }

    private fun fetchPT(uid: String) {
        // Se non trovato in "users", controlla in "personal_trainers"
        db.collection("personal_trainers").document(uid).get()
            .addOnSuccessListener { ptDoc ->
                if (ptDoc.exists()) bindData(
                    ptDoc.getString("firstName"),
                    ptDoc.getString("lastName"),
                    ptDoc.getBoolean("isPersonalTrainer") == true
                ) else preloadFromPrefs()
            }
            .addOnFailureListener { preloadFromPrefs() }
    }

    /**
     * Popola i campi observables con i dati utente recuperati
     */
    private fun bindData(first: String?, last: String?, pt: Boolean) {
        val name = if (!first.isNullOrBlank()) "$first ${last.orEmpty()}"
        else auth.currentUser?.email.orEmpty()
        displayName.set(name)
        roleText.set(if (pt) "Personal Trainer" else "Atleta")
        isTrainer.set(pt)
        isLoggedIn.set(true)

        // Salva in SharedPreferences per preload successivo
        ctx.getSharedPreferences("user_data", Context.MODE_PRIVATE).edit().apply {
            putString("saved_display_name", name)
            putBoolean("is_trainer", pt)
            apply()
        }

        // Aggiorna colore bordo icona in base al ruolo
        iconStrokeColor.set(
            ColorStateList.valueOf(
                ContextCompat.getColor(ctx, if (pt) R.color.green else R.color.lapis)
            )
        )
    }

    private fun preloadFromPrefs() {
        // Carica dati utente salvati se esistono
        val prefs = ctx.getSharedPreferences("user_data", Context.MODE_PRIVATE)
        val name  = prefs.getString("saved_display_name", null)
        val pt    = prefs.getBoolean("is_trainer", false)
        if (!name.isNullOrBlank()) {
            displayName.set(name)
            roleText.set(if (pt) "Personal Trainer" else "Atleta")
            isTrainer.set(pt)
            isLoggedIn.set(true)
            iconStrokeColor.set(
                ColorStateList.valueOf(
                    ContextCompat.getColor(ctx, if (pt) R.color.green else R.color.orange)
                )
            )
        } else {
            // Se nessun dato, reset UI base
            displayName.set("Effettua il login")
            roleText.set("")
            isTrainer.set(false)
            isLoggedIn.set(false)
            iconStrokeColor.set(
                ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.yellow))
            )
        }
    }

    // --- Notifiche giornaliere via WorkManager ---
    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                ctx, android.Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Se Android 13+, richiedi permesso (gestito dal Fragment)
        } else scheduleNotifications()
    }

    private fun scheduleNotifications() {
        // Pianifica notifica mattutina alle 10:00
        scheduleNotification(10, 0, 1,
            "Buongiorno!",
            "Ricordati di bere abbastanza acqua durante la giornata.",
            "morningNotification"
        )
        // Pianifica notifica serale alle 18:00
        scheduleNotification(18, 0, 2,
            "È ora di allenarsi!",
            "Non saltare la tua scheda di oggi 💪",
            "eveningNotification"
        )
    }

    // Funzione per linkare atleta al PT in Firestore
    fun linkAthleteToPT(athleteUid: String) {
        val ptUid = auth.currentUser?.uid ?: return
        val coll  = "atleti_di_$ptUid"
        // Prima recupera il nome dell'atleta da Firestore
        db.collection("users").document(athleteUid).get()
            .addOnSuccessListener { doc ->
                val first = doc.getString("firstName").orEmpty()
                val last  = doc.getString("lastName").orEmpty()
                val fullName = if (first.isNotBlank()) "$first $last".trim() else athleteUid
                // Salva id e nome
                db.collection(coll)
                    .document(athleteUid)
                    .set(mapOf(
                        "uid"      to athleteUid,
                        "name"     to fullName
                    ))
                    .addOnSuccessListener {
                        Toast.makeText(ctx, "Atleta aggiunto: $fullName", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(ctx, "Errore salvataggio atleta: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(ctx, "Impossibile recuperare dati atleta: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    // Crea o aggiorna un PeriodicWorkRequest per notifiche ogni 24h
    private fun scheduleNotification(
        hour: Int, minute: Int, id: Int,
        title: String, msg: String, workName: String
    ) {
        val now    = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
        }
        val delay = target.timeInMillis - now.timeInMillis
        val input = androidx.work.Data.Builder()
            .putInt("id", id)
            .putString("title", title)
            .putString("message", msg)
            .build()
        val req = PeriodicWorkRequestBuilder<MyWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(input)
            .build()
        WorkManager.getInstance(ctx)
            .enqueueUniquePeriodicWork(workName, ExistingPeriodicWorkPolicy.REPLACE, req)
    }
}