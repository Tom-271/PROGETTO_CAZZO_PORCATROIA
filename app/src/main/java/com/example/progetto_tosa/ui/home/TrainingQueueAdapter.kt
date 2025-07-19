package com.example.progetto_tosa.ui.home

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.progetto_tosa.R
import java.util.Collections

class TrainingQueueAdapter(
    private val exercises: MutableList<ScheduledExercise>,
    private val startDragListener: (RecyclerView.ViewHolder) -> Unit
) : RecyclerView.Adapter<TrainingQueueAdapter.ExerciseViewHolder>(),
    ItemMoveCallback.ItemTouchHelperContract {

    inner class ExerciseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtName: TextView = itemView.findViewById(R.id.txtExerciseName)
        val handle: ImageView = itemView.findViewById(R.id.dragHandle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_exercise_queue, parent, false)
        return ExerciseViewHolder(view)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: ExerciseViewHolder, position: Int) {
        val exercise = exercises[position]
        holder.txtName.text = exercise.nome // attenzione al nome del campo!

        holder.handle.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                startDragListener(holder)
            }
            false
        }
    }

    override fun getItemCount(): Int = exercises.size

    override fun onRowMoved(fromPosition: Int, toPosition: Int) {
        Collections.swap(exercises, fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)
    }

    override fun onRowSelected(viewHolder: RecyclerView.ViewHolder) {
        viewHolder.itemView.alpha = 0.7f
    }

    override fun onRowClear(viewHolder: RecyclerView.ViewHolder) {
        viewHolder.itemView.alpha = 1.0f
    }

    fun updateExercises(newExercises: List<ScheduledExercise>) {
        exercises.clear()
        exercises.addAll(newExercises)
        notifyDataSetChanged()
    }

    fun getCurrentList(): List<ScheduledExercise> {
        return exercises // o come si chiama la tua lista interna
    }
}
