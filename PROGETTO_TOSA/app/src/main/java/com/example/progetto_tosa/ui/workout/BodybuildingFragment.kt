package com.example.progetto_tosa.ui.workout

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.progetto_tosa.R
import com.google.android.material.card.MaterialCardView

class BodybuildingFragment : Fragment(R.layout.fragment_bodybuilding) {

    data class Exercise(
        val imageRes: Int,
        val descriptionImage: Int,
        val title: String,
        val subtitle: String,
        val videoUrl: String,
        val description: String,
        val subtitle2: String,
        val description2: String,
        val detailImage1Res: Int,
        val detailImage2Res: Int,
        val descrizioneTotale:String
    )

    private val sharedImage = R.drawable.pancadescrizione

    private val section1 = listOf(
        Exercise(
            imageRes = R.drawable.pancadescrizione,
            descriptionImage = R.drawable.plank,
            title = "PANCA PIANA",
            subtitle = "PANCA PIANA",
            videoUrl = "https://www.youtube.com/watch?v=nclAIgM4NJE",
            description = "La panca piana è un esercizio fondamentale per lo sviluppo della forza e della massa muscolare del petto. Ideale per migliorare la spinta e la stabilità della parte superiore del corpo.",
            subtitle2 = "MUSCOLI COINVOLTI",
            description2 = "- Grande pettorale\n- Deltoide anteriore\n- Tricipite brachiale",
            detailImage1Res = R.drawable.pancadescrizione,
            detailImage2Res = R.drawable.pancadescrizione,
            descrizioneTotale = "Per l’ipertrofia del pettorale si consigliano 3–4 serie da 8–12 ripetizioni (carico 65–75% 1RM) per esercizio (panca piana, panca inclinata, croci), con pause di 60–90 s e due sedute settimanali. Varia angoli e impugnature per sollecitare tutte le fibre."
        ),
        Exercise(
            imageRes = R.drawable.pancadescrizione,
            descriptionImage = R.drawable.petto,
            title = "SPINTE",
            subtitle = "Spinte con manubri su panca inclinata",
            videoUrl = "https://youtu.be/VIDEO_ID_SPINTE",
            description = "prova",
            subtitle2 = "MUSCOLI COINVOLTI",
            description2 = "- Grande pettorale\n- Deltoide anteriore\n- Tricipite brachiale",
            detailImage1Res = sharedImage,
            detailImage2Res = sharedImage,
            descrizioneTotale = "Per l’ipertrofia del pettorale si consigliano 3–4 serie da 8–12 ripetizioni (carico 65–75% 1RM) per esercizio (panca piana, panca inclinata, croci), con pause di 60–90 s e due sedute settimanali. Varia angoli e impugnature per sollecitare tutte le fibre."

        ),
        Exercise(
            imageRes = R.drawable.pancadescrizione,
            descriptionImage = R.drawable.pancadescrizione,
            title = "CROCI AI CAVI",
            subtitle = "Croci ai cavi",
            videoUrl = "https://youtu.be/VIDEO_ID_CROCI",
            description = "prova",
            subtitle2 = "MUSCOLI COINVOLTI",
            description2 = "Muscoli coinvolti: grande pettorale, deltoide anteriore, tricipite...",
            detailImage1Res = sharedImage,
            detailImage2Res = sharedImage,
            descrizioneTotale = "Per l’ipertrofia del pettorale si consigliano 3–4 serie da 8–12 ripetizioni (carico 65–75% 1RM) per esercizio (panca piana, panca inclinata, croci), con pause di 60–90 s e due sedute settimanali. Varia angoli e impugnature per sollecitare tutte le fibre."

        ),
        Exercise(
            imageRes = R.drawable.pancadescrizione,
            descriptionImage = R.drawable.pancadescrizione,
            title = "CHEST PRESS",
            subtitle = "Macchina Chest Press",
            videoUrl = "https://youtu.be/VIDEO_ID_CHESTPRESS",
            description = "prova",
            subtitle2 = "MUSCOLI COINVOLTI",
            description2 = "Muscoli coinvolti: grande pettorale, deltoide anteriore, tricipite...",
            detailImage1Res = sharedImage,
            detailImage2Res = sharedImage,
            descrizioneTotale = "Per l’ipertrofia del pettorale si consigliano 3–4 serie da 8–12 ripetizioni (carico 65–75% 1RM) per esercizio (panca piana, panca inclinata, croci), con pause di 60–90 s e due sedute settimanali. Varia angoli e impugnature per sollecitare tutte le fibre."

        ),
        Exercise(
            imageRes = R.drawable.pancadescrizione,
            descriptionImage = R.drawable.pancadescrizione,
            title = "PECTORAL MACHINE",
            subtitle = "Pectoral Machine",
            videoUrl = "https://youtu.be/VIDEO_ID_PECTORAL",
            description = "prova",
            subtitle2 = "MUSCOLI COINVOLTI",
            description2 = "Muscoli coinvolti: grande pettorale, deltoide anteriore, tricipite...",
            detailImage1Res = sharedImage,
            detailImage2Res = sharedImage,
            descrizioneTotale = "Per l’ipertrofia del pettorale si consigliano 3–4 serie da 8–12 ripetizioni (carico 65–75% 1RM) per esercizio (panca piana, panca inclinata, croci), con pause di 60–90 s e due sedute settimanali. Varia angoli e impugnature per sollecitare tutte le fibre."

        )
    )

