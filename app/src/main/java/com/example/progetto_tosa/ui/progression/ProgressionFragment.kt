package com.example.progetto_tosa.ui.progression

import android.Manifest
import android.annotation.SuppressLint
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import com.github.mikephil.charting.components.LimitLine
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.text.TextUtils
import android.util.TypedValue
import android.text.SpannableStringBuilder
import android.text.Spannable
import android.text.style.RelativeSizeSpan
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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.progetto_tosa.R
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.example.progetto_tosa.ui.progression.ProgressionVmFactory
import com.example.progetto_tosa.ui.progression.ProgressionViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import java.util.UUID

class ProgressionFragment : Fragment(R.layout.fragment_progression) {

    companion object {
        private const val REQUEST_BLE_PERMISSIONS = 1001
        private val HR_SERVICE_UUID = UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb")
        private val HR_MEASUREMENT_CHAR_UUID = UUID.fromString("00002A37-0000-1000-8000-00805f9b34fb")
    }

    // Firebase & BLE
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val bluetoothAdapter: BluetoothAdapter? by lazy { BluetoothAdapter.getDefaultAdapter() }
    private var bluetoothGatt: BluetoothGatt? = null
    private var isScanning = false
    private val foundDevices = mutableListOf<BluetoothDevice>()
    private var permissionRequestedToSettings = false
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

    // UI elements
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var etWeightGoalValue: EditText
    private lateinit var etLeanGoalValue: EditText
    private lateinit var etBodyFatGoalValue: EditText
    private lateinit var btnConfirm: TextView
    private lateinit var bfSubtitle: TextView
    private lateinit var pesoSubtitle: TextView
    private lateinit var leanSubtitle: TextView
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

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViews(view)
        setupEcgChart()

        tvHeartRate.setOnClickListener {
            foundDevices.clear()
            checkAndStartBleScan()
        }
        swipeRefresh.setOnRefreshListener {
            loadGoals()
            loadLatestMeasurements()
        }

        uid = auth.currentUser?.uid.also {
            if (it.isNullOrEmpty()) {
                toast("Devi effettuare il login")
                return
            }
        }
        vm = ViewModelProvider(this, ProgressionVmFactory(requireContext(), uid!!)).get(ProgressionViewModel::class.java)

