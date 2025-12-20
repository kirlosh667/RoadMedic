package com.example.roadmedic

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

class MapActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private lateinit var mMap: GoogleMap
    private val markerPositions = mutableListOf<LatLng>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        // Find the map fragment from the layout
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as? SupportMapFragment

        if (mapFragment == null) {
            Toast.makeText(this, "Map fragment not found in layout", Toast.LENGTH_LONG).show()
            return
        }

        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setOnMarkerClickListener(this)
        showPotholeMarkers()
    }

    private fun showPotholeMarkers() {
        val db = try {
            AppDatabase.getInstance(this)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error opening database", Toast.LENGTH_SHORT).show()
            return
        }

        val reports = try {
            db.reportDao().getAllReports()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error reading reports from DB", Toast.LENGTH_SHORT).show()
            return
        }

        if (reports.isEmpty()) {
            Toast.makeText(this, "No reports to show on map", Toast.LENGTH_SHORT).show()
            return
        }

        markerPositions.clear()
        var firstLatLng: LatLng? = null

        reports.forEachIndexed { index, report ->
            val lat = report.latitude
            val lon = report.longitude

            val position = LatLng(lat, lon)
            markerPositions.add(position)

            if (firstLatLng == null) {
                firstLatLng = position
            }

            val marker = mMap.addMarker(
                MarkerOptions()
                    .position(position)
                    .title("Pothole #${index + 1}")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            )

            val safeAddress = report.address?.replace("|", "/") ?: ""

            // Tag format: timestamp|imagePath|lat|lon|address
            val tagLine =
                "${report.timestamp}|${report.imagePath}|${report.latitude}|${report.longitude}|$safeAddress"
            marker?.tag = tagLine
        }

        // Focus on first marker
        firstLatLng?.let {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(it, 14f))
        }
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        val line = marker.tag as? String ?: return false

        val parts = line.split("|")
        if (parts.size < 4) return true

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

        return true
    }
}
