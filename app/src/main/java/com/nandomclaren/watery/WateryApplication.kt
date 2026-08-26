package com.nandomclaren.watery

import android.app.Application
import com.nandomclaren.watery.widget.WaterWidgetWorker

class WateryApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        WaterWidgetWorker.enqueuePeriodic(this)
    }
}
