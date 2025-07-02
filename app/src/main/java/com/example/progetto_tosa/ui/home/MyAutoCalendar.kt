package com.example.progetto_tosa.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.progetto_tosa.R
import com.example.progetto_tosa.databinding.FragmentMyautocalendarBinding
import java.text.SimpleDateFormat
import java.util.*

class MyAutoCalendar : Fragment() {

    private var _binding: FragmentMyautocalendarBinding? = null
    private val binding get() = _binding!!
    private var weekOffset = 0

    private var dateId: String? = null
    private val daysTextViews by lazy {
        listOf(
            binding.cardMonday,
            binding.cardTuesday,
            binding.cardWednesday,
            binding.cardThursday,
            binding.cardFriday,
            binding.cardSaturday,
            binding.cardSunday
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyautocalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dateId = null
        binding.btnFancy.visibility = View.INVISIBLE

        setupDays()

        binding.btnFancy.setOnClickListener {
            dateId?.let { id ->
                findNavController().navigate(
                    R.id.action_navigation_myautocalendar_to_fragment_my_auto_schedule,
                    bundleOf("selectedDate" to id)
                )
            }
        }

        binding.btnPrevWeek.setOnClickListener {
            weekOffset -= 1
            dateId = null
            binding.btnFancy.visibility = View.INVISIBLE
            setupDays()
        }

        binding.btnNextWeek.setOnClickListener {
            weekOffset += 1
            dateId = null
            binding.btnFancy.visibility = View.INVISIBLE
            setupDays()
        }

    }

    private fun setupDays() {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.add(Calendar.WEEK_OF_YEAR, weekOffset)

        val dateFormatLabel = SimpleDateFormat("EEEE d MMMM", Locale("it", "IT"))
        val dateFormatId = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val dayViews = listOf(
            Triple(binding.cardMonday, binding.textMonday, 0),
            Triple(binding.cardTuesday, binding.textTuesday, 1),
            Triple(binding.cardWednesday, binding.textWednesday, 2),
            Triple(binding.cardThursday, binding.textThursday, 3),
            Triple(binding.cardFriday, binding.textFriday, 4),
            Triple(binding.cardSaturday, binding.textSaturday, 5),
            Triple(binding.cardSunday, binding.textSunday, 6)
        )

        dayViews.forEach { (card, textView, dayOffset) ->
            val dayCalendar = calendar.clone() as Calendar
            dayCalendar.add(Calendar.DAY_OF_YEAR, dayOffset)

            val label = dateFormatLabel.format(dayCalendar.time)
                .replaceFirstChar { it.uppercase() }
            val id = dateFormatId.format(dayCalendar.time)

            textView.text = label

            card.setOnClickListener {
                dateId = id
                binding.btnFancy.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        dateId = null
        binding.btnFancy.visibility = View.INVISIBLE
        setupDays()
    }

}
