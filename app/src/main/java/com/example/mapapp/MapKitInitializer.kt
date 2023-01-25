package com.example.mapapp

import android.content.Context
import com.yandex.mapkit.MapKitFactory

object MapKitInitializer {
    private var initialized = false

    fun initialize(context: Context) {
        if (initialized) {
            return
        }

        MapKitFactory.setApiKey("18c3a22c-a43b-4e3b-a5e3-4cf4da322159")
        MapKitFactory.initialize(context)
        initialized = true
    }
}