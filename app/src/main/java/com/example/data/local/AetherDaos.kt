package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RiskConstitutionDao {
    @Query("SELECT * FROM risk_constitution WHERE id = 'constitution_singleton' LIMIT 1")
    fun observeConfig(): Flow<RiskConstitutionEntity?>

    @Query("SELECT * FROM risk_constitution WHERE id = 'constitution_singleton' LIMIT 1")
    suspend fun getConfig(): RiskConstitutionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(config: RiskConstitutionEntity)
}

@Dao
interface LocalStrategyDao {
    @Query("SELECT * FROM local_strategies ORDER BY sharpeRatio DESC")
    fun observeAll(): Flow<List<LocalStrategyEntity>>

    @Query("SELECT * FROM local_strategies")
    suspend fun getAll(): List<LocalStrategyEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(strategy: LocalStrategyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(strategies: List<LocalStrategyEntity>)

    @Query("DELETE FROM local_strategies WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface LocalMemoryDao {
    @Query("SELECT * FROM local_memories ORDER BY createdAtMs DESC")
    fun observeAll(): Flow<List<LocalMemoryEntity>>

    @Query("SELECT * FROM local_memories ORDER BY createdAtMs DESC")
    suspend fun getAll(): List<LocalMemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(memory: LocalMemoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(memories: List<LocalMemoryEntity>)

    @Query("DELETE FROM local_memories WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface LocalAuditLogDao {
    @Query("SELECT * FROM local_audit_logs ORDER BY createdAtMs DESC LIMIT 250")
    fun observeRecent(): Flow<List<LocalAuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: LocalAuditLogEntity)
}

@Dao
interface PaperOrderDao {
    @Query("SELECT * FROM paper_orders ORDER BY openedAtMs DESC")
    fun observeAll(): Flow<List<PaperOrderEntity>>

    @Query("SELECT * FROM paper_orders WHERE status = 'OPEN'")
    suspend fun getOpenOrders(): List<PaperOrderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(order: PaperOrderEntity)
}
