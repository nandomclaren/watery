package com.nandomclaren.watery.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Periodically refreshes the widget so it (a) resyncs today's total from Health
 * Connect and (b) resets itself shortly after local midnight, without requiring
 * exact-alarm permissions.
 */
class WaterWidgetWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        WaterWidget().updateAll(applicationContext)
        return Result.success()
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "watery_widget_refresh"

        fun enqueuePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<WaterWidgetWorker>(30, TimeUnit.MINUTES)
                .setConstraints(Constraints.NONE)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
