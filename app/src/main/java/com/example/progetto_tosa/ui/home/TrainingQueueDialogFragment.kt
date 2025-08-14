package com.example.progetto_tosa.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.progetto_tosa.databinding.CardTrainingQueueBinding

class TrainingQueueDialogFragment : DialogFragment() {

    private var _binding: CardTrainingQueueBinding? = null
    private val binding get() = _binding!!

    // campi impostati da newInstance(...)
    private var exercises: MutableList<ScheduledExercise> = mutableListOf()
    private lateinit var viewModel: MyAutoScheduleViewModel
    private var onExercisesReordered: ((List<ScheduledExercise>) -> Unit)? = null
    private var selectedUser: String? = null  // Aggiungi il campo selectedUser

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = CardTrainingQueueBinding.inflate(inflater, container, false)

        lateinit var itemTouchHelper: ItemTouchHelper

        val adapter = TrainingQueueAdapter(exercises) { viewHolder ->
            itemTouchHelper.startDrag(viewHolder)
        }

        binding.recyclerTrainingQueue.layoutManager = LinearLayoutManager(
            requireContext(), LinearLayoutManager.VERTICAL, false
        )
        binding.recyclerTrainingQueue.adapter = adapter

        val callback = ItemMoveCallback(adapter)
        itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(binding.recyclerTrainingQueue)

        binding.buttonExit.setOnClickListener { dismiss() }

        binding.SaveOrder.setOnClickListener {
            val reordered = adapter.getCurrentList()
            viewModel.saveReorderedExercises(reordered)
            onExercisesReordered?.invoke(reordered)
            Toast.makeText(requireContext(), "Ordine salvato", Toast.LENGTH_SHORT).show()
        }

        binding.StartTraining.setOnClickListener {
            val reordered = adapter.getCurrentList()
            viewModel.saveReorderedExercises(reordered)
            onExercisesReordered?.invoke(reordered)

            val current = adapter.getCurrentList()
            val date = viewModel.selectedDateId.value
            if (date.isNullOrBlank()) {
                Toast.makeText(requireContext(), "Data non valida", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val category = current.firstOrNull()?.categoria ?: "null"

            // Passa l'intera lista degli esercizi, non solo la categoria del primo esercizio
            val startDialog = TrainingStartDialogFragment.newInstance(
                selectedDate = date,
                category = category,
                exercises = current,  // Lista completa degli esercizi
                selectedUser = selectedUser
            )
            startDialog.show(parentFragmentManager, "start_training_dialog")
            dismiss()
        }


        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(
            exercises: List<ScheduledExercise>,
            viewModel: MyAutoScheduleViewModel,
            onExercisesReordered: (List<ScheduledExercise>) -> Unit
        ): TrainingQueueDialogFragment {
            return TrainingQueueDialogFragment().apply {
                this.exercises = exercises.toMutableList()
                this.viewModel = viewModel
                this.onExercisesReordered = onExercisesReordered
            }
        }
    }
}
