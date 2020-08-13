package com.louis.app.cavity.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.louis.app.cavity.R
import com.louis.app.cavity.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater).also { setContentView(it.root) }

        setupDrawer()
    }

    private fun setupDrawer() {
        setSupportActionBar(binding.main.toolbar)

        val navController = findNavController(R.id.navHostFragment)
        val appBarConfiguration =
            AppBarConfiguration(navController.graph, binding.drawer)
        binding.navView.setupWithNavController(navController)
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.main.toolbar.setupWithNavController(navController, appBarConfiguration)
    }
}