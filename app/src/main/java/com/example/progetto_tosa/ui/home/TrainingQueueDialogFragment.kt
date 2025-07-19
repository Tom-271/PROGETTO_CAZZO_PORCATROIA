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

class TrainingQueueDialogFragment(
    private val exercises: MutableList<ScheduledExercise>,
    private val viewModel: MyAutoScheduleViewModel,
    private val onExercisesReordered: (List<ScheduledExercise>) -> Unit
) : DialogFragment() {

    private var _binding: CardTrainingQueueBinding? = null
    private val binding get() = _binding!!

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

        binding.buttonExit.setOnClickListener {
            dismiss()
        }

        binding.SaveOrder.setOnClickListener {
            val reorderedList = adapter.getCurrentList()
            val context = requireContext().applicationContext

            viewModel.saveReorderedExercises(reorderedList)

            Toast.makeText(context, "Ordine salvato", Toast.LENGTH_SHORT).show()
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
            return TrainingQueueDialogFragment(exercises.toMutableList(), viewModel, onExercisesReordered)
        }
    }
}
