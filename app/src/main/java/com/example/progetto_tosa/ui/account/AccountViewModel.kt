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
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import java.util.concurrent.TimeUnit

class AccountViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()
    private val ctx: Context get() = getApplication()

    // Observable per DataBinding
    val displayName     = ObservableField("Effettua il login")
    val roleText        = ObservableField("")
    val iconStrokeColor = ObservableField(
        ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.yellow))
    )
    val isTrainer       = ObservableBoolean(false)
    val isLoggedIn      = ObservableBoolean(auth.currentUser != null)
    val reminderEnabled = ObservableBoolean(isReminderEnabled())
    val showSettings    = ObservableBoolean(false)

    /** Calcola al volo quale drawable usare */
    val iconResValue: Int
        get() = when {
            isLoggedIn.get() && isTrainer.get() -> R.drawable.personal
            isLoggedIn.get()                    -> R.drawable.atleta
            else                                -> R.drawable.account_principal
        }

    init {
        updateUI()
        scheduleNotifications()
    }

    fun onLoginClick(nav: NavController) {
        nav.navigate(R.id.action_navigation_account_to_navigation_login)
    }

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

    fun onSwitchReminderChanged(enabled: Boolean) {
        reminderEnabled.set(enabled)
        setReminderEnabled(enabled)
        if (enabled) checkAndRequestNotificationPermission()
        else {
            WorkManager.getInstance(ctx).cancelUniqueWork("morningNotification")
            WorkManager.getInstance(ctx).cancelUniqueWork("eveningNotification")
            Toast.makeText(ctx, "Promemoria disattivato", Toast.LENGTH_SHORT).show()
        }
    }

    /** Inverte la visibilità delle impostazioni */
    fun onSettingsClick() {
        showSettings.set(!showSettings.get())
    }

    // --- SharedPreferences ---
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

    // --- Firestore / UI ---
    fun updateUI() {
        val user = auth.currentUser
        if (user == null) {
            preloadFromPrefs()
            return
        }
        val uid = user.uid
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) bindData(
                    doc.getString("firstName"),
                    doc.getString("lastName"),
                    doc.getBoolean("isPersonalTrainer") == true
                ) else fetchPT(uid)
            }
            .addOnFailureListener { preloadFromPrefs() }
    }

    private fun fetchPT(uid: String) {
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

    private fun bindData(first: String?, last: String?, pt: Boolean) {
        val name = if (!first.isNullOrBlank()) "$first ${last.orEmpty()}"
        else auth.currentUser?.email.orEmpty()
        displayName.set(name)
        roleText.set(if (pt) "Personal Trainer" else "Atleta")
        isTrainer.set(pt)
        isLoggedIn.set(true)

        ctx.getSharedPreferences("user_data", Context.MODE_PRIVATE).edit().apply {
            putString("saved_display_name", name)
            putBoolean("is_trainer", pt)
            apply()
        }

        iconStrokeColor.set(
            ColorStateList.valueOf(
                ContextCompat.getColor(ctx, if (pt) R.color.green else R.color.orange)
            )
        )
    }

    private fun preloadFromPrefs() {
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
            displayName.set("Effettua il login")
            roleText.set("")
            isTrainer.set(false)
            isLoggedIn.set(false)
            iconStrokeColor.set(
                ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.yellow))
            )
        }
    }

    // --- Notifiche ---
    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                ctx, android.Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Fragment gestisce onRequestPermissionsResult
        } else scheduleNotifications()
    }

    private fun scheduleNotifications() {
        scheduleNotification(10, 0, 1,
            "Buongiorno!",
            "Ricordati di bere abbastanza acqua durante la giornata.",
            "morningNotification"
        )
        scheduleNotification(18, 0, 2,
            "È ora di allenarsi!",
            "Non saltare la tua scheda di oggi 💪",
            "eveningNotification"
        )
    }

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
