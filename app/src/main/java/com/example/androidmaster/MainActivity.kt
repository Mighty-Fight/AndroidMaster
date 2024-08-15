package com.example.androidmaster

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.SmsManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*

class MainActivity : AppCompatActivity() {

    private lateinit var locationStatus: TextView
    private lateinit var buttonSubmit: Button
    private lateinit var editTextPhone: EditText

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest

    private var latitude: String? = null
    private var longitude: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout)

        // Initialize views
        locationStatus = findViewById(R.id.locationStatus)
        buttonSubmit = findViewById(R.id.buttonSubmit)
        editTextPhone = findViewById(R.id.editTextPhone)

        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Create LocationRequest
        locationRequest = LocationRequest.create().apply {
            interval = 10000 // Update interval in milliseconds
            fastestInterval = 5000 // Fastest update interval in milliseconds
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        // Initialize LocationCallback
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                if (locationResult.locations.isNotEmpty()) {
                    val location = locationResult.locations.last()
                    latitude = location.latitude.toString()
                    longitude = location.longitude.toString()
                    locationStatus.text = "Ubicación: Latitud $latitude, Longitud $longitude"
                }
            }
        }

        // Check permissions
        checkPermissions()

        // Set up button click listener
        buttonSubmit.setOnClickListener {
            // Handle button click
            sendLocationViaSMS()
        }
    }

    private fun checkPermissions() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED -> {
                // Permissions granted, start location updates
                startLocationUpdates()
            }
            else -> {
                // Request permissions
                requestPermissions()
            }
        }
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.SEND_SMS),
            PERMISSIONS_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // All permissions granted, start location updates
                startLocationUpdates()
            } else {
                // Permissions denied, show message
                locationStatus.text = "Permisos no otorgados"
            }
        }
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun sendLocationViaSMS() {
        val phoneNumber = editTextPhone.text.toString()
        if (phoneNumber.isNotEmpty()) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                val message = "Ubicación: Latitud $latitude, Longitud $longitude"
                try {
                    SmsManager.getDefault().sendTextMessage(phoneNumber, null, message, null, null)
                    locationStatus.text = "SMS enviado a $phoneNumber"
                } catch (e: Exception) {
                    locationStatus.text = "Error al enviar SMS: ${e.message}"
                }
            } else {
                // Request SMS permission
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.SEND_SMS),
                    SMS_PERMISSION_REQUEST_CODE
                )
            }
        } else {
            locationStatus.text = "Número de teléfono vacío"
        }
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates() // Stop location updates when activity is paused
    }

    override fun onResume() {
        super.onResume()
        checkPermissions() // Check permissions and start location updates if granted
    }

    companion object {
        private const val PERMISSIONS_REQUEST_CODE = 1
        private const val SMS_PERMISSION_REQUEST_CODE = 2
    }
}
