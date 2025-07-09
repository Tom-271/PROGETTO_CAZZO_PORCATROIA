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
import androidx.navigation.fragment.findNavController
import com.example.progetto_tosa.R
import com.example.progetto_tosa.databinding.FragmentMyTrainerScheduleBinding

class MyTrainerSchedule : Fragment(R.layout.fragment_my_trainer_schedule) {

    private var _binding: FragmentMyTrainerScheduleBinding? = null
    private val binding get() = _binding!!

    private val vm: MyTrainerScheduleViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
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

        // 5) Avvia tutto
        vm.initialize(requireContext())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
