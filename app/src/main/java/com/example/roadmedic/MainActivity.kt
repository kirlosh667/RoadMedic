package com.example.roadmedic


import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        setSupportActionBar(topAppBar)
        // No back button here – this is the root screen

        val btnReportPothole = findViewById<Button>(R.id.btnReportPothole)
        val btnViewReports = findViewById<Button>(R.id.btnViewReports)
        val btnViewMap = findViewById<Button>(R.id.btnViewMap)

        btnReportPothole.setOnClickListener {
            startActivity(Intent(this, ReportPotholeActivity::class.java))
        }

        btnViewReports.setOnClickListener {
            startActivity(Intent(this, ViewReportsActivity::class.java))
        }

        btnViewMap.setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
        }
    }
}
