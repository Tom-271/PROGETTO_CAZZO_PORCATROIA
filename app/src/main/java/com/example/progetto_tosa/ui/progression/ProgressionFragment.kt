package com.example.progetto_tosa.ui.progression

import android.Manifest
import android.annotation.SuppressLint
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import com.example.progetto_tosa.chat.* // ChatOverlay, ChatViewModel, ChatVMFactory, ChatRepository, Secrets
import com.google.firebase.Timestamp
import com.example.progetto_tosa.chat.ChatOverlay
import com.example.progetto_tosa.chat.ChatRepository
import com.example.progetto_tosa.chat.ChatViewModel
import com.example.progetto_tosa.chat.ChatVMFactory
import com.example.progetto_tosa.chat.Secrets
import android.view.ViewGroup
import android.graphics.Color
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.formatter.ValueFormatter
import android.widget.LinearLayout
import com.github.mikephil.charting.components.LimitLine
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.text.TextUtils
import android.util.TypedValue
import android.text.SpannableStringBuilder
import android.text.Spannable
import android.text.style.RelativeSizeSpan
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.progetto_tosa.R
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.example.progetto_tosa.ui.progression.ProgressionVmFactory
import com.example.progetto_tosa.ui.progression.ProgressionViewModel
import com.example.progetto_tosa.ui.progression.BleHrService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import java.util.UUID

// 👉 aggiunte per leggere device connessi e nome modello al rientro
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile

class ProgressionFragment : Fragment(R.layout.fragment_progression) {

    companion object {
        private const val REQUEST_BLE_PERMISSIONS = 1001
        private val HR_SERVICE_UUID = UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb")
        private val HR_MEASUREMENT_CHAR_UUID = UUID.fromString("00002A37-0000-1000-8000-00805f9b34fb")
        const val EXTRA_DEVICE_NAME = "extra_device_name"
    }

    // Firebase & BLE (solo scan; la connessione è nel Service)
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val bluetoothAdapter: BluetoothAdapter? by lazy { BluetoothAdapter.getDefaultAdapter() }
    private var isScanning = false
    private var ptDocListener: com.google.firebase.firestore.ListenerRegistration? = null
    private val foundDevices = mutableListOf<BluetoothDevice>()
    private var permissionRequestedToSettings = false
    private lateinit var btnStopConnection: TextView // o Button se preferisci
    private var initialContainerPaddingTop = 0
    private var initialCardContentPaddingTop = 0
    // Chat
    private lateinit var chatOverlay: ChatOverlay
    private lateinit var chatVM: ChatViewModel
    // Chat Gemini


