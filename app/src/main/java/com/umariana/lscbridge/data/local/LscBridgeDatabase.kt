package com.umariana.lscbridge.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [AppMetadataEntity::class], version = 1, exportSchema = false)
abstract class LscBridgeDatabase : RoomDatabase()
