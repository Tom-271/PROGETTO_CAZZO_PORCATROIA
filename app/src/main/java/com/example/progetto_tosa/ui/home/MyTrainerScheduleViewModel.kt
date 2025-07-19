package com.example.progetto_tosa.ui.home

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import com.example.progetto_tosa.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class MyTrainerScheduleViewModel : ViewModel() {
    // UI state
    private val _subtitleText = MutableLiveData<String>()
    val subtitleText: LiveData<String> = _subtitleText

    private val _showFillButton = MutableLiveData(false)
    val showFillButton: LiveData<Boolean> = _showFillButton

    val selectedDateId = MutableLiveData<String>()

    private val _showChrono = MutableLiveData(false)
    val showChrono: LiveData<Boolean> = _showChrono

    private val _unifiedExercises = MutableLiveData<List<Triple<String, String, String>>>(emptyList())
    val unifiedExercises: LiveData<List<Triple<String, String, String>>> = _unifiedExercises

    // internals
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    lateinit var dateId: String
    lateinit var selectedUserId: String

    private var remaining = 0

    private val CHANNEL_ID = "trainer_channel"
    private val notificationId = 2001
    private val NOTIF_PERMISSION_REQUEST = 101

    fun setArgs(selectedUserId: String, dateId: String) {
        this.selectedUserId = selectedUserId
        selectedDateId.value = dateId
    }

    fun initialize(context: Context) {
        selectedDateId.observeForever { dateStr ->
            createNotificationChannel(context)
            formatSubtitle(dateStr)
            checkUserRole()
            fetchUnifiedList()
        }
    }

    private fun formatSubtitle(dateStr: String) {
        val inFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val parsedDate = inFmt.parse(dateStr) ?: return

        val dayNameFmt = SimpleDateFormat("EEEE", Locale.getDefault())     // lunedì
        val dayNumberFmt = SimpleDateFormat("d", Locale.getDefault())      // 19
        val monthFmt = SimpleDateFormat("MMMM", Locale.getDefault())       // luglio

        val nomeGiorno = dayNameFmt.format(parsedDate).uppercase(Locale.getDefault()) // → LUNEDÌ
        val numeroGiorno = dayNumberFmt.format(parsedDate)                            // → 19
        val mese = monthFmt.format(parsedDate).uppercase(Locale.getDefault())         // → LUGLIO

        val todayFmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val oggi = todayFmt.format(Date())
        val dataConfronto = todayFmt.format(parsedDate)

        _subtitleText.value = if (dataConfronto == oggi)
            "OGGI HAI QUESTA SCHEDA DAL PT:"
        else
            "SCHEDA PER $nomeGiorno $numeroGiorno $mese DAL PT"
    }

    private fun checkUserRole() {
        viewModelScope.launch(Dispatchers.IO) {
            val uid = auth.currentUser?.uid ?: return@launch
            val isPTinUsers = db.collection("users").document(uid).get().await().getBoolean("isPersonalTrainer") == true
            val isPTinPTs   = db.collection("personal_trainers").document(uid).get().await().getBoolean("isPersonalTrainer") == true
            if (isPTinUsers || isPTinPTs) {
                _showFillButton.postValue(true)
                _showChrono.postValue(false)
            } else {
                _showFillButton.postValue(false)
                _showChrono.postValue(true)
            }
        }
    }

    private fun fetchUnifiedList() {
        if (!::dateId.isInitialized) {
            // evita crash se chiamata troppo presto
            return
        }

        val cats = listOf("bodybuilding", "cardio", "corpo-libero", "stretching")
        val tmp = mutableListOf<Triple<String, String, String>>()
        var completed = 0
        for (cat in cats) {
            db.collection("schede_del_pt")
                .document(selectedUserId)
                .collection(dateId)
                .document(cat)
                .collection("esercizi")
                .get()
                .addOnSuccessListener { snap ->
                    snap.documents.forEach { d ->
                        val nome = d.getString("nomeEsercizio") ?: d.id
                        tmp += Triple(nome, cat, d.id)
                    }
                }
                .addOnCompleteListener {
                    completed++
                    if (completed == cats.size) {
                        remaining = tmp.size
                        _unifiedExercises.value = tmp
                    }
                }
        }
    }

    fun renderUnifiedList(container: ViewGroup, esercizi: List<Triple<String, String, String>>) {
        container.removeAllViews()
        val inflater = LayoutInflater.from(container.context)
        val categorieOrd = listOf("bodybuilding", "cardio", "corpo-libero", "stretching")
        val grouped = esercizi.groupBy { it.second }

        for (categoria in categorieOrd) {
            grouped[categoria]?.let { lista ->
                // titolo categoria
                val titolo = TextView(container.context).apply {
                    text = categoria.uppercase()
                    setTextColor(ContextCompat.getColor(context, R.color.orange))
                    textSize = 18f
                    setPadding(36, 24, 8, 8)
                    setTypeface(typeface)
                }
                container.addView(titolo)

                lista.forEach { (nome, cat, docId) ->
                    // riga orizzontale
                    val row = LinearLayout(container.context).apply {
                        orientation = LinearLayout.HORIZONTAL
                        setPadding(36, 4, 8, 16)
                    }
                    val text = TextView(container.context).apply {
                        this.text = "○ $nome"
                        setTextColor(ContextCompat.getColor(context, android.R.color.white))
                        layoutParams = LinearLayout.LayoutParams(
                            0,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            1f
                        )
                    }
                    val infoBtn = ImageButton(container.context).apply {
                        setImageResource(R.drawable.down)
                        background = null
                        tag = false
                    }
                    val detailCard = inflater.inflate(R.layout.exercise_info_card, container, false) as CardView
                    detailCard.visibility = View.GONE

                    // campi dentro detailCard
                    val titleTv     = detailCard.findViewById<TextView>(R.id.cardExerciseTitle).also { it.text = nome }
                    val setsRepsTv  = detailCard.findViewById<TextView>(R.id.cardSetsReps)
                    val weightInput = detailCard.findViewById<EditText>(R.id.cardWeightInput)
                    val saveBtn     = detailCard.findViewById<android.widget.Button>(R.id.cardSaveButton)

                    // carica serie/ripetizioni/peso
                    db.collection("schede_del_pt")
                        .document(selectedUserId)
                        .collection(selectedDateId.value!!)
                        .document(cat)
                        .collection("esercizi")
                        .document(docId)
                        .get()
                        .addOnSuccessListener { d ->
                            val serie = d.getLong("numeroSerie")?.toString() ?: "-"
                            val rip   = d.getLong("numeroRipetizioni")?.toString() ?: "-"
                            setsRepsTv.text = "serie: $serie  |  ripetizioni: $rip"
                            setsRepsTv.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.tick, 0)
                            setsRepsTv.compoundDrawablePadding = 16
                            d.getDouble("peso")?.let { weightInput.setText(it.toString()) }

                            setsRepsTv.setOnClickListener {
                                // elimina esercizio
                                db.collection("schede_del_pt")
                                    .document(selectedUserId)
                                    .collection(selectedDateId.value!!)
                                    .document(cat)
                                    .collection("esercizi")
                                    .document(docId)
                                    .delete()
                                    .addOnSuccessListener {
                                        container.removeView(row)
                                        container.removeView(detailCard)
                                        remaining--
                                        if (remaining == 0) {
                                            sendCompletionNotification(container.context)
                                        }
                                    }
                            }
                        }

                    // toggle dettagli
                    infoBtn.setOnClickListener { v ->
                        val open = v.tag as Boolean
                        detailCard.visibility = if (open) View.GONE else View.VISIBLE
                        (v as ImageButton).setImageResource(if (open) R.drawable.down else R.drawable.up)
                        v.tag = !open
                    }

                    // salva peso
                    saveBtn.setOnClickListener {
                        val peso = weightInput.text.toString().toFloatOrNull()
                        if (peso == null) {
                            Toast.makeText(container.context, "inserisci un peso valido", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                        db.collection("schede_del_pt")
                            .document(selectedUserId)
                            .collection(selectedDateId.value!!)
                            .document(cat)
                            .collection("esercizi")
                            .document(docId)
                            .set(mapOf("peso" to peso), SetOptions.merge())
                    }

                    row.addView(text)
                    row.addView(infoBtn)
                    container.addView(row)
                    container.addView(detailCard)
                }
            }
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "trainer", NotificationManager.IMPORTANCE_HIGH)
            channel.description = "notifiche pt"
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    fun sendCompletionNotification(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                context as android.app.Activity,
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                NOTIF_PERMISSION_REQUEST
            )
            return
        }
        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("scheda pt")
            .setContentText("hai completato la scheda del pt!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(notificationId, notif)
    }
}
