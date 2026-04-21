package com.application.polarapplication.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.application.polarapplication.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class WorkoutForegroundService : Service() {

    // ─────────────────────────────────────────────
    // CONSTANTS
    // ─────────────────────────────────────────────
    companion object {
        const val CHANNEL_ID = "workout_channel"
        const val NOTIFICATION_ID = 1001

        // Intent actions
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESUME = "ACTION_RESUME"

        // Intent extras
        const val EXTRA_HEART_RATE = "EXTRA_HEART_RATE"
        const val EXTRA_CALORIES = "EXTRA_CALORIES"
        const val EXTRA_WORKOUT_TYPE = "EXTRA_WORKOUT_TYPE"

        fun startIntent(context: Context, workoutType: String): Intent =
            Intent(context, WorkoutForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_WORKOUT_TYPE, workoutType)
            }

        fun stopIntent(context: Context): Intent =
            Intent(context, WorkoutForegroundService::class.java).apply {
                action = ACTION_STOP
            }

        fun updateIntent(context: Context, heartRate: Int, calories: Int): Intent =
            Intent(context, WorkoutForegroundService::class.java).apply {
                action = ACTION_START // reuse start to update
                putExtra(EXTRA_HEART_RATE, heartRate)
                putExtra(EXTRA_CALORIES, calories)
            }
    }

    // ─────────────────────────────────────────────
    // STATE
    // ─────────────────────────────────────────────
    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused

    private var heartRate = 0
    private var calories = 0
    private var workoutType = "WORKOUT"

    private var timerJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Default)

    private val binder = WorkoutBinder()

    inner class WorkoutBinder : Binder() {
        fun getService(): WorkoutForegroundService = this@WorkoutForegroundService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    // ─────────────────────────────────────────────
    // LIFECYCLE
    // ─────────────────────────────────────────────
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                workoutType = intent.getStringExtra(EXTRA_WORKOUT_TYPE) ?: workoutType
                heartRate = intent.getIntExtra(EXTRA_HEART_RATE, heartRate)
                calories = intent.getIntExtra(EXTRA_CALORIES, calories)

                if (timerJob == null) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        startForeground(
                            NOTIFICATION_ID,
                            buildNotification(),
                            android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                        )
                    } else {
                        startForeground(NOTIFICATION_ID, buildNotification())
                    }
                    startTimer()
                } else {
                    // Just update the notification with new vitals
                    updateNotification()
                }
            }
            ACTION_STOP -> {
                stopWorkout()
            }
            ACTION_PAUSE -> {
                _isPaused.value = true
                timerJob?.cancel()
                timerJob = null
                updateNotification()
            }
            ACTION_RESUME -> {
                _isPaused.value = false
                startTimer()
                updateNotification()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        timerJob?.cancel()
        super.onDestroy()
    }

    // ─────────────────────────────────────────────
    // TIMER
    // ─────────────────────────────────────────────
    private fun startTimer() {
        timerJob = serviceScope.launch {
            while (true) {
                delay(1000)
                if (!_isPaused.value) {
                    _elapsedSeconds.value++
                    updateNotification()
                }
            }
        }
    }

    private fun stopWorkout() {
        timerJob?.cancel()
        timerJob = null
        _elapsedSeconds.value = 0L
        _isPaused.value = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // ─────────────────────────────────────────────
    // UPDATE VITALS (called from ViewModel)
    // ─────────────────────────────────────────────
    fun updateVitals(newHeartRate: Int, newCalories: Int) {
        heartRate = newHeartRate
        calories = newCalories
        updateNotification()
    }

    // ─────────────────────────────────────────────
    // NOTIFICATION
    // ─────────────────────────────────────────────
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Active Workout",
            NotificationManager.IMPORTANCE_LOW // LOW = no sound, persistent
        ).apply {
            description = "Shows live workout data during an active session"
            setShowBadge(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val elapsed = _elapsedSeconds.value
        val hours = elapsed / 3600
        val minutes = (elapsed % 3600) / 60
        val seconds = elapsed % 60
        val timeString = if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }

        val hrText = if (heartRate > 0) "$heartRate BPM" else "— BPM"
        val calText = "$calories kcal"

        val isPausedNow = _isPaused.value

        val openAppIntent = Intent(
            Intent.ACTION_VIEW,
            android.net.Uri.parse("polar://active_workout")
        ).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Stop action
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, WorkoutForegroundService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Pause / Resume action
        val pauseResumeIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, WorkoutForegroundService::class.java).apply {
                action = if (isPausedNow) ACTION_RESUME else ACTION_PAUSE
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // ← adaugă ic_notification.xml în drawable
            .setContentTitle("$workoutType · $timeString")
            .setContentText("❤ $hrText   🔥 $calText")
            .setContentIntent(openPendingIntent)
            .setOngoing(true) // nu poate fi ștearsă de user
            .setOnlyAlertOnce(true) // nu sună la fiecare update
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .addAction(
                if (isPausedNow) {
                    android.R.drawable.ic_media_play
                } else {
                    android.R.drawable.ic_media_pause
                },
                if (isPausedNow) "Resume" else "Pause",
                pauseResumeIntent
            )
            .addAction(
                android.R.drawable.ic_delete,
                "Stop",
                stopIntent
            )
            .build()
    }

    private fun updateNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification())
    }
}
