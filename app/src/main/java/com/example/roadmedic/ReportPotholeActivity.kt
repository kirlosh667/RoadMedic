package com.example.roadmedic

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.appbar.MaterialToolbar
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

class ReportPotholeActivity : AppCompatActivity() {

    private lateinit var imgPreview: ImageView
    private lateinit var txtLocationOutput: TextView
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // store latest captured data
    private var latestBitmap: Bitmap? = null
    private var lastLat: Double? = null
    private var lastLon: Double? = null
    private var lastAddress: String? = null   // NEW: store address text

    // ---------------- CAMERA RESULT LAUNCHER ----------------
    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data

                val imageBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    data?.extras?.getParcelable("data", Bitmap::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    data?.extras?.get("data") as? Bitmap
                }

                if (imageBitmap != null) {
                    latestBitmap = imageBitmap
                    imgPreview.setImageBitmap(imageBitmap)
                } else {
                    Toast.makeText(this, "No image captured", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Camera cancelled", Toast.LENGTH_SHORT).show()
            }
        }

    // ---------------- PERMISSION LAUNCHERS ----------------

    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                openCamera()
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    private val requestLocationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                getLocationOnce()
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    // ---------------- LIFECYCLE ----------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_pothole)

        // AppBar (back button)
        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        setSupportActionBar(topAppBar)
        topAppBar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val btnCapturePhoto = findViewById<Button>(R.id.btnCapturePhoto)
        val btnGetLocation = findViewById<Button>(R.id.btnGetLocation)
        val btnSaveReport = findViewById<Button>(R.id.btnSaveReport)
        txtLocationOutput = findViewById(R.id.txtLocationOutput)
        imgPreview = findViewById(R.id.imgPreview)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        btnCapturePhoto.setOnClickListener {
            openCameraWithPermission()
        }

        btnGetLocation.setOnClickListener {
            openLocationWithPermission()
        }

        btnSaveReport.setOnClickListener {
            saveReport()
        }
    }

    // ---------------- CAMERA FLOW ----------------

    private fun openCameraWithPermission() {
        val hasCameraPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasCameraPermission) {
            openCamera()
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (cameraIntent.resolveActivity(packageManager) != null) {
            cameraLauncher.launch(cameraIntent)
        } else {
            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show()
        }
    }

    // ---------------- LOCATION FLOW ----------------

    private fun openLocationWithPermission() {
        val hasLocationPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasLocationPermission) {
            getLocationOnce()
        } else {
            requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun getLocationOnce() {
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        lastLat = location.latitude
                        lastLon = location.longitude
                        lastAddress = null     // reset before fetching new one
                        txtLocationOutput.text = "Location: $lastLat, $lastLon"

                        // After lat/lon, fetch human-readable address
                        fetchAddress(lastLat!!, lastLon!!)
                    } else {
                        txtLocationOutput.text = "Location: Not available (try again)"
                        Toast.makeText(this, "Could not get location", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    txtLocationOutput.text = "Location: Error"
                    Toast.makeText(this, "Error getting location", Toast.LENGTH_SHORT).show()
                }
        } catch (e: SecurityException) {
            Toast.makeText(this, "Location permission error", Toast.LENGTH_SHORT).show()
        }
    }

    // Reverse geocode lat/lon → address string and save it in lastAddress
    private fun fetchAddress(lat: Double, lon: Double) {
        Thread {
            try {
                val geocoder = Geocoder(this, Locale.getDefault())
                val results = geocoder.getFromLocation(lat, lon, 1)

                val address = results?.firstOrNull()
                val addressLine = when {
                    address == null -> null
                    !address.getAddressLine(0).isNullOrEmpty() -> address.getAddressLine(0)
                    else -> {
                        val parts = listOfNotNull(
                            address.locality,
                            address.subAdminArea,
                            address.adminArea,
                            address.countryName
                        )
                        if (parts.isNotEmpty()) parts.joinToString(", ") else null
                    }
                }

                runOnUiThread {
                    if (!addressLine.isNullOrEmpty()) {
                        lastAddress = addressLine   // store for DB
                        txtLocationOutput.text = "Location: $lat, $lon\n$addressLine"
                    } else {
                        lastAddress = null
                        txtLocationOutput.text = "Location: $lat, $lon"
                        Toast.makeText(this, "Couldn't get place name", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    lastAddress = null
                    Toast.makeText(this, "Error fetching address", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    // ---------------- SAVE REPORT (to Room + file) ----------------

    private fun saveReport() {
        val bitmap = latestBitmap
        val lat = lastLat
        val lon = lastLon

        if (bitmap == null) {
            Toast.makeText(this, "Please capture a photo first", Toast.LENGTH_SHORT).show()
            return
        }
        if (lat == null || lon == null) {
            Toast.makeText(this, "Please get location first", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val imagesDir = File(filesDir, "images")
            if (!imagesDir.exists()) {
                imagesDir.mkdirs()
            }

            val timestamp = System.currentTimeMillis()
            val imageFileName = "pothole_$timestamp.jpg"
            val imageFile = File(imagesDir, imageFileName)

            val out = FileOutputStream(imageFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            out.flush()
            out.close()

            // Severity from RadioGroup
            val rgSeverity = findViewById<RadioGroup>(R.id.rgSeverity)
            val checkedId = rgSeverity.checkedRadioButtonId
            val severityValue = when (checkedId) {
                R.id.rbLow -> 1
                R.id.rbMedium -> 2
                R.id.rbHigh -> 3
                else -> 1
            }

            // Insert into Room
            val db = AppDatabase.getInstance(this)
            val report = PotholeReportEntity(
                timestamp = timestamp,
                imagePath = imageFile.absolutePath,
                latitude = lat,
                longitude = lon,
                severity = severityValue,
                address = lastAddress   // NEW: save address into DB
            )
            db.reportDao().insert(report)

            Toast.makeText(this, "Report saved!", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error saving report", Toast.LENGTH_SHORT).show()
        }
    }
}
