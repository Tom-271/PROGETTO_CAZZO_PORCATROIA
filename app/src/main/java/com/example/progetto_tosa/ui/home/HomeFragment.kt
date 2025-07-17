package com.example.progetto_tosa.ui.home

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.progetto_tosa.R
import com.example.progetto_tosa.databinding.FragmentHomeBinding
import com.google.android.gms.tasks.Tasks
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.timer

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private var carouselTimer: java.util.Timer? = null
    private var currentPosition = 0
    companion object {
        private const val TAG = "HomeFragment"
        private val CATEGORIES = listOf("bodybuilding", "cardio", "corpo_libero", "stretching")
        private const val AUTO_SCROLL_INTERVAL = 2500L
    }

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

        // 1) Data di oggi in stile card calendario
        val today = Date()
        val dayNameFmt   = SimpleDateFormat("EEEE", Locale.getDefault())
        val dayNumberFmt = SimpleDateFormat("d",     Locale.getDefault())
        val monthFmt     = SimpleDateFormat("MMMM", Locale.getDefault())

        binding.bannerDayName.text   = dayNameFmt.format(today).uppercase(Locale.getDefault())
        binding.bannerDayNumber.text = dayNumberFmt.format(today)
        binding.bannerMonth.text     = monthFmt.format(today).uppercase(Locale.getDefault())

        // 2) ID per Firestore (yyyy-MM-dd)
        val keyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayId = keyFmt.format(today)
        Log.d(TAG, "TodayId = $todayId")

        // Inizialmente nascondi la FAB
        binding.fabBannerStatus.hide()

        // 3) Verifica login
        val user = auth.currentUser
        if (user == null) {
            binding.fabBannerStatus.hide()
            setAllGone()
            setupCarousel()
            return
        }

        // 4) Verifica ruolo
        val prefs = requireActivity().getSharedPreferences("user_data", Context.MODE_PRIVATE)
        val isTrainer = prefs.getBoolean("is_trainer", false)
        if (isTrainer) {
            binding.fabBannerStatus.hide()
            showButtonsForPT()
            setupCarousel()
            return
        }

        // 5) Utente normale
        showButtonsForUser()

        // 6) Recupero nome utente
        val fullName = prefs.getString("saved_display_name", "").orEmpty()
        if (fullName.isBlank()) {
            binding.fabBannerStatus.show()
            setupCarousel()
            return
        }

        // 7) Controllo esercizi odierni
        db.collection("schede_del_pt").document(fullName).let { ref ->
            val tasks = CATEGORIES.map { cat ->
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

        // 8) Chiudi la FAB con animazione verso il basso
        binding.fabBannerStatus.setOnClickListener {
            val fab = binding.fabBannerStatus
            val lp = fab.layoutParams as ViewGroup.MarginLayoutParams
            val distance = fab.height + lp.bottomMargin

            fab.animate()
                .translationY(distance.toFloat())
                .alpha(0f)
                .setDuration(300)
                .withEndAction {
                    fab.visibility = View.GONE
                    fab.translationY = 0f
                    fab.alpha = 1f
                }
        }

        // 9) Avvia carousel
        setupCarousel()
    }

    private fun setupCarousel() {
        val levels = listOf("Easy", "Medium", "Hard", "Insane")
        val descriptions = listOf(
            "Per coloro che vogliono iniziare",
            "Se sei un runner con esperienza",
            "Dedicato ai Professionisti",
            "Per coloro che vogliono superare i limiti!"
        )
        val infos = listOf(
            "3–5 km, 20–35 min",
            "5–10 km, 30–60 min",
            "10–15 km, 45–75 min",
            "> 15 km o intervalli"
        )
        val tips = listOf(
            "Tip: Riscaldati con stretching leggero",
            "Tip: Mantieni respirazione regolare",
            "Tip: Porta con te acqua extra",
            "Tip: Sfida i tuoi tempi"
        )

        val carouselAdapter = object : RecyclerView.Adapter<CarouselVH>() {
            override fun getItemCount() = Int.MAX_VALUE
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
                CarouselVH(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_carousel_button, parent, false) as FrameLayout
                )
            override fun onBindViewHolder(holder: CarouselVH, position: Int) {
                val idx = position % levels.size
                holder.bind(
                    level       = levels[idx],
                    description = descriptions[idx],
                    info        = infos[idx],
                    tip         = tips[idx]
                )
            }
        }

        binding.buttonCarousel.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = carouselAdapter
            PagerSnapHelper().attachToRecyclerView(this)
            currentPosition = Int.MAX_VALUE / 2 - (Int.MAX_VALUE / 2 % levels.size)
            scrollToPosition(currentPosition)
        }
        binding.Myprogression.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_home_to_progressionFragment)
        }

        startCarouselTimer()
    }

    private fun startCarouselTimer() {
        carouselTimer?.cancel()
        carouselTimer = timer(
            initialDelay = 800L,
            period       = AUTO_SCROLL_INTERVAL
        ) {
            activity?.runOnUiThread {
                currentPosition++
                binding.buttonCarousel.smoothScrollToPosition(currentPosition)
            }
        }
    }

    private inner class CarouselVH(itemView: FrameLayout) : RecyclerView.ViewHolder(itemView) {
        private val frontCard: MaterialCardView   = itemView.findViewById(R.id.front)
        private val frontTitle: TextView         = itemView.findViewById(R.id.front_title)
        private val frontDescription: TextView   = itemView.findViewById(R.id.front_description)
        private val backContainer: FrameLayout   = itemView.findViewById(R.id.back)
        private val backText: TextView           = itemView.findViewById(R.id.back_text)
        private val backTip: TextView            = itemView.findViewById(R.id.back_tip)
        private val backBtn: MaterialButton      = itemView.findViewById(R.id.back_button)

        fun bind(level: String, description: String, info: String, tip: String) {
            // FRONTE
            frontTitle.text       = level
            frontDescription.text = description
            frontCard.visibility  = View.VISIBLE

            // RETRO
            backText.text         = info
            backTip.text          = tip
            backContainer.visibility = View.GONE

            val bgRes = when (level) {
                "Easy"   -> R.drawable.card_runner_green
                "Medium" -> R.drawable.card_runner_blue
                "Hard"   -> R.drawable.card_runner_red
                "Insane" -> R.drawable.card_runner_purple
                else     -> R.drawable.card_runner_blue
            }
            backContainer.setBackgroundResource(bgRes)

            frontCard.setOnClickListener { flipCard(showBack = true) }
            backContainer.setOnClickListener { flipCard(showBack = false) }
            backBtn.setOnClickListener {
                Toast.makeText(itemView.context, tip, Toast.LENGTH_SHORT).show()
            }
        }

        private fun flipCard(showBack: Boolean) {
            carouselTimer?.cancel()

            val scaleUp = ObjectAnimator.ofPropertyValuesHolder(
                itemView,
                PropertyValuesHolder.ofFloat("scaleX", 1f, 1.1f),
                PropertyValuesHolder.ofFloat("scaleY", 1f, 1.1f)
            ).apply { duration = 200 }

            val flipOut = ObjectAnimator.ofFloat(itemView, "rotationY", 0f, 90f).apply { duration = 150 }
            flipOut.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    frontCard.visibility  = if (showBack) View.GONE else View.VISIBLE
                    backContainer.visibility = if (showBack) View.VISIBLE else View.GONE
                    if (!showBack) startCarouselTimer()
                }
            })

            val flipIn = ObjectAnimator.ofFloat(itemView, "rotationY", -90f, 0f).apply { duration = 150 }
            val scaleDown = ObjectAnimator.ofPropertyValuesHolder(
                itemView,
                PropertyValuesHolder.ofFloat("scaleX", 1.1f, 1f),
                PropertyValuesHolder.ofFloat("scaleY", 1.1f, 1f)
            ).apply { duration = 200 }

            AnimatorSet().apply {
                play(scaleUp)
                playSequentially(flipOut, flipIn)
                play(scaleDown).after(flipIn)
                start()
            }
        }
        val today = Date()
        val displayFmt = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
        val keyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        binding.bannerDate.text = displayFmt.format(today)
        val todayId = keyFmt.format(today)
        Log.d(TAG, "TodayId = $todayId")

        val viewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        val user = auth.currentUser
        if (user == null) {
            Log.d(TAG, "Utente non loggato")
            binding.bannerStatus.visibility = View.GONE
            setAllGone()
            return
        }

        val prefs = requireActivity().getSharedPreferences("user_data", Context.MODE_PRIVATE)
        val isTrainer = prefs.getBoolean("is_trainer", false)
        if (isTrainer) {
            Log.d(TAG, "Accesso come PT")
            binding.bannerStatus.visibility = View.GONE
            showButtonsForPT()
            return
        }

        showButtonsForUser()

        val fullName = prefs.getString("saved_display_name", "").orEmpty()
        if (fullName.isBlank()) {
            Log.e(TAG, "FullName mancante")
            binding.bannerStatus.text = "Errore controllo scheda"
            return
        }

        viewModel.combinedStatus.observe(viewLifecycleOwner) {
            binding.bannerStatus.text = it
        }

        // Carica i dati per entrambi i banner
        viewModel.loadData(fullName, todayId)
    }

    private fun setAllGone() {
        binding.buttonForTheScheduleIDid.visibility = View.GONE
        binding.buttonForTheSchedulePersonalTrainerDid.visibility = View.GONE
        binding.buttonForPersonalTrainer.visibility = View.GONE
        binding.buttonInutile.visibility = View.VISIBLE
        binding.buttonInutile.strokeColor = ColorStateList.valueOf(
            ContextCompat.getColor(requireContext(), R.color.orange)
        )
        binding.buttonInutile.text = "Effettua il login per accedere al servizio"
        binding.buttonInutile.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_home_to_navigation_account)
        }
        binding.buttonForPersonalTrainer.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_home_to_navigation_myautocalendar)
        }
    }

    private fun showButtonsForUser() {
        val prefs = requireActivity().getSharedPreferences("user_data", Context.MODE_PRIVATE)
        val fullName = prefs.getString("saved_display_name", "").orEmpty()

        binding.buttonForPersonalTrainer.visibility = View.GONE
        binding.buttonForTheScheduleIDid.visibility = View.VISIBLE
        binding.buttonForTheSchedulePersonalTrainerDid.visibility = View.VISIBLE

        binding.buttonForTheScheduleIDid.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_home_to_navigation_myautocalendar)
        }

        binding.buttonForTheSchedulePersonalTrainerDid.setOnClickListener {
            if (fullName.isBlank()) {
                Toast.makeText(context, "Nome utente non disponibile", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val bundle = Bundle().apply {
                putString("selectedUser", fullName)
            }
            findNavController().navigate(
                R.id.action_navigation_home_to_pt_schedule,
                bundle


    private fun showButtonsForPT() {
        binding.buttonForPersonalTrainer.visibility = View.VISIBLE
        binding.buttonForTheScheduleIDid.visibility = View.GONE
        binding.buttonForTheSchedulePersonalTrainerDid.visibility = View.GONE

        binding.buttonForPersonalTrainer.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_home_to_navigation_myautocalendar)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        carouselTimer?.cancel()
        _binding = null
    }
}