    private val section2 = listOf(section1[0])
    private val section3 = listOf(section1[0])
    private val section4 = listOf(section1[0])
    private val section5 = listOf(section1[0])

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyStrokeColor(view)
        setupSection(R.id.cardSection1, R.id.rvSection1, section1)
        setupSection(R.id.cardSection2, R.id.rvSection2, section2)
        setupSection(R.id.cardSection3, R.id.rvSection3, section3)
        setupSection(R.id.cardSection4, R.id.rvSection4, section4)
        setupSection(R.id.cardSection5, R.id.rvSection5, section5)
    }

    override fun onResume() {
        super.onResume()
        applyStrokeColor(requireView())
    }

    private fun applyStrokeColor(root: View) {
        val night = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val colorRes = if (night == Configuration.UI_MODE_NIGHT_YES) R.color.white else R.color.black
        val strokeColor = ContextCompat.getColor(requireContext(), colorRes)
        listOf(
            R.id.cardSection1,
            R.id.cardSection2,
            R.id.cardSection3,
            R.id.cardSection4,
            R.id.cardSection5
        ).forEach { id ->
            root.findViewById<MaterialCardView>(id).setStrokeColor(strokeColor)
        }
    }

    private fun setupSection(headerId: Int, recyclerId: Int, data: List<Exercise>) {
        val headerCard = requireView().findViewById<MaterialCardView>(headerId)
        val recyclerView = requireView().findViewById<RecyclerView>(recyclerId)
        headerCard.setOnClickListener {
            recyclerView.visibility = if (recyclerView.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        recyclerView.adapter = ExerciseAdapter(data) { ex ->
            ExerciseDetailFragment.newInstance(
                ex.title,
                ex.videoUrl,
                ex.description,
                ex.subtitle2,
                ex.description2,
                ex.detailImage1Res,
                ex.detailImage2Res,
                ex.descriptionImage,
                ex.descrizioneTotale
            ).show((requireActivity() as FragmentActivity).supportFragmentManager, "exercise_detail")
        }
    }

    private inner class ExerciseAdapter(
        private val items: List<Exercise>,
        private val onClick: (Exercise) -> Unit
    ) : RecyclerView.Adapter<ExerciseAdapter.VH>() {

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val card: MaterialCardView = view.findViewById(R.id.cardExercise)
            val imageView: ImageView = view.findViewById(R.id.imageViewExercise)
            val titleIcon: ImageView = view.findViewById(R.id.imageViewTitleIcon)
            val subtitleView: TextView = view.findViewById(R.id.textViewSubtitleExercise)
            val titleViewTop: TextView = view.findViewById(R.id.textViewTitleTop)
            val descrizioneTotale = view.findViewById<TextView>(R.id.descrizioneTotale)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.cards_exercise, parent, false)
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val ex = items[position]
            holder.imageView.setImageResource(ex.imageRes)
            holder.titleIcon.setImageResource(ex.imageRes)
            holder.titleIcon.visibility = View.VISIBLE
            holder.subtitleView.text = ex.subtitle
            holder.titleViewTop.text = ex.title
            holder.card.setOnClickListener { onClick(ex) }
        }

        override fun getItemCount(): Int = items.size
    }
}
