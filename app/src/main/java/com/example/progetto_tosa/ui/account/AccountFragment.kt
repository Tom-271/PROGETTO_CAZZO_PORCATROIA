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

    private var _binding: FragmentAccountBinding? = null
    private val binding get() = _binding!!

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Navigazione
        binding.userData.setOnClickListener {
            findNavController().navigate(R.id.action_account_to_UserData)
        }
        binding.UserProgram.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_account_to_navigation_home)
        }
        binding.TrainerAllievs.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_account_to_navigation_allievi)
        }
        // Login/Logout
        binding.ButtonLogin.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_account_to_navigation_login)
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

        // Impostazioni overlay
        binding.impostazioni.setOnClickListener {
            if(binding.switchReminder.visibility == View.VISIBLE)
            {
                binding.labelReminder.visibility = View.GONE
                binding.switchReminder.visibility = View.GONE
            }
            else
            {
                binding.switchReminder.visibility = View.VISIBLE
                binding.labelReminder.visibility = View.VISIBLE

            }
        }


        // Promemoria notifiche
        binding.switchReminder.isChecked = isReminderEnabled()
        binding.switchReminder.setOnCheckedChangeListener { _, isChecked ->
            setReminderEnabled(isChecked)
            if (isChecked) checkAndRequestNotificationPermission()
            else {
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

    private fun updateLoginLogoutButtons() {
        val isLoggedIn = auth.currentUser != null

        // Login / Logout
        binding.ButtonLogin.visibility    = if (isLoggedIn) View.GONE else View.VISIBLE
        binding.signOut.visibility        = if (isLoggedIn) View.VISIBLE else View.GONE

        // TrainerAllievs: visibile solo se loggato E is_trainer=true
        val prefs = requireActivity()
            .getSharedPreferences("user_data", Context.MODE_PRIVATE)
        val isTrainer = prefs.getBoolean("is_trainer", false)
        binding.TrainerAllievs.visibility = if (isLoggedIn && isTrainer) View.VISIBLE else View.GONE

        // Disabilito / offusco gli altri pulsanti se non loggato
        listOf(binding.UserProgram, binding.impostazioni, binding.userData).forEach { btn ->
            btn.isEnabled = isLoggedIn
            btn.alpha     = if (isLoggedIn) 1f else 0.4f
        }
    }



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
                if (doc.exists()) bindUserData(doc)
                else db.collection("personal_trainers").document(uid)
                    .get()
                    .addOnSuccessListener { ptDoc ->
                        if (ptDoc.exists()) bindUserData(ptDoc)
                        else applySavedUserDataOrFallback()
                    }
            }
            .addOnFailureListener { applySavedUserDataOrFallback() }
    }

    private fun applySavedUserDataOrFallback() {
        preloadUserDataFromPreferences()
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
            binding.tvWeight,
            binding.tvHeight
        ).forEach { it.visibility = View.GONE }

        val isPT = doc.getBoolean("isPersonalTrainer") == true
        binding.TrainerAllievs.visibility = if (isPT) View.VISIBLE else View.GONE
        binding.iconaUtente.strokeColor = ColorStateList.valueOf(
            ContextCompat.getColor(
                requireContext(), if (isPT) R.color.green else R.color.orange
            )
        )
        binding.ruolo.text = if (isPT) "Personal Trainer" else "Atleta"
        binding.iconaUtente.setImageResource(if (isPT) R.drawable.personal else R.drawable.atleta)
        binding.NomeUtente.setTextColor(
            ContextCompat.getColorStateList(
                requireContext(), if (isPT) R.color.perNomePersonal else R.color.perNomeAtleta
            )
        )
    }

    private fun isReminderEnabled(): Boolean {
        return requireContext()
            .getSharedPreferences("settings", Context.MODE_PRIVATE)
            .getBoolean("reminder_enabled", false)
    }

    private fun setReminderEnabled(enabled: Boolean) {
        requireContext()
            .getSharedPreferences("settings", Context.MODE_PRIVATE)
            .edit().putBoolean("reminder_enabled", enabled).apply()
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                requireContext(), android.Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                1001
            )
        } else scheduleNotifications(requireContext())
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            scheduleNotifications(requireContext())
        } else {
            Toast.makeText(requireContext(), "Permesso notifiche negato", Toast.LENGTH_SHORT).show()
            binding.switchReminder.isChecked = false
            setReminderEnabled(false)
        }
    }

    private fun scheduleSingleNotification(
        context: Context, hour: Int, minute: Int,
        id: Int, title: String, message: String, workName: String
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

    private fun scheduleNotifications(context: Context) {
        scheduleSingleNotification(
            context = context,
            hour = 10, minute = 0, id = 1,
            title = "Buongiorno!",
            message = "Ricordati di bere abbastanza acqua durante la giornata.",
            workName = "morningNotification"
        )
        scheduleSingleNotification(
            context = context,
            hour = 18, minute = 0, id = 2,
            title = "È ora di allenarsi!",
            message = "Non saltare la tua scheda di oggi 💪",
            workName = "eveningNotification"
        )
    }
}
