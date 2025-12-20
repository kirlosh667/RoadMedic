package com.example.roadmedic

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ReportDao {

    @Insert
    fun insert(report: PotholeReportEntity)

    @Query("SELECT * FROM pothole_reports ORDER BY timestamp DESC")
    fun getAllReports(): List<PotholeReportEntity>

    @Query("DELETE FROM pothole_reports")
    fun clearAll()
}
