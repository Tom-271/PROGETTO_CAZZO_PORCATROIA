package com.example.progetto_tosa.ui.home

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible   // assicurati di avere questo import

import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.progetto_tosa.R
import com.example.progetto_tosa.databinding.FragmentHomeBinding
import com.example.progetto_tosa.chat.* // ChatOverlay, ChatViewModel, ChatVMFactory, ChatRepository, Secrets
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private var currentDate = Date()

    // Chat
    private lateinit var chatOverlay: ChatOverlay
    private lateinit var chatVM: ChatViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /* -------- CHAT SETUP -------- */
        val repo = ChatRepository.create(apiKey = Secrets.GEMINI_KEY)
        chatVM = ViewModelProvider(this, ChatVMFactory(repo))[ChatViewModel::class.java]
        chatOverlay = ChatOverlay(view as ViewGroup, chatVM, viewLifecycleOwner).apply {
            // aggiungi toggle nel modulo se non l'hai già fatto
            // fun toggle() { if (overlayRoot.isVisible) hide() else show() }
        }
        binding.buttonForChatGPT.setOnClickListener {
            // se hai implementato toggle():
            chatOverlay.toggle()
        }
        /* ---------------------------- */


        setupBannerDate()
        binding.fabBannerStatus.hide()

        val user = auth.currentUser
        if (user == null) {
            showLoginPrompt()
            return
        }

        val prefs = requireActivity().getSharedPreferences("user_data", Context.MODE_PRIVATE)
        val isTrainer = prefs.getBoolean("is_trainer", false)
        if (isTrainer) {
            binding.fabBannerStatus.hide()
            showButtonsForPT()
            return
        }

        showButtonsForUser()

        val fullName = prefs.getString("saved_display_name", "").orEmpty()
        if (fullName.isBlank()) {
            binding.fabBannerStatus.show()
            return
        }

        checkTodaysSchedule(fullName)

        binding.fabBannerStatus.setOnClickListener {
            binding.fabBannerStatus.animate()
                .translationY(binding.fabBannerStatus.height.toFloat())
                .alpha(0f)
                .setDuration(300)
                .withEndAction {
                    binding.fabBannerStatus.visibility = View.GONE
                    binding.fabBannerStatus.translationY = 0f
                    binding.fabBannerStatus.alpha = 1f
                }
        }

        binding.Myprogression.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_home_to_progressionFragment)
        }
    }


    private fun setupBannerDate() {
        val dayNameFmt = SimpleDateFormat("EEEE", Locale.getDefault())
        val dayNumberFmt = SimpleDateFormat("d", Locale.getDefault())
        val monthFmt = SimpleDateFormat("MMMM", Locale.getDefault())

        binding.bannerDayName.text = dayNameFmt.format(currentDate).uppercase(Locale.getDefault())
        binding.bannerDayNumber.text = dayNumberFmt.format(currentDate)
        binding.bannerMonth.text = monthFmt.format(currentDate).uppercase(Locale.getDefault())
    }

    private fun checkTodaysSchedule(fullName: String) {
        val todayId = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val categories = listOf("bodybuilding", "cardio", "corpo_libero", "stretching")
        val ref = db.collection("schede_del_pt").document(fullName)
        val tasks = categories.map { cat ->
            ref.collection(todayId).document(cat)
                .collection("esercizi").limit(1).get()
        }
        Tasks.whenAllSuccess<QuerySnapshot>(tasks)
            .addOnSuccessListener { snaps ->
                val hasAny = snaps.any { it.documents.isNotEmpty() }
                if (hasAny) {
                    binding.fabBannerStatus.apply {
                        text = "Oggi il PT ha preparato per te una scheda!"
                        show()
                    }
                } else {
                    binding.fabBannerStatus.hide()
                }
            }
            .addOnFailureListener {
                binding.fabBannerStatus.apply {
                    text = "Errore controllo scheda"
                    show()
                }
            }
    }

    private fun showLoginPrompt() {
        binding.buttonForTheScheduleIDid.visibility = View.VISIBLE
        binding.buttonForTheScheduleIDid.alpha = 0.2f
        binding.buttonForTheSchedulePersonalTrainerDid.visibility = View.VISIBLE
        binding.buttonForTheSchedulePersonalTrainerDid.alpha = 0.2f
        binding.buttonForPersonalTrainer.visibility = View.GONE
        binding.buttonInutile.visibility = View.VISIBLE

        binding.buttonInutile.text = "L'applicazione offre la possibilità di creare delle schede personali di allenamento e di interagire con il proprio personal trainer, accedi al servizio per farlo anche tu!"
        binding.buttonInutile.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_home_to_navigation_login)
        }
    }

    private fun showButtonsForUser() {
        val prefs = requireActivity().getSharedPreferences("user_data", Context.MODE_PRIVATE)
        val fullName = prefs.getString("saved_display_name", "").orEmpty()

        binding.buttonForPersonalTrainer.visibility = View.GONE
        binding.buttonForTheScheduleIDid.visibility = View.VISIBLE
        binding.buttonForTheSchedulePersonalTrainerDid.visibility = View.VISIBLE

        binding.buttonForTheScheduleIDid.setOnClickListener {
            val selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(currentDate)
            val bundle = Bundle().apply { putString("selectedDate", selectedDate) }
            findNavController().navigate(
                R.id.action_navigation_home_to_fragment_my_auto_schedule,
                bundle
            )
        }

        binding.buttonForTheSchedulePersonalTrainerDid.setOnClickListener {
            val selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            if (fullName.isBlank()) {
                Toast.makeText(context, "Nome utente non disponibile", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val bundle = Bundle().apply {
                putString("selectedDate", selectedDate)
                putString("selectedUser", fullName)
            }
            findNavController().navigate(
                R.id.action_navigation_home_to_fragment_my_trainer_schedule,
                bundle
            )
        }
    }

    private fun showButtonsForPT() {
        binding.buttonForPersonalTrainer.visibility = View.VISIBLE
        binding.buttonForTheScheduleIDid.visibility = View.GONE
        binding.buttonForTheSchedulePersonalTrainerDid.visibility = View.GONE

        binding.buttonForPersonalTrainer.setOnClickListener {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val bundle = Bundle().apply { putString("selectedDate", today) }
            findNavController().navigate(
                R.id.action_navigation_home_to_fragment_my_auto_schedule,
                bundle
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
