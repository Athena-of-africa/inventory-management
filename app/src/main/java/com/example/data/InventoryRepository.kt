package com.example.data

import kotlinx.coroutines.flow.Flow

class InventoryRepository(private val dao: InventoryDao) {

    val allItems: Flow<List<InventoryItem>> = dao.getAllItemsFlow()
    val allLogs: Flow<List<ActivityLog>> = dao.getAllLogsFlow()

    fun getItemsByHousehold(householdId: Int): Flow<List<InventoryItem>> {
        return dao.getItemsByHouseholdFlow(householdId)
    }

    fun getLogsByHousehold(householdId: Int): Flow<List<ActivityLog>> {
        return dao.getLogsByHouseholdFlow(householdId)
    }

    fun getUsersByHousehold(householdId: Int): Flow<List<User>> {
        return dao.getUsersByHouseholdFlow(householdId)
    }

    suspend fun getItemById(id: Int): InventoryItem? {
        return dao.getItemById(id)
    }

    suspend fun getItemByBarcode(barcode: String): InventoryItem? {
        return dao.getItemByBarcode(barcode)
    }

    suspend fun getItemByBarcodeAndHousehold(barcode: String, householdId: Int): InventoryItem? {
        return dao.getItemByBarcodeAndHousehold(barcode, householdId)
    }

    /**
     * Adds an item to the database or increments its quantity if it already exists.
     * Scoped to the specific family/household group.
     */
    suspend fun addItem(
        name: String,
        quantity: Int,
        unit: String,
        category: String,
        addedBy: String,
        barcode: String? = null,
        householdId: Int = 1
    ) {
        val existingItem = if (!barcode.isNullOrBlank()) {
            dao.getItemByBarcodeAndHousehold(barcode, householdId)
        } else {
            // Find manually by matching name & category & householdId
            null
        }

        val finalItem = existingItem?.copy(
            quantity = existingItem.quantity + quantity,
            status = "In Stock",
            dateAdded = System.currentTimeMillis()
        ) ?: InventoryItem(
            name = name,
            quantity = quantity,
            unit = unit,
            category = category,
            addedBy = addedBy,
            barcode = barcode,
            status = "In Stock",
            householdId = householdId
        )

        if (finalItem.id != 0) {
            dao.updateItem(finalItem)
        } else {
            dao.insertItem(finalItem)
        }

        // Write to log with household context
        dao.insertLog(
            ActivityLog(
                itemName = name,
                actionType = "ADD",
                quantity = quantity,
                personName = addedBy,
                householdId = householdId
            )
        )
    }

    /**
     * Lowers item quantity in the fridge. Scoped to the specific household.
     */
    suspend fun takeOutItem(
        itemId: Int,
        quantityToTake: Int,
        personName: String,
        householdId: Int = 1
    ): Boolean {
        val item = dao.getItemById(itemId) ?: return false
        if (item.quantity <= 0) return false

        val newQuantity = (item.quantity - quantityToTake).coerceAtLeast(0)
        val newStatus = when {
            newQuantity == 0 -> "Out of Stock"
            newQuantity <= 2 -> "Running Low"
            else -> "In Stock"
        }

        val updatedItem = item.copy(
            quantity = newQuantity,
            status = newStatus
        )
        dao.updateItem(updatedItem)

        // Write transaction with household context
        val actualTaken = if (item.quantity < quantityToTake) item.quantity else quantityToTake
        dao.insertLog(
            ActivityLog(
                itemName = item.name,
                actionType = "REMOVE",
                quantity = actualTaken,
                personName = personName,
                householdId = householdId
            )
        )
        return true
    }

    suspend fun deleteItem(item: InventoryItem) {
        dao.deleteItem(item)
    }

    // ====== COLLABORATION LOGIC & AUTH SEEDS ======

    suspend fun findUserByEmail(email: String): User? {
        return dao.getUserByEmail(email)
    }

    suspend fun registerUser(name: String, email: String, password: String = "123456", isAdmin: Boolean = false, householdId: Int = 1): User {
        val existing = dao.getUserByEmail(email)
        if (existing != null) {
            val updated = existing.copy(name = name, password = password, isAdmin = isAdmin, householdId = householdId)
            dao.insertUser(updated)
            return updated
        }
        val newUser = User(name = name, email = email, password = password, isAdmin = isAdmin, householdId = householdId)
        val generatedId = dao.insertUser(newUser)
        return newUser.copy(id = generatedId.toInt())
    }

    suspend fun removeUser(user: User) {
        dao.deleteUser(user)
    }

    suspend fun getOrCreateHouseholdByCode(name: String, inviteCode: String): Household {
        val existing = dao.getHouseholdByInviteCode(inviteCode)
        if (existing != null) return existing

        val household = Household(name = name, inviteCode = inviteCode)
        val generatedId = dao.insertHousehold(household)
        return household.copy(id = generatedId.toInt())
    }

    suspend fun getAllHouseholds(): List<Household> {
        return dao.getAllHouseholds()
    }

    suspend fun findHouseholdByInviteCode(code: String): Household? {
        return dao.getHouseholdByInviteCode(code)
    }
}
