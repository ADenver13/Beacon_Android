package com.example.myapplication

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.util.Log
import kotlin.math.pow

class BluetoothScanner(
    private val context: Context,
    private val targetMac: String
) {
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        manager.adapter
    }

    private val bluetoothLeScanner: BluetoothLeScanner?
        get() = bluetoothAdapter?.bluetoothLeScanner

    var onDeviceFound: ((distance: Double) -> Unit)? = null

    // Buffer for smoothing RSSI readings
    private val rssiBuffer = mutableListOf<Int>()
    private val maxBufferSize = 4

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.let {
                if (it.device.address == targetMac) {
                    val rssi = it.rssi
                    addRssiMeasurement(rssi)
                    val filteredRssi = rssiBuffer.average()  // Average recent readings
                    val distance = calculateDistance(filteredRssi)
                    onDeviceFound?.invoke(distance)
                    Log.d("BluetoothScanner", "RSSI: $rssi, Filtered RSSI: $filteredRssi, Distance: $distance meters")
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("BLE_SCAN", "Scan failed: $errorCode")
        }
    }

    fun startScan() {
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(0L)
            .build()
        bluetoothLeScanner?.startScan(null, settings, scanCallback)
        Log.d("BluetoothScanner", "Started scanning...")
    }

    fun stopScan() {
        bluetoothLeScanner?.stopScan(scanCallback)
        Log.d("BluetoothScanner", "Stopped scanning.")
    }

    // Add RSSI measurement, trim if needed
    private fun addRssiMeasurement(rssi: Int) {
        rssiBuffer.add(rssi)
        if (rssiBuffer.size > maxBufferSize) {
            rssiBuffer.removeAt(0)
        }
    }

    // Log-distance path loss model
    private fun calculateDistance(averageRssi: Double): Double {
        val A = -59  // RSSI value at 1 meter (calibrated for S24+ indoors)
        val n = 2.0
        return 10.0.pow((A - averageRssi) / (10 * n))
    }
}
