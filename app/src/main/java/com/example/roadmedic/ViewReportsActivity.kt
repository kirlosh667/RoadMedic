package com.example.roadmedic

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import java.io.File

class ViewReportsActivity : AppCompatActivity() {

    private lateinit var listReports: ListView
    private lateinit var tvTitle: TextView
    private lateinit var btnClearReports: Button

    private var rawLines: List<String> = emptyList()
    private var displayList: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_reports)

        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        setSupportActionBar(topAppBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        topAppBar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        tvTitle = findViewById(R.id.tvTitle)
        listReports = findViewById(R.id.listReports)
        btnClearReports = findViewById(R.id.btnClearReports)

        loadReports()

        btnClearReports.setOnClickListener {
            clearAllReports()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun loadReports() {
        val db = AppDatabase.getInstance(this)
        val reports = db.reportDao().getAllReports()

        if (reports.isEmpty()) {
            Toast.makeText(this, "No reports saved yet", Toast.LENGTH_SHORT).show()
            tvTitle.text = "Saved Pothole Reports (0)"
            listReports.adapter = null
            return
        }

        // rawLines used for passing data to detail screen (now includes address as 5th field)
        rawLines = reports.map { report ->
            val safeAddress = report.address?.replace("|", "/") ?: ""
            "${report.timestamp}|${report.imagePath}|${report.latitude}|${report.longitude}|$safeAddress"
        }

        displayList = reports.mapIndexed { index, report ->
            val timeString = android.text.format.DateFormat.format(
                "yyyy-MM-dd HH:mm:ss",
                report.timestamp
            )

            val severityLabel = when (report.severity) {
                1 -> "Low"
                2 -> "Medium"
                3 -> "High"
                else -> "Unknown"
            }

            val addressLine = report.address ?: ""

            buildString {
                append("Report #${index + 1}\n")
                append("Time: $timeString\n")
                append("Severity: $severityLabel\n")
                append("Lat: ${report.latitude}, Lon: ${report.longitude}")
                if (addressLine.isNotEmpty()) {
                    append("\n")
                    append(addressLine)
                }
            }
        }

        tvTitle.text = "Saved Pothole Reports (${displayList.size})"

        val adapter = ArrayAdapter(
            this,
            R.layout.list_item_report,
            R.id.txtItem,
            displayList
        )

        listReports.adapter = adapter

        listReports.setOnItemClickListener { _, _, position, _ ->
            val line = rawLines[position]
            val parts = line.split("|")
            if (parts.size >= 4) {
                val timestamp = parts[0]
                val imagePath = parts[1]
                val lat = parts[2]
                val lon = parts[3]
                val address = if (parts.size >= 5) parts[4] else null

                val intent = Intent(this, ReportDetailActivity::class.java)
                intent.putExtra("timestamp", timestamp)
                intent.putExtra("imagePath", imagePath)
                intent.putExtra("lat", lat)
                intent.putExtra("lon", lon)
                intent.putExtra("address", address)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Invalid report data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun clearAllReports() {
        val db = AppDatabase.getInstance(this)
        db.reportDao().clearAll()

        val imagesDir = File(filesDir, "images")
        if (imagesDir.exists() && imagesDir.isDirectory) {
            imagesDir.listFiles()?.forEach { it.delete() }
        }

        Toast.makeText(this, "All reports cleared", Toast.LENGTH_SHORT).show()

        tvTitle.text = "Saved Pothole Reports (0)"
        listReports.adapter = null
    }
}
