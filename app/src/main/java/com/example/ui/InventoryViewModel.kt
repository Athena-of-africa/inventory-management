package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class InventoryViewModel(private val repository: InventoryRepository) : ViewModel() {

    // === Multi-User & Group State Flows ===
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _currentHousehold = MutableStateFlow<Household?>(null)
    val currentHousehold: StateFlow<Household?> = _currentHousehold.asStateFlow()

    private val _households = MutableStateFlow<List<Household>>(emptyList())
    val households: StateFlow<List<Household>> = _households.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    // Scoped list updates dynamically on switching households
    @OptIn(ExperimentalCoroutinesApi::class)
    val items: StateFlow<List<InventoryItem>> = _currentHousehold
        .filterNotNull()
        .flatMapLatest { hh ->
            repository.getItemsByHousehold(hh.id)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val logs: StateFlow<List<ActivityLog>> = _currentHousehold
        .filterNotNull()
        .flatMapLatest { hh ->
            repository.getLogsByHousehold(hh.id)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentHouseholdMembers: StateFlow<List<User>> = _currentHousehold
        .filterNotNull()
        .flatMapLatest { hh ->
            repository.getUsersByHousehold(hh.id)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current scanner simulator state
    private val _scannedBarcode = MutableStateFlow<String?>(null)
    val scannedBarcode: StateFlow<String?> = _scannedBarcode.asStateFlow()

    private val _scannedMetadata = MutableStateFlow<ScannedItemDetails?>(null)
    val scannedMetadata: StateFlow<ScannedItemDetails?> = _scannedMetadata.asStateFlow()

    // Preset items for barcode mock scans
    val barcodePresetDatabase = mapOf(
        "5011234567" to ScannedItemDetails("Full Cream Milk", "Dairy", "carton", "5011234567"),
        "5011789123" to ScannedItemDetails("Organic Eggs", "Produce", "dozen", "5011789123"),
        "5011456234" to ScannedItemDetails("Royal Gala Apples", "Produce", "pcs", "5011456234"),
        "5011999888" to ScannedItemDetails("Fresh Orange Juice", "Beverages", "bottle", "5011999888"),
        "5011000111" to ScannedItemDetails("Soda Can", "Beverages", "can", "5011000111"),
        "5011122334" to ScannedItemDetails("Chicken Breasts", "Meat & Poultry", "pack", "5011122334")
    )

    init {
        // Seed default multi-user database
        viewModelScope.launch {
            val primaryHouse = repository.getOrCreateHouseholdByCode("Sweet family home", "SMITH123")
            val secondaryHouse = repository.getOrCreateHouseholdByCode("Office breakroom", "WORK456")
            
            _households.value = repository.getAllHouseholds()

            // Register default family members with predefined passwords and admin flags
            repository.registerUser(
                name = "Admin Dave",
                email = "admin@smith.com",
                password = "admin123",
                isAdmin = true,
                householdId = primaryHouse.id
            )
            val alex = repository.registerUser(
                name = "Chef Alex",
                email = "alex@smith.com",
                password = "alexpassword",
                isAdmin = false,
                householdId = primaryHouse.id
            )
            repository.registerUser(
                name = "Alice Smith",
                email = "alice@smith.com",
                password = "alicepassword",
                isAdmin = false,
                householdId = primaryHouse.id
            )
            repository.registerUser(
                name = "Tommy Miller",
                email = "tommy@family.com",
                password = "tommypassword",
                isAdmin = false,
                householdId = primaryHouse.id
            )

            // Register office workers
            repository.registerUser(
                name = "Manager Kate",
                email = "kate@office.com",
                password = "katepassword",
                isAdmin = true,
                householdId = secondaryHouse.id
            )
            repository.registerUser(
                name = "Intern Joe",
                email = "joe@office.com",
                password = "joepassword",
                isAdmin = false,
                householdId = secondaryHouse.id
            )

            // On startup, we pre-select the primary household compartment,
            // but keep authentication false until explicit login occurs.
            _currentHousehold.value = primaryHouse
            _currentUser.value = null
            _isAuthenticated.value = false

            // Pre-seed some items to default primary house if empty
            val startItemsFlow = repository.getItemsByHousehold(primaryHouse.id).first()
            if (startItemsFlow.isEmpty()) {
                repository.addItem("Organic Eggs", 12, "pcs", "Produce", "Chef Alex", "5011789123", primaryHouse.id)
                repository.addItem("Full Cream Milk", 2, "carton", "Dairy", "Alice Smith", "5011234567", primaryHouse.id)
                repository.addItem("Fresh Orange Juice", 1, "bottle", "Beverages", "Chef Alex", "5011999888", primaryHouse.id)
            }

            // Pre-seed for secondary house
            val officeItemsFlow = repository.getItemsByHousehold(secondaryHouse.id).first()
            if (officeItemsFlow.isEmpty()) {
                repository.addItem("Soda Can", 24, "can", "Beverages", "Manager Kate", "5011000111", secondaryHouse.id)
                repository.addItem("Chicken Breasts", 3, "pack", "Meat & Poultry", "Manager Kate", "5011122334", secondaryHouse.id)
            }
        }
    }

    fun onBarcodeScanned(barcode: String) {
        viewModelScope.launch {
            _scannedBarcode.value = barcode
            val hhId = _currentHousehold.value?.id ?: 1
            val existingItem = repository.getItemByBarcodeAndHousehold(barcode, hhId)
            if (existingItem != null) {
                _scannedMetadata.value = ScannedItemDetails(
                    name = existingItem.name,
                    category = existingItem.category,
                    unit = existingItem.unit,
                    barcode = existingItem.barcode,
                    isAlreadyInInventory = true,
                    currentQtyInInventory = existingItem.quantity
                )
            } else {
                val preset = barcodePresetDatabase[barcode]
                if (preset != null) {
                    _scannedMetadata.value = preset
                } else {
                    _scannedMetadata.value = ScannedItemDetails(
                        name = "",
                        category = "Other",
                        unit = "pcs",
                        barcode = barcode,
                        isAlreadyInInventory = false
                    )
                }
            }
        }
    }

    fun clearScannedResult() {
        _scannedBarcode.value = null
        _scannedMetadata.value = null
    }

    // High Level Operations (fully Multi-User & Household aware)
    fun addItem(
        name: String,
        quantity: Int,
        unit: String,
        category: String,
        addedBy: String,
        barcode: String? = null
    ) {
        viewModelScope.launch {
            val hhId = _currentHousehold.value?.id ?: 1
            repository.addItem(name, quantity, unit, category, addedBy, barcode, hhId)
        }
    }

    fun takeOutItem(itemId: Int, quantityToTake: Int, personName: String, onFinished: (Boolean) -> Unit) {
        viewModelScope.launch {
            val hhId = _currentHousehold.value?.id ?: 1
            val success = repository.takeOutItem(itemId, quantityToTake, personName, hhId)
            onFinished(success)
        }
    }

    fun deleteItem(item: InventoryItem) {
        viewModelScope.launch {
            repository.deleteItem(item)
        }
    }

    // ====== AUTHENTICATION & MULTI-USER SERVICES ======

    fun selectUser(user: User) {
        _currentUser.value = user
    }

    fun selectHousehold(household: Household) {
        viewModelScope.launch {
            _currentHousehold.value = household
            // Load members of this household
            val members = repository.getUsersByHousehold(household.id).first()
            if (members.isNotEmpty()) {
                _currentUser.value = members.firstOrNull { it.isAdmin } ?: members.first()
            } else {
                // Register a fallback Admin user for this empty household
                val defaultUser = repository.registerUser(
                    name = "Admin",
                    email = "admin@domain.com",
                    password = "admin123",
                    isAdmin = true,
                    householdId = household.id
                )
                _currentUser.value = defaultUser
            }
        }
    }

    // Connect user and check username and password
    fun handleCredentialSignIn(emailText: String, passwordText: String, onFinished: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val email = emailText.trim()
            val password = passwordText.trim()
            if (email.isBlank() || password.isBlank()) {
                onFinished(false, "Please enter both Email and Password.")
                return@launch
            }
            val existingUser = repository.findUserByEmail(email)
            if (existingUser == null) {
                onFinished(false, "No user account registered with this email. Try out default addresses under accounts list.")
                return@launch
            }
            if (existingUser.password != password) {
                onFinished(false, "Incorrect password. Please try again.")
                return@launch
            }
            
            // Successfully Authenticated! Set flows safely.
            _currentUser.value = existingUser
            
            // Set household corresponding to user's assigned household
            val allH = repository.getAllHouseholds()
            val assignedH = allH.find { it.id == existingUser.householdId }
            if (assignedH != null) {
                _currentHousehold.value = assignedH
            } else if (allH.isNotEmpty()) {
                _currentHousehold.value = allH.first()
            }
            
            _isAuthenticated.value = true
            onFinished(true, "Successfully logged in as ${existingUser.name}!")
        }
    }

    // Connection signup process
    fun handleCredentialSignUp(
        fullName: String,
        emailText: String,
        passwordText: String,
        householdInviteCode: String,
        onFinished: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val name = fullName.trim()
            val email = emailText.trim()
            val password = passwordText.trim()
            val inviteCode = householdInviteCode.trim().uppercase()

            if (name.isBlank() || email.isBlank() || password.isBlank() || inviteCode.isBlank()) {
                onFinished(false, "All fields are required to create an account.")
                return@launch
            }

            // Find or create the target household context
            val hostHousehold = repository.findHouseholdByInviteCode(inviteCode)
            if (hostHousehold == null) {
                onFinished(false, "Invite code '#$inviteCode' does not exist. Use 'SMITH123' or create a new group.")
                return@launch
            }

            val registeredUser = repository.registerUser(
                name = name,
                email = email,
                password = password,
                isAdmin = false, // Self sign ups are standard users
                householdId = hostHousehold.id
            )

            _currentUser.value = registeredUser
            _currentHousehold.value = hostHousehold
            _isAuthenticated.value = true
            onFinished(true, "Register success! Logged into #${hostHousehold.name}")
        }
    }

    // Creates a new household first, makes the registering user an Admin of it, and authenticates the session
    fun handleCreateHouseholdAndSignUp(
        fullName: String,
        emailText: String,
        passwordText: String,
        householdNameText: String,
        householdInviteCode: String,
        onFinished: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val name = fullName.trim()
            val email = emailText.trim()
            val password = passwordText.trim()
            val houseName = householdNameText.trim()
            val inviteCode = householdInviteCode.trim().uppercase()

            if (name.isBlank() || email.isBlank() || password.isBlank() || houseName.isBlank() || inviteCode.isBlank()) {
                onFinished(false, "All fields are required to create a household and register.")
                return@launch
            }

            // Verify if a user with this email already exists
            val existingUser = repository.findUserByEmail(email)
            if (existingUser != null) {
                onFinished(false, "A user with email '$email' is already registered. Please sign in instead.")
                return@launch
            }

            // Verify if the invite code is already taken
            val existingHh = repository.findHouseholdByInviteCode(inviteCode)
            if (existingHh != null) {
                onFinished(false, "Sync code '#$inviteCode' is already taken. Please choose another code.")
                return@launch
            }

            // Create new household
            val newHousehold = repository.getOrCreateHouseholdByCode(houseName, inviteCode)

            // Since they are creating a brand new household, they are registered as an Admin!
            val registeredUser = repository.registerUser(
                name = name,
                email = email,
                password = password,
                isAdmin = true, // Creator is Admin
                householdId = newHousehold.id
            )

            _currentUser.value = registeredUser
            _currentHousehold.value = newHousehold
            _isAuthenticated.value = true
            onFinished(true, "Household #${newHousehold.name} successfully created! You are logged in as Admin.")
        }
    }

    // Handles administrative provisioning of family members
    fun handleAdminProvisionMember(
        nameText: String,
        emailText: String,
        passwordText: String,
        isAdminFlag: Boolean,
        onFinished: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val activeAdmin = _currentUser.value
            if (activeAdmin == null || !activeAdmin.isAdmin) {
                onFinished(false, "Access Denied: Only Admin users can provision accounts.")
                return@launch
            }

            val name = nameText.trim()
            val email = emailText.trim()
            val password = passwordText.trim()
            if (name.isBlank() || email.isBlank() || password.isBlank()) {
                onFinished(false, "Please specify Name, Email, and Password.")
                return@launch
            }

            val activeHhId = _currentHousehold.value?.id ?: 1
            repository.registerUser(
                name = name,
                email = email,
                password = password,
                isAdmin = isAdminFlag,
                householdId = activeHhId
            )
            onFinished(true, "Provisioned account credentials for $name!")
        }
    }

    fun handleRegisterAndJoinHousehold(
        houseName: String,
        inviteCode: String,
        onFinished: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            if (houseName.isBlank() || inviteCode.isBlank()) {
                onFinished(false, "Household Name and Invite Code are required")
                return@launch
            }
            val household = repository.getOrCreateHouseholdByCode(houseName, inviteCode)
            _households.value = repository.getAllHouseholds()
            selectHousehold(household)
            onFinished(true, "Created and switched to household: ${household.name}")
        }
    }

    fun handleJoinByInviteCode(inviteCode: String, onFinished: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val hh = repository.findHouseholdByInviteCode(inviteCode)
            if (hh != null) {
                selectHousehold(hh)
                onFinished(true, "Joined household ${hh.name} successfully!")
            } else {
                onFinished(false, "Invalid invite code. Try SMITH123 or WORK456, or create a new one.")
            }
        }
    }

    fun handleRemoveUserFromActiveGroup(user: User, onFinished: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val activeAdmin = _currentUser.value
            if (activeAdmin == null || !activeAdmin.isAdmin) {
                onFinished(false, "Access Denied: Only the provisioned administrative user can delete members.")
                return@launch
            }
            if (activeAdmin.id == user.id) {
                onFinished(false, "You cannot remove yourself from the active session.")
                return@launch
            }
            repository.removeUser(user)
            onFinished(true, "Successfully removed ${user.name} from directory registration.")
        }
    }

    fun handleSignOut() {
        _currentUser.value = null
        _isAuthenticated.value = false
    }
}

data class ScannedItemDetails(
    val name: String,
    val category: String,
    val unit: String,
    val barcode: String?,
    val isAlreadyInInventory: Boolean = false,
    val currentQtyInInventory: Int = 0
)

class InventoryViewModelFactory(private val repository: InventoryRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InventoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return InventoryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
