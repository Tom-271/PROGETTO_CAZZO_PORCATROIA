package com.example.progetto_tosa.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.progetto_tosa.R
import com.example.progetto_tosa.databinding.FragmentMyTrainerScheduleBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.view.isEmpty

class MyTrainerSchedule : Fragment(R.layout.fragment_my_trainer_schedule) {

    private var _binding: FragmentMyTrainerScheduleBinding? = null
    private val binding get() = _binding!!

    private lateinit var vm: MyTrainerScheduleViewModel

    private var currentDate = Date()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // ⚠️ INIZIALIZZA PRIMA IL VIEWMODEL
        vm = ViewModelProvider(this)[MyTrainerScheduleViewModel::class.java]

        _binding = FragmentMyTrainerScheduleBinding.inflate(inflater, container, false)
        binding.vm = vm
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1) Imposta args
        vm.setArgs(
            selectedUserId = requireArguments().getString("selectedUser")
                ?: error("selectedUser mancante"),
            dateId = requireArguments().getString("selectedDate")
                ?: error("selectedDate mancante")
        )
        // 2) Cronometro
        binding.chrono.setOnClickListener {
            findNavController().navigate(R.id.action_fragment_my_trainer_schedule_to_navigation_cronotimer)
        }

        // 3) Osservatori ViewModel
        vm.showFillButton.observe(viewLifecycleOwner) { show ->
            binding.btnFillSchedule.visibility = if (show) VISIBLE else GONE
        }
        vm.subtitleText.observe(viewLifecycleOwner) { text ->
            binding.subtitleAllExercises.text = text
            binding.subtitleAllExercises.visibility = VISIBLE
        }
        vm.showChrono.observe(viewLifecycleOwner) { show ->
            binding.chrono.visibility = if (show) VISIBLE else GONE
        }
        vm.unifiedExercises.observe(viewLifecycleOwner) { list ->
            // popola ogni LinearLayout di categoria
            vm.renderUnifiedList(
                container = binding.bodybuildingDetailsContainer,
                esercizi = list.filter { it.second == "bodybuilding" }
            )
            vm.renderUnifiedList(
                container = binding.cardioDetailsContainer,
                esercizi = list.filter { it.second == "cardio" }
            )
            vm.renderUnifiedList(
                container = binding.corpoliberoDetailsContainer,
                esercizi = list.filter { it.second == "corpo-libero" }
            )
            vm.renderUnifiedList(
                container = binding.stretchingDetailsContainer,
                esercizi = list.filter { it.second == "stretching" }
            )

            // mostra il messaggio se tutte le categorie sono vuote
            val isEmpty =
                binding.bodybuildingDetailsContainer.isEmpty() &&
                        binding.cardioDetailsContainer.isEmpty() &&
                        binding.corpoliberoDetailsContainer.isEmpty() &&
                        binding.stretchingDetailsContainer.isEmpty()

            binding.emptyMessage.visibility = if (isEmpty) View.VISIBLE else View.GONE
        }

        // 4) Click “riempi scheda”
        binding.btnFillSchedule.setOnClickListener {
            findNavController().navigate(
                R.id.action_fragment_my_trainer_schedule_to_fragment_workout,
                bundleOf(
                    "selectedUser" to vm.selectedUserId,
                    "selectedDate" to vm.dateId
                )
            )
        }

        binding.arrowLeft.setOnClickListener {
            changeDayBy(-1) // giorno precedente
        }

        binding.arrowRight.setOnClickListener {
            changeDayBy(1) // giorno successivo
        }

        setupBannerDate()

        // 5) Avvia tutto
        vm.initialize(requireContext())
    }

    private fun changeDayBy(days: Int) {
        val calendar = java.util.Calendar.getInstance()
        calendar.time = currentDate
        calendar.add(java.util.Calendar.DATE, days)
        currentDate = calendar.time

        val newDateId = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(currentDate)
        vm.selectedDateId.value = newDateId // 🔁 trigger aggiornamento automatico

        setupBannerDate()
    }

    private fun setupBannerDate() {
        val dayNameFmt = SimpleDateFormat("EEEE", Locale.getDefault()) // es. lunedì
        val dayNumberFmt = SimpleDateFormat("d", Locale.getDefault())   // es. 19
        val monthFmt = SimpleDateFormat("MMMM", Locale.getDefault())   // es. luglio

        val nomeGiorno = dayNameFmt.format(currentDate).uppercase(Locale.getDefault()) // → LUNEDÌ
        val numeroGiorno = dayNumberFmt.format(currentDate)                            // → 19
        val mese = monthFmt.format(currentDate).uppercase(Locale.getDefault())         // → LUGLIO

        val testo = "SCHEDA PER $nomeGiorno $numeroGiorno $mese DAL PT"
        binding.subtitleAllExercises.text = testo
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
