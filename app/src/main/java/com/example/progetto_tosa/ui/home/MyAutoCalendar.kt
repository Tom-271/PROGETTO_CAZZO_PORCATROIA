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

        setupDays()

        binding.btnFancy.setOnClickListener {
            dateId?.let { id ->
                findNavController().navigate(
                    R.id.action_navigation_myautocalendar_to_fragment_my_auto_schedule,
                    bundleOf("selectedDate" to id)
                )
            }
        }
    }

    private fun setupDays() {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)

        val dateFormatLabel = SimpleDateFormat("EEEE d MMMM", Locale("it", "IT"))
        val dateFormatId = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        daysTextViews.forEachIndexed { index, cardView ->
            val currentDate = calendar.time
            val label = dateFormatLabel.format(currentDate).replaceFirstChar { it.uppercase() }
            val id = dateFormatId.format(currentDate)

            // Trova il TextView figlio e aggiorna il testo
            val textView = cardView.findViewById<ViewGroup>(0)
                ?.getChildAt(0) as? android.widget.TextView
            textView?.text = label

            cardView.setOnClickListener {
                dateId = id
                binding.btnFancy.visibility = View.VISIBLE
            }

            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
