package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "inventory_items")
data class InventoryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val quantity: Int,
    val unit: String = "pcs",
    val category: String = "Produce",
    val dateAdded: Long = System.currentTimeMillis(),
    val status: String = "In Stock", // e.g., "In Stock", "Running Low", "Out of Stock"
    val addedBy: String = "User",
    val barcode: String? = null,
    val householdId: Int = 1 // Scoped to specific households
) : Serializable

@Entity(tableName = "activity_logs")
data class ActivityLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val itemName: String,
    val actionType: String, // "ADD" or "REMOVE" or "STOCK_OUT"
    val quantity: Int,
    val personName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val householdId: Int = 1 // Scoped to specific households
) : Serializable

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val email: String,
    val password: String = "123456",
    val isAdmin: Boolean = false,
    val householdId: Int = 1
) : Serializable

@Entity(tableName = "households")
data class Household(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val inviteCode: String
) : Serializable
