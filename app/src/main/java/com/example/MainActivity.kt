package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.data.InventoryDatabase
import com.example.data.InventoryRepository
import com.example.ui.InventoryViewModel
import com.example.ui.InventoryViewModelFactory
import com.example.ui.screens.FridgeAppMainScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

  private val database by lazy { InventoryDatabase.getDatabase(this) }
  private val repository by lazy { InventoryRepository(database.inventoryDao) }
  private val viewModel: InventoryViewModel by viewModels {
    InventoryViewModelFactory(repository)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        FridgeAppMainScreen(viewModel = viewModel)
      }
    }
  }
}
