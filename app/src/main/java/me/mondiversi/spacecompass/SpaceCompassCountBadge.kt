package me.mondiversi.spacecompass

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.WindowCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference
import java.io.File
import org.json.JSONObject
import kotlin.math.roundToInt
import kotlin.random.Random

@Composable
internal fun SpaceCompassAnimatedCountBadge(
    countText: String?,
    indicatorColor: Color,
    containerColor: Color,
    contentColor: Color,
    syncInProgress: Boolean,
    pulseEnabled: Boolean = false,
    modifier: Modifier,
    fontSize: TextUnit,
    horizontalPadding: Dp,
    borderWidth: Dp = 1.8.dp,
    syncStrokeWidth: Dp = 2.dp,
    contentDescriptionText: String? = null,
    onClick: (() -> Unit)? = null,
    shadowElevation: Dp = 0.dp,
    tonalElevation: Dp = 0.dp
) {
    val pulseTransition =
        rememberInfiniteTransition(
            label = "countBadgePulse"
        )
    val borderAlpha by
        pulseTransition.animateFloat(
            initialValue = 0.24f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    animation =
                        tween(
                            durationMillis = 1200
                        ),
                    repeatMode =
                        RepeatMode.Reverse
                ),
            label = "countBadgeBorderAlpha"
        )
    val syncRotation =
        if (syncInProgress) {
            val syncTransition =
                rememberInfiniteTransition(
                    label = "countBadgeSync"
                )
            val angle by syncTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(900),
                        repeatMode = RepeatMode.Restart
                    ),
                label = "countBadgeSyncRotation"
            )
            angle
        } else {
            0f
        }

    val shape = RoundedCornerShape(50)
    val badgeContainerModifier =
        when {
            onClick != null && contentDescriptionText != null ->
                modifier
                    .clip(shape)
                    .clickable(onClick = onClick)
                    .spaceCompassAccessibleAction(
                        label = contentDescriptionText,
                        onClick = onClick
                    )
            onClick != null ->
                modifier
                    .clip(shape)
                    .clickable(onClick = onClick)
            contentDescriptionText != null ->
                modifier
                    .semantics {
                        contentDescription = contentDescriptionText
                    }
            else -> modifier
        }
    val badgeBorder =
        if (syncInProgress) {
            null
        } else {
            BorderStroke(
                width = borderWidth,
                color =
                    indicatorColor.copy(
                        alpha =
                            if (pulseEnabled) {
                                borderAlpha
                            } else {
                                1f
                            }
                    )
            )
        }
    Box(
        modifier = badgeContainerModifier,
        contentAlignment = Alignment.Center
    ) {
        val badgeModifier =
            Modifier
                .matchParentSize()
                .padding(
                    if (syncInProgress) 2.dp else 0.dp
                )

        Surface(
            modifier = badgeModifier,
            shape = shape,
            color = containerColor,
            contentColor = contentColor,
            border = badgeBorder,
            shadowElevation = shadowElevation,
            tonalElevation = tonalElevation
        ) {}

        countText?.let { text ->
            Text(
                modifier =
                    Modifier.padding(
                        horizontal = horizontalPadding
                    ),
                text = text,
                color = contentColor,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }

        if (syncInProgress) {
            Canvas(
                modifier = Modifier.matchParentSize()
            ) {
                val strokeWidth = syncStrokeWidth.toPx()
                val inset = strokeWidth / 2f
                val arcSize = Size(
                    width = size.width - strokeWidth,
                    height = size.height - strokeWidth
                )
                drawArc(
                    color = indicatorColor,
                    startAngle = syncRotation,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(
                        width = strokeWidth,
                        cap = StrokeCap.Round
                    )
                )
            }
        }
    }
}

