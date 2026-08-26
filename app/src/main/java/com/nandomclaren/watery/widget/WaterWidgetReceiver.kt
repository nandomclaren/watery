package com.nandomclaren.watery.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class WaterWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = WaterWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WaterWidgetWorker.enqueuePeriodic(context)
    }
}
