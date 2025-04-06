package com.example.myapplication

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log

class DeviceOrientationManager(context: Context) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private var accelerometerReading = FloatArray(3)
    private var magnetometerReading = FloatArray(3)

    // Callback when azimuth changes.
    var onOrientationChangedCallback: ((azimuth: Float) -> Unit)? = null

    fun start() {
        //always be listening for device movement, get back in big arr
        accelerometer?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        magnetometer?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            when (it.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    accelerometerReading = it.values.clone()
                }
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    magnetometerReading = it.values.clone()
                }
            }
        }

        val R = FloatArray(9)
        val I = FloatArray(9)
        if (SensorManager.getRotationMatrix(R, I, accelerometerReading, magnetometerReading)) {
            val orientation = FloatArray(3)
            SensorManager.getOrientation(R, orientation)
            val azimuth = orientation[0]
            onOrientationChangedCallback?.invoke(azimuth)
            Log.d("DeviceOrientation", "Azimuth: $azimuth")
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        //Not really needed but abstract so it makes me
    }
}
