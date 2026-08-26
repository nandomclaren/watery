package com.nandomclaren.watery.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.nandomclaren.watery.data.HealthConnectManager
import com.nandomclaren.watery.data.WaterPreferencesRepository
import com.nandomclaren.watery.data.WaterUiState
import kotlin.math.roundToInt

private val DarkBackground = Color(0xFF1C1C1E)
private val BadgeRed = Color(0xFFE0342F)
private val CupOutline = Color(0x33FFFFFF)
private val CupFill = Color(0xFF2FA8E0)
private val White = Color(0xFFFFFFFF)
private val MutedWhite = Color(0xB3FFFFFF)
private val ButtonTrack = Color(0x1FFFFFFF)

/** This widget always renders with a fixed dark palette, so day and night resolve the same. */
private fun solid(color: Color) = ColorProvider(day = color, night = color)

class WaterWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = WaterPreferencesRepository(context)
        val healthConnectManager = HealthConnectManager(context)
        val result = try {
            Result.success(repository.syncAndGetState(healthConnectManager, allowHealthConnectRead = false))
        } catch (e: Exception) {
            Result.failure(e)
        }

        provideContent {
            GlanceTheme {
                result.fold(
                    onSuccess = { WaterWidgetContent(it) },
                    onFailure = { WaterWidgetError(it) },
                )
            }
        }
    }
}

@Composable
private fun WaterWidgetError(error: Throwable) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(solid(DarkBackground))
            .cornerRadius(24.dp)
            .padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Erro: ${error::class.simpleName}: ${error.message}",
            style = TextStyle(color = solid(White), fontSize = 11.sp),
        )
    }
}

@Composable
private fun WaterWidgetContent(state: WaterUiState) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(solid(DarkBackground))
            .cornerRadius(24.dp)
            .padding(16.dp)
            .clickable(actionRunCallback<AddGlassAction>()),
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "H2O",
                    style = TextStyle(
                        color = solid(White),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                Box(
                    modifier = GlanceModifier
                        .background(solid(BadgeRed))
                        .cornerRadius(12.dp)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "${state.drunkGlasses}/${state.goalGlasses}",
                        style = TextStyle(
                            color = solid(White),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
            }

            Spacer(modifier = GlanceModifier.defaultWeight())

            Box(
                modifier = GlanceModifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                WaterCup(drunkGlasses = state.drunkGlasses, goalGlasses = state.goalGlasses)
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            Box(
                modifier = GlanceModifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = formatLiters(state.drunkLiters),
                    style = TextStyle(
                        color = solid(White),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }

            Spacer(modifier = GlanceModifier.defaultWeight())

            Row(modifier = GlanceModifier.fillMaxWidth()) {
                Box(
                    modifier = GlanceModifier
                        .size(28.dp)
                        .background(solid(ButtonTrack))
                        .cornerRadius(14.dp)
                        .clickable(actionRunCallback<UndoGlassAction>()),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "↺",
                        style = TextStyle(color = solid(MutedWhite), fontSize = 14.sp),
                    )
                }
                Spacer(modifier = GlanceModifier.defaultWeight())
                Box(
                    modifier = GlanceModifier
                        .size(28.dp)
                        .background(solid(CupFill))
                        .cornerRadius(14.dp)
                        .clickable(actionRunCallback<AddGlassAction>()),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "+",
                        style = TextStyle(
                            color = solid(White),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
            }
        }
    }
}

private const val CUP_MAX_SEGMENTS = 10

@Composable
private fun WaterCup(drunkGlasses: Int, goalGlasses: Int) {
    val segments = goalGlasses.coerceIn(1, CUP_MAX_SEGMENTS)
    val filledSegments = if (goalGlasses <= CUP_MAX_SEGMENTS) {
        drunkGlasses.coerceIn(0, segments)
    } else {
        ((drunkGlasses.toFloat() / goalGlasses.toFloat()) * segments)
            .roundToInt()
            .coerceIn(0, segments)
    }

    Box(
        modifier = GlanceModifier
            .width(56.dp)
            .height(72.dp)
            .background(solid(CupOutline))
            .cornerRadius(10.dp)
            .padding(3.dp),
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            for (index in 0 until segments) {
                val isFilled = index >= segments - filledSegments
                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .defaultWeight()
                        .padding(vertical = 1.dp)
                        .background(solid(if (isFilled) CupFill else Color.Transparent))
                        .cornerRadius(4.dp),
                ) {}
            }
        }
    }
}

private fun formatLiters(liters: Double): String {
    val rounded = Math.round(liters * 10.0) / 10.0
    val text = if (rounded == rounded.toLong().toDouble()) {
        rounded.toLong().toString()
    } else {
        rounded.toString()
    }
    return "${text}L"
}
