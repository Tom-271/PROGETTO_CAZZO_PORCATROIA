package com.example.progetto_tosa.ui.account

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.progetto_tosa.R
import com.example.progetto_tosa.databinding.FragmentAccountBinding
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import androidx.work.*
import com.example.progetto_tosa.workers.MyWorker
import java.util.*
import java.util.concurrent.TimeUnit

class AccountFragment : Fragment() {


    // === VARIABILI E INIZIALIZZAZIONI ===


    private var _binding: FragmentAccountBinding? = null
    private val binding get() = _binding!!

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }


    // === CICLO DI VITA DEL FRAGMENT ===


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        // === TEMA CHIARO/SCURO ===


        val prefs = requireActivity().getSharedPreferences("settings", Context.MODE_PRIVATE)
        var isDarkMode = prefs.getBoolean("darkMode", true)

        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )

        updateThemeButtons(isDarkMode)

        binding.btnLightMode.setOnClickListener {
            if (!isDarkMode) {
                isDarkMode = true
                prefs.edit().putBoolean("darkMode", true).apply()
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                updateThemeButtons(isDarkMode)
            }
        }

        binding.btnDarkMode.setOnClickListener {
            if (isDarkMode) {
                isDarkMode = false
                prefs.edit().putBoolean("darkMode", false).apply()
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                updateThemeButtons(isDarkMode)
            }
        }


        // === NAVIGAZIONE ===


        binding.UserJourney.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_account_to_navigation_allievi)
        }

        binding.userData.setOnClickListener {
            findNavController().navigate(R.id.action_account_to_UserData)
        }

        binding.UserProgram.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_account_to_navigation_home)
        }


        // === LOGIN / LOGOUT ===


        binding.ButtonLogin.setOnClickListener {
            startActivity(Intent(requireContext(), LoginActivity::class.java))
        }

        binding.signOut.setOnClickListener {
            Toast.makeText(requireContext(), "Ci vediamo al prossimo allenamento!", Toast.LENGTH_SHORT).show()
            AuthUI.getInstance().signOut(requireContext()).addOnCompleteListener {
                clearSavedUserData()
                updateLoginLogoutButtons()
                preloadUserDataFromPreferences()
                updateUI()
            }
        }

        updateLoginLogoutButtons()


        // === PROMEMORIA NOTIFICA ===


        binding.switchReminder.isChecked = isReminderEnabled()

        binding.switchReminder.setOnCheckedChangeListener { _, isChecked ->
            setReminderEnabled(isChecked)

            if (isChecked) {
                checkAndRequestNotificationPermission()
            } else {
                WorkManager.getInstance(requireContext()).cancelUniqueWork("dailyWorkoutNotification")
                Toast.makeText(requireContext(), "Promemoria disattivato", Toast.LENGTH_SHORT).show()
            }
        }

        scheduleNotifications(requireContext())
        WorkManager.getInstance(requireContext()).cancelUniqueWork("dailyWorkoutNotification")

        preloadUserDataFromPreferences()
        updateUI()
    }

    override fun onResume() {
        super.onResume()
        updateLoginLogoutButtons()
        preloadUserDataFromPreferences()
        updateUI()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    // === TEMA E BOTTONI ===


    private fun updateLoginLogoutButtons() {
        val isLoggedIn = auth.currentUser != null
        binding.ButtonLogin.visibility = if (isLoggedIn) View.GONE else View.VISIBLE
        binding.signOut.visibility = if (isLoggedIn) View.VISIBLE else View.GONE
    }

    private fun updateThemeButtons(isDark: Boolean) {
        binding.btnDarkMode.visibility = if (isDark) View.VISIBLE else View.GONE
        binding.btnLightMode.visibility = if (!isDark) View.VISIBLE else View.GONE
    }


    // === GESTIONE DATI UTENTE ===


    private fun clearSavedUserData() {
        requireActivity()
            .getSharedPreferences("user_data", Context.MODE_PRIVATE)
            .edit() {
                remove("saved_display_name")
                remove("is_trainer")
            }
    }

    private fun preloadUserDataFromPreferences() {
        val prefs = requireActivity().getSharedPreferences("user_data", Context.MODE_PRIVATE)
        val savedName = prefs.getString("saved_display_name", null)
        val isTrainer = prefs.getBoolean("is_trainer", false)

        if (!savedName.isNullOrBlank()) {
            binding.NomeUtente.text = savedName

            if (isTrainer) {
                binding.ruolo.text = "Personal Trainer"
                binding.iconaUtente.setImageResource(R.drawable.personal)
                binding.iconaUtente.strokeColor = ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.green)
                )
                binding.NomeUtente.setTextColor(
                    ContextCompat.getColorStateList(requireContext(), R.color.perNomePersonal)
                )
            } else {
                binding.ruolo.text = "Atleta"
                binding.iconaUtente.setImageResource(R.drawable.atleta)
                binding.iconaUtente.strokeColor = ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.orange)
                )
                binding.NomeUtente.setTextColor(
                    ContextCompat.getColorStateList(requireContext(), R.color.perNomeAtleta)
                )
            }
        } else {
            binding.NomeUtente.text = "Effettua il login"
            binding.ruolo.text = ""
            binding.iconaUtente.setImageResource(R.drawable.account_principal)
        }
    }

    private fun updateUI() {
        val user = auth.currentUser

        if (user == null) {
            applySavedUserDataOrFallback()
            return
        }

        val uid = user.uid

        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    bindUserData(doc)
                } else {
                    db.collection("personal_trainers").document(uid)
                        .get()
                        .addOnSuccessListener { ptDoc ->
                            if (ptDoc.exists()) {
                                bindUserData(ptDoc)
                            } else {
                                applySavedUserDataOrFallback()
                            }
                        }
                }
            }
            .addOnFailureListener {
                applySavedUserDataOrFallback()
            }
    }

    private fun applySavedUserDataOrFallback() {
        val prefs = requireActivity().getSharedPreferences("user_data", Context.MODE_PRIVATE)
        val savedName = prefs.getString("saved_display_name", null)
        val isTrainer = prefs.getBoolean("is_trainer", false)

        if (!savedName.isNullOrBlank()) {
            binding.NomeUtente.text = savedName

            if (isTrainer) {
                binding.TrainerProgram.visibility = View.VISIBLE
                val green = ContextCompat.getColor(requireContext(), R.color.green)
                binding.iconaUtente.strokeColor = ColorStateList.valueOf(green)
                binding.ruolo.text = "Personal Trainer"
                binding.iconaUtente.setImageResource(R.drawable.personal)
                binding.NomeUtente.setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.perNomePersonal))
            } else {
                binding.TrainerProgram.visibility = View.GONE
                val orange = ContextCompat.getColor(requireContext(), R.color.orange)
                binding.iconaUtente.strokeColor = ColorStateList.valueOf(orange)
                binding.ruolo.text = "Atleta"
                binding.iconaUtente.setImageResource(R.drawable.atleta)
                binding.NomeUtente.setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.perNomeAtleta))
            }

        } else {
            binding.NomeUtente.text = "Utente sconosciuto"
            binding.iconaUtente.setImageResource(R.drawable.account_principal)
            binding.ruolo.text = ""
        }
    }

    private fun bindUserData(doc: DocumentSnapshot) {
        val name = doc.getString("firstName").orEmpty()
        val surname = doc.getString("lastName").orEmpty()
        val email = doc.getString("email") ?: auth.currentUser?.email.orEmpty()
        val displayName = if (name.isNotBlank()) "$name $surname" else email

        binding.NomeUtente.text = displayName
        requireActivity().getSharedPreferences("user_data", Context.MODE_PRIVATE).edit {
            putString("saved_display_name", displayName)
            putBoolean("is_trainer", doc.getBoolean("isPersonalTrainer") == true)
        }

        listOf(
            binding.tvFirstLast,
            binding.tvEmail,
            binding.tvBirthday,
            binding.tvAge,
            binding.tvWeight,
            binding.tvHeight
        ).forEach { it.visibility = View.GONE }

        val isPT = doc.getBoolean("isPersonalTrainer") == true
        if (isPT) {
            binding.TrainerProgram.visibility = View.VISIBLE
            val green = ContextCompat.getColor(requireContext(), R.color.green)
            binding.iconaUtente.strokeColor = ColorStateList.valueOf(green)
            binding.ruolo.text = "Personal Trainer"
            binding.iconaUtente.setImageResource(R.drawable.personal)
            binding.NomeUtente.setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.perNomePersonal))
        } else {
            binding.TrainerProgram.visibility = View.GONE
            val orange = ContextCompat.getColor(requireContext(), R.color.orange)
            binding.iconaUtente.strokeColor = ColorStateList.valueOf(orange)
            binding.ruolo.text = "Atleta"
            binding.iconaUtente.setImageResource(R.drawable.atleta)
            binding.NomeUtente.setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.perNomeAtleta))
        }
    }


    // === NOTIFICHE E WORKMANAGER ===


    private fun isReminderEnabled(): Boolean {
        val prefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)
        return prefs.getBoolean("reminder_enabled", false)
    }

    private fun setReminderEnabled(enabled: Boolean) {
        val prefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("reminder_enabled", enabled).apply()
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            } else {
                scheduleNotifications(requireContext())
            }
        } else {
            scheduleNotifications(requireContext())
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                scheduleNotifications(requireContext())
            } else {
                Toast.makeText(requireContext(), "Permesso notifiche negato", Toast.LENGTH_SHORT).show()
                binding.switchReminder.isChecked = false
                setReminderEnabled(false)
            }
        }
    }

    private fun scheduleSingleNotification(
        context: Context,
        hour: Int,
        minute: Int,
        id: Int,
        title: String,
        message: String,
        workName: String
    ) {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
        }

        val delay = target.timeInMillis - now.timeInMillis

        val input = Data.Builder()
            .putInt("id", id)
            .putString("title", title)
            .putString("message", message)
            .build()

        val request = PeriodicWorkRequestBuilder<MyWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(input)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            workName,
            ExistingPeriodicWorkPolicy.REPLACE,
            request
        )
    }


    fun scheduleNotifications(context: Context) {

        // notifica 1 alle 10:00

        scheduleSingleNotification(
            context = context,
            hour = 10,
            minute = 0,
            id = 1,
            title = "Buongiorno!",
            message = "Ricordati di bere abbastanza acqua durante la giornata.",
            workName = "morningNotification"
        )

        // notifica 2 alle 18:00

        scheduleSingleNotification(
            context = context,
            hour = 18,
            minute = 0,
            id = 2,
            title = "È ora di allenarsi!",
            message = "Non saltare la tua scheda di oggi 💪",
            workName = "eveningNotification"
        )
    }

}

