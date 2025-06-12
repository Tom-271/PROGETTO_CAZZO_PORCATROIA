package com.example.progetto_tosa.ui.workout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
import android.widget.ViewFlipper
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.progetto_tosa.R
import com.example.progetto_tosa.databinding.FragmentWorkoutBinding

class WorkoutFragment : Fragment() {

    private var _binding: FragmentWorkoutBinding? = null
    private val binding get() = _binding!!

    // Lista di "tip" da mostrare nel ViewFlipper
    private val tips = listOf(
        "    Ricorda di idratarti spesso durante la giornata",
        "Il preworkout aiuta a sostenerti durante l'allenamento",
        "  Mantieni il core sempre contratto durante gli squat",
        "           Fare stretching aiuta nell'evitare infortuni",
        "   La tecnica è più importante del peso che sollevi!",
        "    Chiedi aiuto al personal trainer se hai domande!",
        "              Mantieni un'alimentazione adeguata!"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        // Inflate e binding
        _binding = FragmentWorkoutBinding.inflate(inflater, container, false)
        val root = binding.root

        // Navigazione ai fragment
        binding.btnItem1.setOnClickListener {
            findNavController().navigate(R.id.action_workout_to_bodybuilding)
        }
        binding.btnItem2.setOnClickListener {
            findNavController().navigate(R.id.action_workout_to_corpolibero)
        }
        binding.btnItem3.setOnClickListener {
            findNavController().navigate(R.id.action_workout_to_cardio)
        }
        binding.btnItem4.setOnClickListener {
            findNavController().navigate(R.id.action_workout_to_stretching)
        }

        // Configurazione del ViewFlipper per i "Tips"
        val vfTips: ViewFlipper = binding.vfTips
        for (tip in tips) { //dipende dalla lunghezza totale della lista di tips
            val tv = TextView(requireContext()).apply {
                text = tip
            }
            vfTips.addView(tv)
        }
        vfTips.flipInterval = 4000
        vfTips.inAnimation = AnimationUtils.loadAnimation(requireContext(), android.R.anim.slide_in_left)
        vfTips.outAnimation = AnimationUtils.loadAnimation(requireContext(), android.R.anim.slide_out_right)
        vfTips.isAutoStart = true
        vfTips.startFlipping()

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
