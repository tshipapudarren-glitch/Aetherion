package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        RiskConstitutionEntity::class,
        LocalStrategyEntity::class,
        LocalMemoryEntity::class,
        LocalAuditLogEntity::class,
        PaperOrderEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AetherDatabase : RoomDatabase() {
    abstract fun riskConstitutionDao(): RiskConstitutionDao
    abstract fun localStrategyDao(): LocalStrategyDao
    abstract fun localMemoryDao(): LocalMemoryDao
    abstract fun localAuditLogDao(): LocalAuditLogDao
    abstract fun paperOrderDao(): PaperOrderDao

    companion object {
        @Volatile
        private var INSTANCE: AetherDatabase? = null

        fun getInstance(context: Context): AetherDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AetherDatabase::class.java,
                    "aetherion_institutional.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
