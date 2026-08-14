package com.darkjade.streamlib.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ScanState { IDLE, SCANNING, COMPLETED, FAILED }

/** Single-row table tracking the most recent library scan status. */
@Entity(tableName = "scan_status")
data class ScanStatusEntity(
    @PrimaryKey val id: Int = 0,
    val state: ScanState = ScanState.IDLE,
    val filesFound: Int = 0,
    val filesProcessed: Int = 0,
    val startedAt: Long? = null,
    val finishedAt: Long? = null,
    val errorMessage: String? = null,
)
