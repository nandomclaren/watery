package com.nandomclaren.watery.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Volume
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.roundToInt

private const val HEALTH_CONNECT_PACKAGE = "com.google.android.apps.healthdata"

class HealthConnectManager(private val context: Context) {

    val permissions: Set<String> = setOf(
        HealthPermission.getWritePermission(HydrationRecord::class),
        HealthPermission.getReadPermission(HydrationRecord::class),
    )

    val isAvailable: Boolean
        get() = HealthConnectClient.getSdkStatus(context, HEALTH_CONNECT_PACKAGE) ==
            HealthConnectClient.SDK_AVAILABLE

    val needsProviderUpdate: Boolean
        get() = HealthConnectClient.getSdkStatus(context, HEALTH_CONNECT_PACKAGE) ==
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED

    private val client: HealthConnectClient? by lazy {
        if (isAvailable) HealthConnectClient.getOrCreate(context) else null
    }

    fun createPermissionRequestContract() =
        PermissionController.createRequestPermissionResultContract()

    suspend fun hasAllPermissions(): Boolean {
        val c = client ?: return false
        return c.permissionController.getGrantedPermissions().containsAll(permissions)
    }

    fun openHealthConnectInPlayStore() {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(
                "market://details?id=$HEALTH_CONNECT_PACKAGE&url=healthconnect%3A%2F%2Fonboarding",
            )
            setPackage("com.android.vending")
            putExtra("overlay", true)
            putExtra("callerId", context.packageName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    /** Inserts a hydration record and returns its record id for later undo. */
    suspend fun insertGlass(volumeMl: Int): String? {
        val c = client ?: return null
        val now = Instant.now()
        val offset = ZonedDateTime.now().offset
        val record = HydrationRecord(
            startTime = now.minusSeconds(1),
            startZoneOffset = offset,
            endTime = now,
            endZoneOffset = offset,
            volume = Volume.milliliters(volumeMl.toDouble()),
            metadata = Metadata.manualEntry(),
        )
        val result = c.insertRecords(listOf(record))
        return result.recordIdsList.firstOrNull()
    }

    suspend fun deleteRecord(recordId: String) {
        val c = client ?: return
        c.deleteRecords(HydrationRecord::class, recordIdsList = listOf(recordId), clientRecordIdsList = emptyList())
    }

    /** Total hydration volume, in mL, logged today (local calendar day) in Health Connect. */
    suspend fun readTodayTotalMl(): Int? {
        val c = client ?: return null
        val zone = ZoneId.systemDefault()
        val startOfDay = LocalDate.now(zone).atStartOfDay(zone).toInstant()
        val response = c.aggregate(
            AggregateRequest(
                metrics = setOf(HydrationRecord.VOLUME_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(startOfDay, Instant.now()),
            ),
        )
        return response[HydrationRecord.VOLUME_TOTAL]?.inMilliliters?.roundToInt() ?: 0
    }
}
