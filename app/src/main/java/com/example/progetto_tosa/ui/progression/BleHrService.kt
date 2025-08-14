package com.example.progetto_tosa.ui.progression

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.progetto_tosa.R
import java.util.UUID

class BleHrService : Service() {

    companion object {
        //azioni pubbliche per controllare il service //commento
        const val ACTION_START = "BleHrService.START" //commento
        const val ACTION_STOP = "BleHrService.STOP" //commento

        //extra di input //commento
        const val EXTRA_DEVICE_ADDRESS = "device_address" //commento

        //broadcast in uscita verso la UI //commento
        const val ACTION_HR_UPDATE = "BleHrService.HR_UPDATE" //commento
        const val EXTRA_BPM = "extra_bpm" //commento
        const val ACTION_STATE = "BleHrService.STATE" //commento
        const val EXTRA_STATE = "extra_state" //commento
        const val STATE_CONNECTING = "connecting" //commento
        const val STATE_CONNECTED = "connected" //commento
        const val STATE_DISCONNECTED = "disconnected" //commento

        // 👉 NUOVO: nome dispositivo nel broadcast stato
        const val EXTRA_DEVICE_NAME = "extra_device_name" //commento

        //notifica foreground //commento
        private const val NOTIF_ID = 42 //commento
        private const val NOTIF_CHANNEL = "ble_hr_channel" //commento
    }

    //BLE //commento
    private val bluetoothAdapter: BluetoothAdapter? by lazy { BluetoothAdapter.getDefaultAdapter() } //commento
    private var gatt: BluetoothGatt? = null //commento
    private var deviceAddress: String? = null //commento

    //reconnect con backoff esponenziale //commento
    private var reconnectAttempts = 0 //commento
    private val mainHandler = Handler(Looper.getMainLooper()) //commento

    //UUID standard Heart Rate //commento
    private val HR_SERVICE_UUID = UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb") //commento
    private val HR_MEAS_UUID = UUID.fromString("00002A37-0000-1000-8000-00805f9b34fb") //commento
    private val CCC_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb") //commento

    override fun onBind(intent: Intent?): IBinder? = null //service senza binding //commento

