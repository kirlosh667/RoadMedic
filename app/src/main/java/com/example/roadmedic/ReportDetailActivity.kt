package com.example.roadmedic

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import java.io.File

class ReportDetailActivity : AppCompatActivity() {

    private var lat: String? = null
    private var lon: String? = null
    private var imagePath: String? = null
    private var timestamp: String? = null
    private var address: String? = null   // NEW

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_detail)

        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        setSupportActionBar(topAppBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        topAppBar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val imgDetail = findViewById<ImageView>(R.id.imgDetail)
        val tvDetailTime = findViewById<TextView>(R.id.tvDetailTime)
        val tvDetailLocation = findViewById<TextView>(R.id.tvDetailLocation)
        val tvDetailPath = findViewById<TextView>(R.id.tvDetailPath)
        val btnOpenInMaps = findViewById<Button>(R.id.btnOpenInMaps)
        val btnShareReport = findViewById<Button>(R.id.btnShareReport)

        timestamp = intent.getStringExtra("timestamp")
        imagePath = intent.getStringExtra("imagePath")
        lat = intent.getStringExtra("lat")
        lon = intent.getStringExtra("lon")
        address = intent.getStringExtra("address")  // NEW

        if (timestamp == null || imagePath == null || lat == null || lon == null) {
            Toast.makeText(this, "Missing report data", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val timeString = android.text.format.DateFormat.format(
            "yyyy-MM-dd HH:mm:ss",
            timestamp!!.toLongOrNull() ?: 0L
        )

        tvDetailTime.text = getString(R.string.report_detail_time, timeString)

        val baseLocationText = getString(R.string.report_detail_location, lat, lon)
        tvDetailLocation.text = if (!address.isNullOrEmpty()) {
            "$baseLocationText\n$address"
        } else {
            baseLocationText
        }

        tvDetailPath.text = getString(R.string.report_detail_image_path, imagePath)

        val imgFile = File(imagePath!!)
        if (imgFile.exists()) {
            val bitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
            imgDetail.setImageBitmap(bitmap)
        } else {
            Toast.makeText(this, "Image file not found", Toast.LENGTH_SHORT).show()
        }

        btnOpenInMaps.setOnClickListener {
            openInMaps()
        }

        btnShareReport.setOnClickListener {
            shareReport()
        }
    }

    private fun openInMaps() {
        val latVal = lat?.toDoubleOrNull()
        val lonVal = lon?.toDoubleOrNull()

        if (latVal == null || lonVal == null) {
            Toast.makeText(this, "Invalid location", Toast.LENGTH_SHORT).show()
            return
        }

        val uri = Uri.parse("geo:$latVal,$lonVal?q=$latVal,$lonVal(Pothole)")
        val mapIntent = Intent(Intent.ACTION_VIEW, uri)
        mapIntent.setPackage("com.google.android.apps.maps")

        if (mapIntent.resolveActivity(packageManager) != null) {
            startActivity(mapIntent)
        } else {
            mapIntent.setPackage(null)
            startActivity(mapIntent)
        }
    }

    private fun shareReport() {
        val timeString = android.text.format.DateFormat.format(
            "yyyy-MM-dd HH:mm:ss",
            timestamp?.toLongOrNull() ?: 0L
        )

        val addressLine = address ?: "Not available"

        val shareText = """
            RoadMedic Pothole Report
            Time: $timeString
            Location: $lat, $lon
            Address: $addressLine
        """.trimIndent()

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Pothole Report")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }

        startActivity(Intent.createChooser(shareIntent, "Share report via"))
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
