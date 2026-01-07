package com.example.privacyclipboard

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.privacyclipboard.database.AppDatabase
import com.example.privacyclipboard.database.ClipboardEvent
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

class MonitorService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val pendingNotifications = ConcurrentHashMap<String, String>()
    private var notificationJob: Job? = null

    // FONTE DA VERDADE PARA A UI
    companion object {
        var isServiceActive = false
    }

    override fun onCreate() {
        super.onCreate()
        isServiceActive = true // Marca como ativo ao criar
        startForegroundService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isServiceActive = true
        LogcatMonitor.start(serviceScope, this)
        observeEvents()
        return START_STICKY
    }

    private fun observeEvents() {
        serviceScope.launch {
            EventBus.events.collect { (appName, type) ->
                handleEvent(appName, type)
            }
        }
    }

    private fun handleEvent(appName: String, type: String) {
        // Salva no banco
        serviceScope.launch(Dispatchers.IO) {
            val database = AppDatabase.getDatabase(applicationContext)
            val historyText = if (type == "Bloqueado") "Tentou ler (Bloqueado)" else "Leu seu Clipboard"
            database.clipboardDao().insert(
                ClipboardEvent(appName = appName, contentType = historyText)
            )
        }

        // Agrupa notificações (Debounce de 1.5s)
        pendingNotifications[appName] = type

        if (notificationJob?.isActive != true) {
            notificationJob = serviceScope.launch {
                delay(1500)
                showGroupedNotification()
                pendingNotifications.clear()
            }
        }
    }

    private fun showGroupedNotification() {
        if (pendingNotifications.isEmpty()) return

        val manager = getSystemService(NotificationManager::class.java)
        val channelId = "privacy_alert_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Alertas de Privacidade",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Avisa quando um app acessa sua área de transferência"
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }

        val appsList = pendingNotifications.keys.toList()
        val count = appsList.size

        // Título mais impactante
        val title = if (count == 1) "Sentinela: ${appsList[0]}" else "Sentinela: $count Apps Detectados"

        val inboxStyle = NotificationCompat.InboxStyle()
        appsList.forEach { app ->
            val type = pendingNotifications[app]
            inboxStyle.addLine("• $app ($type)")
        }

        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_stat_eye)
            .setContentTitle(title)
            .setContentText("Toque para ver detalhes")
            .setStyle(inboxStyle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceActive = false // Marca como inativo ao destruir
        LogcatMonitor.stop()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundService() {
        val channelId = "monitor_service_core"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Monitoramento Ativo", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Sentinela Ativo")
            .setContentText("Vigiando seus dados copiados")
            .setSmallIcon(R.drawable.ic_stat_eye)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
    }
}