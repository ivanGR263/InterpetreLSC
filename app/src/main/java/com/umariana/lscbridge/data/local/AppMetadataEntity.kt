package com.umariana.lscbridge.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_metadata")
data class AppMetadataEntity(
    @PrimaryKey val id: Int = 1,
    val lastUpdatedAtMillis: Long = 0L
)
