package com.example.myapplication

import android.os.Bundle
import android.os.Vibrator
import android.os.VibrationEffect
import android.content.Context
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {

    private lateinit var vibrator: Vibrator

    private lateinit var bluetoothScanner: BluetoothScanner
    private lateinit var deviceOrientationManager: DeviceOrientationManager

    private lateinit var distanceTextView: TextView
    private lateinit var directionTextView: TextView
    private lateinit var arrowImage: ImageView

    // Latest sensor values
    private var latestDistance: Double = 0.0
    private var latestAzimuth: Float = 0f

    private var macAddr = "DD:34:02:09:C8:B8"

    private lateinit var deviceMap: Map<String, Pair<String, String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Link views from XML
        val titleText = findViewById<TextView>(R.id.titleText)
        distanceTextView = findViewById(R.id.distanceTextView)
        directionTextView = findViewById(R.id.directionTextView)
        arrowImage = findViewById(R.id.arrowImage)

        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        // CSV data from assets when ap starts
        loadDeviceData()

        val deviceName = lookupNameByMac(macAddr)
        Log.d("MainActivity", "Device name for $macAddr is $deviceName")

        // Set header to name of stop if found
        titleText.text = deviceName ?: "Proximity Tracker"

        //Init managers
        bluetoothScanner = BluetoothScanner(this, macAddr).apply {
            onDeviceFound = { distance ->
                latestDistance = distance
                runOnUiThread { updateDisplay() }
                Log.d("MainActivity", "Distance updated: $distance m")
            }
        }

        deviceOrientationManager = DeviceOrientationManager(this).apply {
            onOrientationChangedCallback = { azimuth ->
                latestAzimuth = azimuth
                runOnUiThread { updateDisplay() }
                Log.d("MainActivity", "Azimuth updated: $azimuth")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        bluetoothScanner.startScan()
        deviceOrientationManager.start()
    }

    override fun onPause() {
        super.onPause()
        bluetoothScanner.stopScan()
        deviceOrientationManager.stop()
    }

    private fun updateDisplay() {
        val azimuthDegrees = Math.toDegrees(latestAzimuth.toDouble())
        val invertedAzimuth = (azimuthDegrees + 180) % 360

        distanceTextView.text = String.format("Distance: %.2f m", latestDistance)
        directionTextView.text = String.format("Direction: %.2f°", azimuthDegrees)

        arrowImage.rotation = ((-azimuthDegrees).toFloat() + 180f) % 360

        // Check if  device is pointing toward the signal
        val withinThreshold = invertedAzimuth in 330.0..360.0 || invertedAzimuth in 0.0..30.0

        if (withinThreshold) {
            vibrateShortPulse()
        }
    }

    private fun vibrateShortPulse() {
        if (vibrator.hasVibrator()) {
            val effect = VibrationEffect.createOneShot(100, 255)
            vibrator.vibrate(effect)
        }
    }

    // Load the CSV file from assets, store in a map
    private fun loadDeviceData() {
        try {
            val inputStream = assets.open("beacons.csv")
            val reader = BufferedReader(InputStreamReader(inputStream))
            // Skip header row
            reader.readLine()
            val map = mutableMapOf<String, Pair<String, String>>()
            reader.forEachLine { line ->
                val tokens = line.split(",")
                if (tokens.size >= 3) {
                    val mac = tokens[0].trim()
                    val name = tokens[1].trim()
                    val url = tokens[2].trim()
                    map[mac] = Pair(name, url)
                }
            }
            reader.close()
            deviceMap = map
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Lookup the name associated with a MAC address.
    private fun lookupNameByMac(macAddress: String): String? {
        return deviceMap[macAddress]?.first
    }

}