    override fun onCreate() {
        super.onCreate()
        createNotifChannel() //commento
        startForeground(NOTIF_ID, buildNotification("Attesa connessione…")) //commento
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                deviceAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS)
                sendState(STATE_CONNECTING)
                updateNotification("Connessione in corso…")
                connectNow()
            }
            ACTION_STOP -> {
                // forza disconnessione pulita
                updateNotification("Disconnessione…")
                safeCloseGatt()
                // notifica subito la UI (non affidarti solo alla callback)
                sendState(STATE_DISCONNECTED)
                stopForeground(true)
                stopSelf()
            }
        }
        return START_STICKY
    }


    @SuppressLint("MissingPermission")
    private fun connectNow() {
        val addr = deviceAddress ?: return //commento
        if (!hasConnectPermission()) {
            //manca permesso BLUETOOTH_CONNECT su Android 12+ //commento
            updateNotification("Permesso BLE mancante") //commento
            return
        }
        //se già connesso allo stesso device, non riconnettere //commento
        gatt?.device?.let { d ->
            if (d.address == addr &&
                getConnectedGattDevices().any { it.address == addr }
            ) return //già connesso //commento
        }
        val dev: BluetoothDevice = try {
            bluetoothAdapter?.getRemoteDevice(addr) ?: return
        } catch (_: IllegalArgumentException) {
            updateNotification("Device non valido") //commento
            return
        }
        safeCloseGatt() //commento
        gatt = dev.connectGatt(this, false, callback) //commento
    }

    private val callback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    reconnectAttempts = 0 //commento
                    // 👉 INVIA anche il nome del dispositivo nel broadcast
                    val devName = g.device.name ?: g.device.address ?: "dispositivo" //commento
                    sendState(STATE_CONNECTED, devName) //commento
                    updateNotification("Connesso a $devName") //commento
                    //discover services immediato //commento
                    try { g.discoverServices() } catch (_: SecurityException) {} //commento
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    sendState(STATE_DISCONNECTED) //commento
                    updateNotification("Disconnesso, riconnessione…") //commento
                    scheduleReconnect() //commento
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            if (!hasConnectPermission()) return //commento
            val ch = g.getService(HR_SERVICE_UUID)?.getCharacteristic(HR_MEAS_UUID) ?: run {
                updateNotification("Caratteristica HR non trovata") //commento
                return
            }
            try {
                g.setCharacteristicNotification(ch, true) //commento
                val ccc = ch.getDescriptor(CCC_UUID)
                if (ccc != null) {
                    ccc.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE //commento
                    g.writeDescriptor(ccc) //commento
                }
            } catch (_: SecurityException) { }
        }

        override fun onCharacteristicChanged(g: BluetoothGatt, c: BluetoothGattCharacteristic) {
            if (c.uuid != HR_MEAS_UUID) return //commento
            val data = c.value ?: return //commento
            val flag = data[0].toInt() //commento
            val bpm = if (flag and 0x01 == 0) {
                data[1].toInt() and 0xFF //commento
            } else {
                ((data[2].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF) //commento
            }
            broadcastHr(bpm) //commento
        }
    }

    private fun broadcastHr(bpm: Int) {
        val i = Intent(ACTION_HR_UPDATE).putExtra(EXTRA_BPM, bpm) //commento
        LocalBroadcastManager.getInstance(this).sendBroadcast(i) //commento
    }

    // 👉 MODIFICATA: accetta opzionalmente il nome da inviare alla UI
    private fun sendState(state: String, deviceName: String? = null) {
        val i = Intent(ACTION_STATE).putExtra(EXTRA_STATE, state) //commento
        if (!deviceName.isNullOrEmpty()) {
            i.putExtra(EXTRA_DEVICE_NAME, deviceName) //commento
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(i) //commento
    }

    private fun scheduleReconnect() {
        safeCloseGatt() //commento
        //backoff: 1s,2s,4s,8s,16s,32s (max) //commento
        val delay = (1000L * (1 shl reconnectAttempts).coerceAtMost(32)) //commento
        reconnectAttempts++ //commento
        mainHandler.postDelayed({ connectNow() }, delay) //commento
    }

    @SuppressLint("MissingPermission")
    private fun safeCloseGatt() {
        try {
            gatt?.disconnect()   // ← prima disconnette (genera state change lato stack)
        } catch (_: SecurityException) { }
        try {
            gatt?.close()        // ← poi chiude la risorsa
        } catch (_: SecurityException) { }
        gatt = null
    }


    private fun stopSelfSafely() {
        safeCloseGatt()
        sendState(STATE_DISCONNECTED)  // ← avvisa la UI
        stopForeground(true)
        stopSelf()
        updateNotification("Disconnesso")
    }

    private fun hasConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED //commento
        } else true //commento
    }

    //verifica dispositivi GATT connessi (utile per evitare doppie connessioni) //commento
    @SuppressLint("MissingPermission")
    private fun getConnectedGattDevices(): List<BluetoothDevice> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            if (!hasConnectPermission()) return emptyList() //commento
            val mgr = getSystemService(BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager //commento
            return mgr.getConnectedDevices(BluetoothProfile.GATT) //commento
        }
        return emptyList() //commento
    }

    //notifiche foreground //commento
    private fun createNotifChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                NOTIF_CHANNEL,
                "Heart Rate",
                NotificationManager.IMPORTANCE_LOW
            )
            ch.setShowBadge(false) //commento
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager //commento
            nm.createNotificationChannel(ch) //commento
        }
    }

    private fun buildNotification(text: String): Notification {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName) //commento
        val pi = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        ) //commento
        return NotificationCompat.Builder(this, NOTIF_CHANNEL)
            .setContentTitle("HR Monitor")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_heart) //sostituisci con una tua icona valida //commento
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager //commento
        nm.notify(NOTIF_ID, buildNotification(text)) //commento
    }

    override fun onDestroy() {
        safeCloseGatt() //commento
        super.onDestroy()
    }
}
