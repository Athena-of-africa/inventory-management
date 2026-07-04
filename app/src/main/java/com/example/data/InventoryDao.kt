package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {
    // Inventory Items operations (global fallback + household-scoped)
    @Query("SELECT * FROM inventory_items ORDER BY category ASC, name ASC")
    fun getAllItemsFlow(): Flow<List<InventoryItem>>

    @Query("SELECT * FROM inventory_items WHERE householdId = :householdId ORDER BY category ASC, name ASC")
    fun getItemsByHouseholdFlow(householdId: Int): Flow<List<InventoryItem>>

    @Query("SELECT * FROM inventory_items WHERE id = :id")
    suspend fun getItemById(id: Int): InventoryItem?

    @Query("SELECT * FROM inventory_items WHERE barcode = :barcode LIMIT 1")
    suspend fun getItemByBarcode(barcode: String): InventoryItem?

    @Query("SELECT * FROM inventory_items WHERE barcode = :barcode AND householdId = :householdId LIMIT 1")
    suspend fun getItemByBarcodeAndHousehold(barcode: String, householdId: Int): InventoryItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: InventoryItem): Long

    @Update
    suspend fun updateItem(item: InventoryItem): Int

    @Delete
    suspend fun deleteItem(item: InventoryItem): Int

    // Activity Log operations (global + household-scoped)
    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC")
    fun getAllLogsFlow(): Flow<List<ActivityLog>>

    @Query("SELECT * FROM activity_logs WHERE householdId = :householdId ORDER BY timestamp DESC")
    fun getLogsByHouseholdFlow(householdId: Int): Flow<List<ActivityLog>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLog(log: ActivityLog): Long

    // ====== NEW INVENTORY OPERATIONS FOR USERS & HOUSEHOLDS ======
    
    // User Operations
    @Query("SELECT * FROM users WHERE householdId = :householdId")
    fun getUsersByHouseholdFlow(householdId: Int): Flow<List<User>>

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long

    @Delete
    suspend fun deleteUser(user: User)

    // Household Operations
    @Query("SELECT * FROM households")
    suspend fun getAllHouseholds(): List<Household>

    @Query("SELECT * FROM households WHERE inviteCode = :code LIMIT 1")
    suspend fun getHouseholdByInviteCode(code: String): Household?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHousehold(household: Household): Long
}