        loadGoals()
        loadLatestMeasurements()
        setupConfirmButton()
        setupGraphButtons()
    }

    private fun bindViews(v: View) {
        swipeRefresh = v.findViewById(R.id.swipeRefresh)
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

        etWeightGoalValue.isEnabled = false
        etLeanGoalValue.isEnabled = false
        etBodyFatGoalValue.isEnabled = false
        btnConfirm.visibility = View.GONE
    }

    private fun setupEcgChart() {
        ecgDataSet = LineDataSet(mutableListOf(), "ECG").apply {
            setDrawCircles(false)
            lineWidth = 2f
            setDrawValues(false)
        }

        // ① Le 3 limit line
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
                axisMaximum = 160f  // assicurati che sia ≥ della soglia più alta
                granularity = 10f
                setLabelCount(17, true)
            }
            axisRight.isEnabled = false
        }
    }



    @SuppressLint("MissingPermission")
    private fun connectToHeartRateDevice(device: BluetoothDevice) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT), REQUEST_BLE_PERMISSIONS)
            return
        }
        try { bluetoothAdapter?.bluetoothLeScanner?.stopScan(hrScanCallback) } catch (_: SecurityException) {}
        try {
            bluetoothGatt = device.connectGatt(requireContext(), false, object : BluetoothGattCallback() {
                override fun onConnectionStateChange(
                    gatt: BluetoothGatt,
                    status: Int,
                    newState: Int
                ) {
                    if (newState == BluetoothGatt.STATE_CONNECTED) {
                        requireActivity().runOnUiThread {
                            // 1) Calcola 16 dp in pixel
                            val topPx = TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_DIP,
                                46f,
                                resources.displayMetrics
                            ).toInt()

                            // 2) Mantieni gli altri padding di containerHeart e applica solo il top
                            val left   = containerHeart.paddingLeft
                            val right  = containerHeart.paddingRight
                            val bottom = containerHeart.paddingBottom
                            containerHeart.setPadding(left, topPx, right, bottom)

                            // 3) Aggiorna tvHeartRate con pallino verde e nome dispositivo
                            tvHeartRate.apply {
                                text = "connesso a: ${gatt.device.name ?: gatt.device.address}"
                                setCompoundDrawablesWithIntrinsicBounds(
                                    R.drawable.ic_green_dot, 0, 0, 0
                                )
                                setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
                                ellipsize = TextUtils.TruncateAt.END
                                maxLines = 1
                                isClickable = false
                            }
                        }
                        // 4) Continua con la discovery dei servizi
                        gatt.discoverServices()
                    }
                }




                override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                    gatt.getService(HR_SERVICE_UUID)?.getCharacteristic(HR_MEASUREMENT_CHAR_UUID)?.let { hrChar ->
                        gatt.setCharacteristicNotification(hrChar, true)
                        val cccUuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
                        hrChar.getDescriptor(cccUuid)?.apply {
                            value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            gatt.writeDescriptor(this)
                        }
                    }
                }
                override fun onCharacteristicChanged(
                    gatt: BluetoothGatt,
                    characteristic: BluetoothGattCharacteristic
                ) {
                    if (characteristic.uuid == HR_MEASUREMENT_CHAR_UUID) {
                        val data = characteristic.value ?: return
                        val flag = data[0].toInt()
                        val bpm = if (flag and 0x01 == 0) {
                            data[1].toInt() and 0xFF
                        } else {
                            ((data[2].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)
                        }

                        requireActivity().runOnUiThread {
                            // Assicurati che la View sia visibile
                            tvBpmValue.visibility = View.VISIBLE
                            ivHeartPulse.visibility = View.VISIBLE

                            // Costruisci lo Spannable (come prima)
                            val label   = "Rilevati: "
                            val bpmText = "$bpm"
                            val unit    = " bpm"
                            val ssb = SpannableStringBuilder().apply {
                                append(label)
                                val start = length
                                append(bpmText)
                                setSpan(RelativeSizeSpan(1.8f), start, start + bpmText.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                                append(unit)
                            }

                            // Imposta il testo completo
                            tvBpmValue.text = ssb

                            // (non serve più ellipsize—il testo non verrà tagliato)
                            tvBpmValue.ellipsize = null
                            tvBpmValue.maxLines = Integer.MAX_VALUE

                            // avvia animazioni e ECG
                            animateHeart(bpm)
                            currentBpm = bpm.toFloat()
                            scheduleNextBeat()
                            startEcg()
                        }



                    }
                }


            })
        } catch (e: SecurityException) {
            // ignore
        }
    }

    private fun animateHeart(bpm: Int) {
        heartAnimator?.cancel()
        val cycleMs = (60000f / bpm).toLong()
        heartAnimator = ObjectAnimator.ofPropertyValuesHolder(ivHeartPulse,
            PropertyValuesHolder.ofFloat("scaleX", 0.8f, 1.1f),
            PropertyValuesHolder.ofFloat("scaleY", 0.8f, 1.1f)
        ).apply { duration = cycleMs; repeatCount = ValueAnimator.INFINITE; repeatMode = ValueAnimator.REVERSE; start() }
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
                scanner.startScan(listOf(ScanFilter.Builder().setServiceUuid(ParcelUuid(HR_SERVICE_UUID)).build()),
                    ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), hrScanCallback)
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
            .setItems(names) { _, which -> connectToHeartRateDevice(foundDevices[which]) }
            .setNegativeButton("Annulla", null)
            .show()
    }

    private fun loadLatestMeasurements() {
        val entriesRef = db.collection("users").document(uid!!).collection("bodyFatEntries")
        entriesRef.orderBy("epochDay", Query.Direction.DESCENDING).limit(10).get().addOnSuccessListener { snaps ->
            val bf = snaps.documents.firstOrNull { it.contains("bodyFatPercent") }?.getDouble("bodyFatPercent")?.toFloat()
            bfSubtitle.text = bf?.let { "%.1f %%".format(it) } ?: "—"
        }
        entriesRef.orderBy("epochDay", Query.Direction.DESCENDING).limit(10).get().addOnSuccessListener { snaps ->
            val w = snaps.documents.firstOrNull { it.contains("bodyWeightKg") }?.getDouble("bodyWeightKg")?.toFloat()
            pesoSubtitle.text = w?.let { "%.1f kg".format(it) } ?: "—"
        }
        entriesRef.orderBy("epochDay", Query.Direction.DESCENDING).limit(10).get().addOnSuccessListener { snaps ->
            val lm = snaps.documents.firstOrNull { it.contains("leanMassKg") }?.getDouble("leanMassKg")?.toFloat()
            leanSubtitle.text = lm?.let { "%.1f kg".format(it) } ?: "—"
            swipeRefresh.isRefreshing = false
        }
    }

    private fun loadGoals() {
        db.collection("personal_trainers").document(uid!!).get().addOnSuccessListener { snap ->
            isPtUser = snap.getBoolean("isPersonalTrainer") == true
            btnConfirm.visibility = if (isPtUser) View.VISIBLE else View.GONE
            if (isPtUser) {
                snap.getDouble("targetWeight")?.let { etWeightGoalValue.setText("%.1f".format(it)) }
                snap.getDouble("targetLeanMass")?.let { etLeanGoalValue.setText("%.1f".format(it)) }
                snap.getDouble("targetFatMass")?.let { etBodyFatGoalValue.setText("%.1f".format(it)) }
            } else {
                db.collection("users").document(uid!!).get().addOnSuccessListener { uSnap ->
                    uSnap.getDouble("targetWeight")?.let { etWeightGoalValue.setText("%.1f".format(it)) }
                    uSnap.getDouble("targetLeanMass")?.let { etLeanGoalValue.setText("%.1f".format(it)) }
                    uSnap.getDouble("targetFatMass")?.let { etBodyFatGoalValue.setText("%.1f".format(it)) }
                }
            }
            swipeRefresh.isRefreshing = false
        }
    }

    private fun setupConfirmButton() {
        btnConfirm.text = if (!editingGoals) "modifica parametri" else "salva obiettivi"
        btnConfirm.setOnClickListener {
            if (!editingGoals) {
                editingGoals = true
                btnConfirm.text = "salva obiettivi"
                etWeightGoalValue.isEnabled = true
                etLeanGoalValue.isEnabled = true
                etBodyFatGoalValue.isEnabled = true
                etWeightGoalValue.imeOptions = EditorInfo.IME_ACTION_DONE
                etLeanGoalValue.imeOptions = EditorInfo.IME_ACTION_DONE
                etBodyFatGoalValue.imeOptions = EditorInfo.IME_ACTION_DONE
            } else {
                val w = etWeightGoalValue.text.toString().replace(',', '.').toDoubleOrNull()
                val lean = etLeanGoalValue.text.toString().replace(',', '.').toDoubleOrNull()
                val fat = etBodyFatGoalValue.text.toString().replace(',', '.').toDoubleOrNull()
                when {
                    w == null -> toast("Peso obiettivo non valido")
                    lean == null -> toast("M. magra obiettivo non valida")
                    fat == null -> toast("% grasso obiettivo non valido")
                    else -> {
                        vm.updateGoals(newWeight = w, newLean = lean, newFat = fat)
                        val targetRef = if (isPtUser)
                            db.collection("personal_trainers").document(uid!!)
                        else
                            db.collection("users").document(uid!!)
                        targetRef.set(
                            mapOf(
                                "targetWeight" to w,
                                "targetLeanMass" to lean,
                                "targetFatMass" to fat
                            ), SetOptions.merge()
                        )
                        toast("Obiettivi salvati")
                        etWeightGoalValue.isEnabled = false
                        etLeanGoalValue.isEnabled = false
                        etBodyFatGoalValue.isEnabled = false
                        editingGoals = false
                        btnConfirm.text = "modifica parametri"
                    }
                }
            }
        }
    }

    private fun setupGraphButtons() {
        val nav = findNavController()
        buttonForBF.setOnClickListener { nav.navigate(R.id.action_progressionFragment_to_graphsFragment, bundleOf("graphType" to "bodyfat")) }
        buttonForWEIGHT.setOnClickListener { nav.navigate(R.id.action_progressionFragment_to_graphsFragment, bundleOf("graphType" to "weight")) }
        buttonForMassaMagra.setOnClickListener { nav.navigate(R.id.action_progressionFragment_to_graphsFragment, bundleOf("graphType" to "lean")) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Ferma animazioni e handler
        heartAnimator?.cancel()
        ecgHandler.removeCallbacksAndMessages(null)
        // Chiudi GATT con check permessi
        if (bluetoothGatt != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT)
                        == PackageManager.PERMISSION_GRANTED) {
                        bluetoothGatt?.close()
                    }
                } else {
                    bluetoothGatt?.close()
                }
            } catch (e: SecurityException) {
                // permesso mancante, ignora
                Log.w("ProgressionFragment", "Impossibile chiudere BluetoothGatt", e)
            }
            bluetoothGatt = null
        }
    }

    private fun toast(msg: String) =
        android.widget.Toast.makeText(requireContext(), msg, android.widget.Toast.LENGTH_SHORT).show()
}