    private val hrScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            result.device?.takeIf { dev -> foundDevices.none { it.address == dev.address } }?.let {
                foundDevices.add(it)
            }
        }
        override fun onScanFailed(errorCode: Int) {
            toast("Scan BLE fallito: codice $errorCode")
            isScanning = false
        }
    }

    // per firebase e la registrazione dei dati registrati
    private var todayEpochDay: Long = java.time.LocalDate.now().toEpochDay()
    private var todayMaxBpm: Int = 0
    private var hasLoadedDailyMax = false
    private lateinit var chartDailyMaxHr: LineChart
    private var hrListener: com.google.firebase.firestore.ListenerRegistration? = null
    private lateinit var btnToggleDetails: LinearLayout
    private lateinit var cardDailyMax: CardView
    private lateinit var textforsecondgraph: TextView
    private lateinit var imgToggleArrow: ImageView
    private lateinit var tvToggleDetails: TextView

    private var detailsExpanded: Boolean = false

    // UI elements
    private lateinit var etWeightGoalValue: EditText
    private lateinit var etLeanGoalValue: EditText
    private lateinit var etBodyFatGoalValue: EditText
    private lateinit var btnConfirm: TextView
    private lateinit var bfSubtitle: TextView
    private lateinit var pesoSubtitle: TextView
    private lateinit var leanSubtitle: TextView
    private lateinit var titoloperconnessione: TextView
    private lateinit var buttonForBF: CardView
    private lateinit var buttonForWEIGHT: CardView
    private lateinit var buttonForMassaMagra: CardView
    private lateinit var tvHeartRate: TextView
    private lateinit var ivHeartPulse: ImageView
    private lateinit var ecgChart: LineChart
    private lateinit var tvBpmValue: TextView
    private lateinit var containerHeart: FrameLayout

    private lateinit var vm: ProgressionViewModel
    private var uid: String? = null
    private var isPtUser = false
    private var editingGoals = false

    // Heart animation & ECG data
    private var heartAnimator: ObjectAnimator? = null
    private val ecgHandler = Handler(Looper.getMainLooper())
    private lateinit var ecgDataSet: LineDataSet
    private var ecgTime = 0f
    private var currentBpm = 60f
    private var nextBeatTime = 0f
    private val sampleIntervalMs = 50L
    private var ecgRunning = false

    // Receiver dal Service
    private val hrReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.action) {
                BleHrService.ACTION_HR_UPDATE -> {
                    val bpm = intent.getIntExtra(BleHrService.EXTRA_BPM, 0)
                    tvBpmValue.visibility = View.VISIBLE
                    ivHeartPulse.visibility = View.VISIBLE

                    val label = "Rilevati: "
                    val bpmText = "$bpm"
                    val unit = " bpm"
                    val ssb = SpannableStringBuilder().apply {
                        append(label)
                        val start = length
                        append(bpmText)
                        setSpan(
                            RelativeSizeSpan(1.8f),
                            start, start + bpmText.length,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        append(unit)
                    }
                    tvBpmValue.text = ssb

                    animateHeart(bpm)
                    currentBpm = bpm.toFloat()
                    scheduleNextBeat()
                    startEcg()

                    updateDailyMaxHeartRate(bpm)
                }

                BleHrService.ACTION_STATE -> {
                    val state = intent.getStringExtra(BleHrService.EXTRA_STATE)
                    when (state) {
                        BleHrService.STATE_CONNECTED -> {
                            titoloperconnessione.visibility = View.GONE

                            // mostra bottone rosso "stop"
                            btnStopConnection.apply {
                                visibility = View.VISIBLE
                                setOnClickListener { stopBleService() }
                            }

                            // padding top del container (46dp)
                            val topPx = TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_DIP, 46f, resources.displayMetrics
                            ).toInt()
                            val left = containerHeart.paddingLeft
                            val right = containerHeart.paddingRight
                            val bottom = containerHeart.paddingBottom
                            containerHeart.setPadding(left, topPx, right, bottom)

                            // padding top dentro la card del grafico (es. 16dp)
                            val cardTopPx = TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_DIP, 16f, resources.displayMetrics
                            ).toInt()
                            cardDailyMax.setContentPadding(
                                cardDailyMax.contentPaddingLeft,
                                cardTopPx,
                                cardDailyMax.contentPaddingRight,
                                cardDailyMax.contentPaddingBottom
                            )

                            // nome modello dal broadcast (fallback su manager)
                            val connectedName =
                                intent.getStringExtra(BleHrService.EXTRA_DEVICE_NAME)
                                    ?.takeIf { it.isNotBlank() }
                                    ?: getFirstConnectedDeviceName()

                            tvHeartRate.apply {
                                text = if (connectedName.isNotEmpty())
                                    "connesso a $connectedName"
                                else
                                    "connesso"
                                setCompoundDrawablesWithIntrinsicBounds(
                                    R.drawable.ic_green_dot, 0, 0, 0
                                )
                                setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
                                ellipsize = TextUtils.TruncateAt.END
                                maxLines = 1
                                isClickable = false
                            }
                        }

                        BleHrService.STATE_DISCONNECTED -> {
                            // nascondi bottone rosso
                            stopAndClearEcg()

                            btnStopConnection.visibility = View.GONE

                            // ripristina padding originali
                            containerHeart.setPadding(
                                containerHeart.paddingLeft,
                                initialContainerPaddingTop,
                                containerHeart.paddingRight,
                                containerHeart.paddingBottom
                            )
                            cardDailyMax.setContentPadding(
                                cardDailyMax.contentPaddingLeft,
                                initialCardContentPaddingTop,
                                cardDailyMax.contentPaddingRight,
                                cardDailyMax.contentPaddingBottom
                            )

                            // ripristina UI "non connesso"
                            tvHeartRate.apply {
                                text = "Effettua la connessione"
                                setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                                setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
                                isClickable = true
                            }
                            titoloperconnessione.visibility = View.VISIBLE
                        }
                    }
                }

            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViews(view)
        setupEcgChart()
        titoloperconnessione.visibility = View.VISIBLE


        detailsExpanded = savedInstanceState?.getBoolean("detailsExpanded") ?: false
        applyToggleUi(detailsExpanded)

        btnToggleDetails.setOnClickListener {
            detailsExpanded = !detailsExpanded
            applyToggleUi(detailsExpanded)
        }

        tvHeartRate.setOnClickListener {
            foundDevices.clear()
            checkAndStartBleScan()
        }

        uid = auth.currentUser?.uid.also {
            if (it.isNullOrEmpty()) {
                toast("Devi effettuare il login")
                return
            }
        }
        attachPtRoleListener()
        vm = ViewModelProvider(this, ProgressionVmFactory(requireContext(), uid!!)).get(ProgressionViewModel::class.java)

        loadGoals()
        loadLatestMeasurements()
        setupConfirmButton()
        setupGraphButtons()
        setupDailyHrChart()
        attachDailyHrListener()
    }

    private fun bindViews(v: View) {
        etWeightGoalValue = v.findViewById(R.id.etWeightGoalValue)
        etLeanGoalValue = v.findViewById(R.id.etLeanGoalValue)
        etBodyFatGoalValue = v.findViewById(R.id.etBodyFatGoalValue)
        btnConfirm = v.findViewById(R.id.buttonConfirm)
        bfSubtitle = v.findViewById(R.id.bfSubtitle)
        pesoSubtitle = v.findViewById(R.id.pesoSubtitle)
        leanSubtitle = v.findViewById(R.id.weightSubtitle)
        buttonForBF = v.findViewById(R.id.buttonForBF)
        buttonForWEIGHT = v.findViewById(R.id.buttonForWEIGHT)
        buttonForMassaMagra = v.findViewById(R.id.buttonForMassaMagra)
        tvHeartRate = v.findViewById(R.id.tvHeartRate)
        tvBpmValue   = v.findViewById(R.id.tvBpmValue)
        ivHeartPulse = v.findViewById(R.id.ivHeartPulse)
        ecgChart = v.findViewById(R.id.ecgChart)
        containerHeart = v.findViewById(R.id.containerHeart)
        chartDailyMaxHr = v.findViewById(R.id.chartDailyMaxHr)
        titoloperconnessione = v.findViewById(R.id.titoloperconnessione)
        btnToggleDetails = v.findViewById(R.id.btnToggleDetails)
        cardDailyMax     = v.findViewById(R.id.cardDailyMax)
        textforsecondgraph = v.findViewById(R.id.textforsecondgraph)
        imgToggleArrow   = v.findViewById(R.id.imgToggleArrow)
        tvToggleDetails  = v.findViewById(R.id.tvToggleDetails)
        btnStopConnection = v.findViewById(R.id.btnStopConnection)
        btnStopConnection.visibility = View.GONE

        // --- CHAT SETUP (Gemini) ---
        val repo = ChatRepository.create(apiKey = Secrets.GEMINI_KEY)
        chatVM = ViewModelProvider(this, ChatVMFactory(repo))[ChatViewModel::class.java]

        val overlayRoot = v.findViewById<ViewGroup>(R.id.rootContainer)
        chatOverlay = ChatOverlay(overlayRoot, chatVM, viewLifecycleOwner)
        chatOverlay.hide()  // visibilità GONE -> non intercetta i tocchi

        // pulsante: visivamente “piatto”, ma cliccabile
        v.findViewById<View>(R.id.buttonForChatGPT)?.apply {
            elevation = 0f
            translationZ = 0f
            isEnabled = true
            isClickable = true
            isFocusable = true
            setOnClickListener { chatOverlay.toggle() }
        }
        // --- FINE CHAT SETUP ---

        initialContainerPaddingTop = containerHeart.paddingTop
        initialCardContentPaddingTop = cardDailyMax.contentPaddingTop

        etWeightGoalValue.isEnabled = false
        etLeanGoalValue.isEnabled = false
        etBodyFatGoalValue.isEnabled = false
        btnConfirm.visibility = View.GONE

        setGoalsEditable(false)
    }




    private fun stopBleService() {
        // UI immediata
        stopAndClearEcg()
        btnStopConnection.visibility = View.GONE
        // invia lo STOP al service
        val i = Intent(requireContext(), BleHrService::class.java).apply {
            action = BleHrService.ACTION_STOP
        }
        requireContext().startService(i)
    }

    private fun setupEcgChart() {
        ecgDataSet = LineDataSet(mutableListOf(), "ECG").apply {
            setDrawCircles(false)
            lineWidth = 2f
            setDrawValues(false)
        }

        val llWarmup = LimitLine(90f, "Riscaldamento").apply {
            lineWidth = 2f
            enableDashedLine(10f, 10f, 0f)
            labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
            textSize = 12f
            lineColor = ContextCompat.getColor(requireContext(), R.color.blue)
            textColor = ContextCompat.getColor(requireContext(), R.color.blue)
        }
        val llModerate = LimitLine(114f, "Moderata").apply {
            lineWidth = 2f
            enableDashedLine(10f, 10f, 0f)
            labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
            textSize = 12f
            lineColor = ContextCompat.getColor(requireContext(), R.color.green)
            textColor = ContextCompat.getColor(requireContext(), R.color.green)
        }
        val llHigh = LimitLine(152f, "Alta").apply {
            lineWidth = 2f
            enableDashedLine(10f, 10f, 0f)
            labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
            textSize = 12f
            lineColor = ContextCompat.getColor(requireContext(), R.color.semi_transparent_red)
            textColor = ContextCompat.getColor(requireContext(), R.color.semi_transparent_red)
        }

        ecgChart.apply {
            data = LineData(ecgDataSet)
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(false)
            setScaleEnabled(false)

            axisLeft.apply {
                removeAllLimitLines()
                addLimitLine(llWarmup)
                addLimitLine(llModerate)
                addLimitLine(llHigh)

                axisMinimum = 0f
                axisMaximum = 160f
                granularity = 10f
                setLabelCount(17, true)
            }
            axisRight.isEnabled = false
        }
    }

    private fun attachPtRoleListener() {
        val uidSafe = uid ?: return
        ptDocListener?.remove()
        ptDocListener = db.collection("personal_trainers").document(uidSafe)
            .addSnapshotListener { snap, err ->
                val isPT = (err == null && snap?.exists() == true) ||
                        (snap?.getBoolean("isPersonalTrainer") == true)
                isPtUser = isPT
                btnConfirm.visibility = if (isPtUser) View.VISIBLE else View.GONE
                Log.d("ProgressionFragment", "PT? $isPtUser (exists=${snap?.exists()})")
            }
    }

    private fun stopAndClearEcg() {
        // ferma animazione cuore e nascondi UI HR
        heartAnimator?.cancel()
        ivHeartPulse.visibility = View.GONE
        tvBpmValue.visibility = View.GONE

        // ferma il loop ECG
        ecgRunning = false
        ecgHandler.removeCallbacks(ecgSampleRunnable)
        ecgHandler.removeCallbacksAndMessages(null) // extra safety

        // reset valori e SVUOTA il dataset
        ecgTime = 0f
        currentBpm = 60f
        nextBeatTime = 0f
        ecgDataSet.clear()
        ecgChart.data = LineData(ecgDataSet)
        ecgChart.invalidate()
    }


    private fun animateHeart(bpm: Int) {
        heartAnimator?.cancel()
        val cycleMs = (60000f / bpm).toLong()
        heartAnimator = ObjectAnimator.ofPropertyValuesHolder(ivHeartPulse,
            PropertyValuesHolder.ofFloat("scaleX", 0.8f, 1.1f),
            PropertyValuesHolder.ofFloat("scaleY", 0.8f, 1.1f)
        ).apply {
            duration = cycleMs
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            start()
        }
    }

    // salva su Firestore se il bpm supera il massimo odierno
    private fun updateDailyMaxHeartRate(bpm: Int) {
        val uidLocal = uid ?: return
        val nowEpoch = java.time.LocalDate.now().toEpochDay()

        if (nowEpoch != todayEpochDay) {
            todayEpochDay = nowEpoch
            todayMaxBpm = 0
            hasLoadedDailyMax = false
        }

        if (!hasLoadedDailyMax) {
            db.collection("users").document(uidLocal)
            hrDailyCol()?.document(todayEpochDay.toString())
                ?.get()
                ?.addOnSuccessListener { snap ->
                    val existing = snap.getLong("maxBpm")?.toInt() ?: 0
                    todayMaxBpm = existing
                    hasLoadedDailyMax = true
                    if (bpm > todayMaxBpm) writeDailyMax(uidLocal, bpm)
                }
                ?.addOnFailureListener {
                    hasLoadedDailyMax = true
                    if (bpm > todayMaxBpm) writeDailyMax(uidLocal, bpm)
                }
            return
        }

        if (bpm > todayMaxBpm) writeDailyMax(uidLocal, bpm)
    }

    private fun writeDailyMax(uidLocal: String, bpm: Int) {
        todayMaxBpm = bpm

        val data = mapOf(
            "epochDay" to todayEpochDay,
            "maxBpm" to bpm,
            "updatedAt" to com.google.firebase.Timestamp.now()
        )

        db.collection("users").document(uidLocal)
            .collection("heartRateDaily").document(todayEpochDay.toString())
            .set(data, SetOptions.merge())

        db.collection("users").document(uidLocal)
            .set(
                mapOf(
                    "currentDailyMaxBpm" to bpm,
                    "lastHeartRateEpochDay" to todayEpochDay
                ),
                SetOptions.merge()
            )
    }

    private fun setupDailyHrChart() = chartDailyMaxHr.apply {
        description.isEnabled = false
        setNoDataText("Nessun dato HR giornaliero")
        setTouchEnabled(true)
        setPinchZoom(true)
        legend.isEnabled = false
        axisRight.isEnabled = false

        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        axisLeft.setDrawGridLines(true)

        xAxis.textColor = Color.LTGRAY
        axisLeft.textColor = Color.LTGRAY

        setExtraOffsets(8f, 16f, 8f, 16f)
    }

    private fun attachDailyHrListener() {
        hrListener?.remove()
        val col = hrDailyCol() ?: return
        hrListener = col
            .orderBy("epochDay", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                val points = snap.documents.mapNotNull { d ->
                    val epoch = d.getLong("epochDay") ?: return@mapNotNull null
                    val maxBpm = d.getLong("maxBpm")?.toInt() ?: return@mapNotNull null
                    epoch to maxBpm
                }
                updateDailyHrChart(points)
            }
    }

    private fun updateDailyHrChart(points: List<Pair<Long, Int>>) {
        if (points.isEmpty()) {
            chartDailyMaxHr.clear()
            chartDailyMaxHr.invalidate()
            return
        }

        val sorted = points.sortedBy { it.first }
        val entries = sorted.mapIndexed { idx, p -> Entry(idx.toFloat(), p.second.toFloat()) }

        val ds = LineDataSet(entries, "Daily Max HR").apply {
            axisDependency = YAxis.AxisDependency.LEFT
            mode = LineDataSet.Mode.LINEAR
            setDrawValues(false)
            lineWidth = 2f
            setDrawCircles(true)
            circleRadius = 3f
            color = Color.WHITE
            setCircleColor(Color.WHITE)
            highLightColor = Color.YELLOW
            setDrawFilled(true)
            fillColor = Color.parseColor("#33FFFFFF")
            fillAlpha = 60
        }

        chartDailyMaxHr.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                val idx = value.toInt()
                return if (idx in sorted.indices) {
                    val d = java.time.LocalDate.ofEpochDay(sorted[idx].first)
                    "${d.dayOfMonth}/${d.monthValue}"
                } else ""
            }
        }

        val maxHr = sorted.maxOfOrNull { it.second }?.toFloat() ?: 0f
        val maxY = (maxHr + 10f).coerceAtLeast(160f)
        chartDailyMaxHr.axisLeft.apply {
            axisMinimum = 0f
            axisMaximum = maxY
        }

        chartDailyMaxHr.data = LineData(ds)
        chartDailyMaxHr.notifyDataSetChanged()
        chartDailyMaxHr.invalidate()
    }

    private fun scheduleNextBeat() {
        nextBeatTime = ecgTime + 60f / currentBpm
    }

    private fun startEcg() {
        if (!ecgRunning) {
            ecgRunning = true
            ecgHandler.post(ecgSampleRunnable)
        }
    }

    private val ecgSampleRunnable = object : Runnable {
        override fun run() {
            val delta = ecgTime - nextBeatTime
            val amp = currentBpm / 200f * 1.5f
            val value = when {
                delta in 0f..0.05f  -> amp
                delta in 0.05f..0.1f -> -amp/2f
                else                 -> 0f
            }
            ecgDataSet.addEntry(Entry(ecgTime, currentBpm))
            ecgTime += sampleIntervalMs / 1000f
            ecgChart.data.notifyDataChanged()
            ecgChart.notifyDataSetChanged()
            ecgChart.setVisibleXRangeMaximum(5f)
            ecgChart.moveViewToX(ecgTime)
            ecgHandler.postDelayed(this, sampleIntervalMs)
        }
    }

    private fun applyToggleUi(expanded: Boolean) {
        cardDailyMax.visibility = if (expanded) View.VISIBLE else View.GONE
        textforsecondgraph.visibility = if(expanded) View.VISIBLE else View.GONE
        imgToggleArrow.setImageResource(
            if (expanded) R.drawable.ic_arrow_up else R.drawable.ic_arrow_down
        )
        tvToggleDetails.text = if (expanded) "Nascondi dettagli" else "Maggiori dettagli"
    }

    @SuppressLint("MissingPermission")
    private fun checkAndStartBleScan() {
        val needed = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) needed += Manifest.permission.BLUETOOTH_SCAN
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) needed += Manifest.permission.BLUETOOTH_CONNECT
        } else {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) needed += Manifest.permission.ACCESS_FINE_LOCATION
        }
        if (needed.isNotEmpty()) {
            requestPermissions(needed.toTypedArray(), REQUEST_BLE_PERMISSIONS)
        } else {
            startBleScan()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startBleScan() {
        if (isScanning) return
        isScanning = true
        foundDevices.clear()
        bluetoothAdapter?.bluetoothLeScanner?.let { scanner ->
            try {
                scanner.startScan(
                    listOf(ScanFilter.Builder().setServiceUuid(ParcelUuid(HR_SERVICE_UUID)).build()),
                    ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(),
                    hrScanCallback
                )
            } catch (e: SecurityException) {
                toast("Permesso BLE mancante per scan")
                isScanning = false
                return
            }
            tvHeartRate.postDelayed({
                try { scanner.stopScan(hrScanCallback) } catch (_: SecurityException) { toast("Impossibile fermare scan") }
                isScanning = false
                if (foundDevices.isEmpty()) toast("Nessun dispositivo trovato") else showDevicePicker()
            }, 1000)
        } ?: run {
            toast("BLE scanner non disponibile")
            isScanning = false
        }
    }

    private fun showDevicePicker() {
        if (foundDevices.isEmpty()) { toast("Nessun dispositivo trovato"); return }
        val names = foundDevices.map { it.name ?: it.address }.toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle("Scegli dispositivo HR")
            .setItems(names) { _, which ->
                val addr = foundDevices[which].address
                startBleService(addr)
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    private fun startBleService(address: String) {
        val i = Intent(requireContext(), BleHrService::class.java).apply {
            action = BleHrService.ACTION_START
            putExtra(BleHrService.EXTRA_DEVICE_ADDRESS, address)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireContext().startForegroundService(i)
        } else {
            requireContext().startService(i)
        }
    }

    private fun loadLatestMeasurements() {
        val col = entriesCol() ?: return
        col.orderBy("epochDay", Query.Direction.DESCENDING).limit(10).get().addOnSuccessListener { snaps ->
            val bf = snaps.documents.firstOrNull { it.contains("bodyFatPercent") }?.getDouble("bodyFatPercent")?.toFloat()
            bfSubtitle.text = bf?.let { "%.1f %%".format(it) } ?: "—"
        }
        col.orderBy("epochDay", Query.Direction.DESCENDING).limit(10).get().addOnSuccessListener { snaps ->
            val w = snaps.documents.firstOrNull { it.contains("bodyWeightKg") }?.getDouble("bodyWeightKg")?.toFloat()
            pesoSubtitle.text = w?.let { "%.1f kg".format(it) } ?: "—"
        }
        col.orderBy("epochDay", Query.Direction.DESCENDING).limit(10).get().addOnSuccessListener { snaps ->
            val lm = snaps.documents.firstOrNull { it.contains("leanMassKg") }?.getDouble("leanMassKg")?.toFloat()
            leanSubtitle.text = lm?.let { "%.1f kg".format(it) } ?: "—"
        }
    }

    private fun loadGoals() {
        val uidSafe = uid ?: return

        db.collection("personal_trainers").document(uidSafe)
            .get()
            .addOnSuccessListener { ptSnap ->
                isPtUser = ptSnap.exists() || (ptSnap.getBoolean("isPersonalTrainer") == true)

                btnConfirm.visibility = if (isPtUser) View.VISIBLE else View.GONE

                val targetRef = if (isPtUser)
                    db.collection("personal_trainers").document(uidSafe)
                else
                    db.collection("users").document(uidSafe)

                targetRef.get()
                    .addOnSuccessListener { tSnap ->
                        fun num(k: String) = (tSnap.get(k) as? Number)?.toDouble()
                        num("targetWeight")?.let { etWeightGoalValue.setText(String.format("%.1f", it)) }
                        num("targetLeanMass")?.let { etLeanGoalValue.setText(String.format("%.1f", it)) }
                        num("targetFatMass")?.let  { etBodyFatGoalValue.setText(String.format("%.1f", it)) }

                        editingGoals = false
                        btnConfirm.text = "modifica parametri"
                        setGoalsEditable(false)
                    }
            }
            .addOnFailureListener {
                isPtUser = false
                btnConfirm.visibility = View.GONE
            }
    }

    // Ritorna la root del documento in base al ruolo
    private fun roleRootDoc() =
        uid?.let { if (isPtUser) db.collection("personal_trainers").document(it)
        else           db.collection("users").document(it) }

    // La collection dove salvi/leggi i daily HR
    private fun hrDailyCol() = roleRootDoc()?.collection("heartRateDaily")
    private fun entriesCol() = roleRootDoc()?.collection("bodyFatEntries")

    private fun setupConfirmButton() {
        btnConfirm.text = if (!editingGoals) "modifica parametri" else "salva obiettivi"

        btnConfirm.setOnClickListener {
            if (!isPtUser) {
                toast("Solo il personal trainer può modificare gli obiettivi")
                return@setOnClickListener
            }

            if (!editingGoals) {
                editingGoals = true
                btnConfirm.text = "salva obiettivi"
                setGoalsEditable(true)
                etWeightGoalValue.imeOptions = EditorInfo.IME_ACTION_DONE
                etLeanGoalValue.imeOptions   = EditorInfo.IME_ACTION_DONE
                etBodyFatGoalValue.imeOptions= EditorInfo.IME_ACTION_DONE
                etWeightGoalValue.requestFocus()
                return@setOnClickListener
            }

            val w    = etWeightGoalValue.text.toString().replace(',', '.').toDoubleOrNull()
            val lean = etLeanGoalValue.text.toString().replace(',', '.').toDoubleOrNull()
            val fat  = etBodyFatGoalValue.text.toString().replace(',', '.').toDoubleOrNull()

            when {
                w == null   -> { toast("Peso obiettivo non valido"); return@setOnClickListener }
                lean == null-> { toast("Massa magra obiettivo non valida"); return@setOnClickListener }
                fat == null -> { toast("% grasso obiettivo non valido"); return@setOnClickListener }
            }

            vm.updateGoals(newWeight = w, newLean = lean, newFat = fat)

            val uidSafe = uid ?: return@setOnClickListener
            db.collection("personal_trainers").document(uidSafe)
                .set(
                    mapOf(
                        "targetWeight" to w,
                        "targetLeanMass" to lean,
                        "targetFatMass"  to fat
                    ),
                    SetOptions.merge()
                )
                .addOnSuccessListener {
                    toast("Obiettivi salvati")
                    editingGoals = false
                    btnConfirm.text = "modifica parametri"
                    setGoalsEditable(false)
                }
                .addOnFailureListener { e ->
                    toast("Errore salvataggio obiettivi: ${e.localizedMessage}")
                }
        }
    }

    private fun setupGraphButtons() {
        val nav = findNavController()
        buttonForBF.setOnClickListener { nav.navigate(R.id.action_progressionFragment_to_graphsFragment, bundleOf("graphType" to "bodyfat")) }
        buttonForWEIGHT.setOnClickListener { nav.navigate(R.id.action_progressionFragment_to_graphsFragment, bundleOf("graphType" to "weight")) }
        buttonForMassaMagra.setOnClickListener { nav.navigate(R.id.action_progressionFragment_to_graphsFragment, bundleOf("graphType" to "lean")) }
    }

    override fun onStart() {
        super.onStart()
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
            hrReceiver,
            IntentFilter().apply {
                addAction(BleHrService.ACTION_HR_UPDATE)
                addAction(BleHrService.ACTION_STATE)
            }
        )
        applyConnectedUiIfNeeded()
    }

    override fun onStop() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(hrReceiver)
        super.onStop()
    }

    override fun onDestroyView() {
        hrListener?.remove()
        hrListener = null
        super.onDestroyView()
        heartAnimator?.cancel()
        ecgHandler.removeCallbacksAndMessages(null)
        // NON chiudere la connessione BLE qui: la gestisce BleHrService
    }

    private fun toast(msg: String) =
        android.widget.Toast.makeText(requireContext(), msg, android.widget.Toast.LENGTH_SHORT).show()

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("detailsExpanded", detailsExpanded)
    }

    private fun setGoalsEditable(enabled: Boolean) {
        listOf(etWeightGoalValue, etLeanGoalValue, etBodyFatGoalValue).forEach {
            it.isEnabled = enabled
            it.isFocusable = enabled
            it.isFocusableInTouchMode = enabled
        }
    }

    // ===== Helpers aggiunti SOLO per il ripristino della UI con nome modello =====

    private fun getFirstConnectedDeviceName(): String {
        return try {
            val mgr = requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
                return ""
            }
            val dev = mgr.getConnectedDevices(BluetoothProfile.GATT).firstOrNull()
            dev?.name ?: dev?.address ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    private fun applyConnectedUiIfNeeded() {
        val name = getFirstConnectedDeviceName()
        if (name.isEmpty()) {
            btnStopConnection.visibility = View.GONE
            return
        }

        // padding container
        val topPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 46f, resources.displayMetrics
        ).toInt()
        containerHeart.setPadding(
            containerHeart.paddingLeft, topPx, containerHeart.paddingRight, containerHeart.paddingBottom
        )

        // padding dentro card
        val cardTopPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 16f, resources.displayMetrics
        ).toInt()
        cardDailyMax.setContentPadding(
            cardDailyMax.contentPaddingLeft,
            cardTopPx,
            cardDailyMax.contentPaddingRight,
            cardDailyMax.contentPaddingBottom
        )

        tvHeartRate.apply {
            text = "connesso a $name"
            setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_green_dot, 0, 0, 0)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            ellipsize = TextUtils.TruncateAt.END
            maxLines = 1
            isClickable = false
        }

        titoloperconnessione.visibility = View.GONE

        btnStopConnection.apply {
            visibility = View.VISIBLE
            setOnClickListener { stopBleService() }
        }
    }
}
