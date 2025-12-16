package com.ext.smartbottomsheetlibrary

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ext.smart_bottomsheet_library.SmartBottomSheet

class MainActivity : AppCompatActivity() {

    private lateinit var bottomSheet: SmartBottomSheet

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize bottomSheet
        bottomSheet = findViewById(R.id.smartSheet)

        // Optional: Listen to state changes
        bottomSheet.setOnStateChangeListener { isExpanded ->
            // Handle state change if needed
        }
    }
}