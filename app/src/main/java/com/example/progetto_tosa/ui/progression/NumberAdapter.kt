package com.example.progetto_tosa.ui.progression

import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.progetto_tosa.R
import com.example.progetto_tosa.data.BodyFatEntry
import java.time.LocalDate

/**
 * Adapter per mostrare ultime misurazioni (BF%, Peso, Massa Magra) e gestire rimozioni.
 */
class NumberAdapter(
    entries: List<BodyFatEntry>,
    private val type: EntryType
) : RecyclerView.Adapter<NumberAdapter.VH>() {

    enum class EntryType { BODYFAT, WEIGHT, LEAN }

    // Uso una lista mutabile per poter rimuovere elementi dinamicamente
    private val items: MutableList<BodyFatEntry> = entries.toMutableList()

    /**
     * Restituisce l'elemento alla posizione specificata
     */
    fun getItemAt(position: Int): BodyFatEntry = items[position]

    /**
     * Rimuove l'elemento dalla lista e notifica l'adapter
     */
    fun removeItem(position: Int) {
        items.removeAt(position)
        notifyItemRemoved(position)
    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCombined: TextView = itemView.findViewById(android.R.id.text1)
        private val tvDate: TextView    = itemView.findViewById(android.R.id.text2)

        fun bind(entry: BodyFatEntry) {
            // 1) valore + unità
            val rawValue = when (type) {
                EntryType.BODYFAT -> entry.bodyFatPercent?.let { "%.1f%%".format(it) } ?: "-"
                EntryType.WEIGHT  -> entry.bodyWeightKg?.let  { "%.1f kg".format(it) }  ?: "-"
                EntryType.LEAN    -> entry.leanMassKg?.let    { "%.1f kg".format(it) }  ?: "-"
            }

            // 2) data formattata
            val date = LocalDate.ofEpochDay(entry.epochDay)
            val rawDate = "%02d/%02d/%04d".format(
                date.dayOfMonth, date.monthValue, date.year
            )

            // 3) colore grigio da risorse
            val gray = ContextCompat.getColor(itemView.context, R.color.light_gray)

            // 4) costruisco lo SpannableString:
            //    - valore in grassetto +20% size
            //    - separator ' – '
            //    - data in corsivo -10% size e colore grigio
            val combinedText = "\$rawValue  –  \$rawDate".replace("\$rawValue", rawValue).replace("\$rawDate", rawDate)
            val spannable = SpannableString(combinedText).apply {
                // grassetto + relativo size per il valore
                setSpan(StyleSpan(Typeface.BOLD),
                    0, rawValue.length,
                    SPAN_EXCLUSIVE_EXCLUSIVE)
                setSpan(RelativeSizeSpan(1.2f),
                    0, rawValue.length,
                    SPAN_EXCLUSIVE_EXCLUSIVE)
                // corsivo + relativo size + colore per la data
                val startDate = rawValue.length + 4
                setSpan(StyleSpan(Typeface.ITALIC),
                    startDate, combinedText.length,
                    SPAN_EXCLUSIVE_EXCLUSIVE)
                setSpan(RelativeSizeSpan(0.9f),
                    startDate, combinedText.length,
                    SPAN_EXCLUSIVE_EXCLUSIVE)
                setSpan(ForegroundColorSpan(gray),
                    startDate, combinedText.length,
                    SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            tvCombined.text = spannable
            tvDate.visibility = View.GONE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}