package com.example.privacyclipboard

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.privacyclipboard.database.AppDatabase
import com.example.privacyclipboard.database.ClipboardEvent
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

class MonitorService : Service() {

    // CORREÇÃO 1: Handler para capturar erros fatais em corrotinas sem derrubar o app
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e("MonitorService", "Erro crítico em corrotina: ${throwable.message}")
    }

    // Escopo atualizado com o handler
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob() + exceptionHandler)

    private val pendingNotifications = ConcurrentHashMap<String, String>()
    private var notificationJob: Job? = null

    companion object {
        var isServiceActive = false
    }

    override fun onCreate() {
        super.onCreate()
        isServiceActive = true
        startForegroundService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isServiceActive = true
        // Reinicia o monitor se necessário, o escopo agora é seguro
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
        serviceScope.launch(Dispatchers.IO) {
            try {
                val database = AppDatabase.getDatabase(applicationContext)
                val historyText = if (type == "Bloqueado") "Tentou ler (Bloqueado)" else "Leu seu Clipboard"
                database.clipboardDao().insert(
                    ClipboardEvent(appName = appName, contentType = historyText)
                )
            } catch (e: Exception) {
                Log.e("MonitorService", "Falha ao salvar no banco: ${e.message}")
            }
        }

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

        // Separa apps bloqueados e aprovados
        val bloqueados = appsList.filter { pendingNotifications[it] == "Bloqueado" }
        val aprovados = appsList.filter { pendingNotifications[it] == "Leu" }

        val title = when {
            count == 1 && bloqueados.isNotEmpty() -> "Bloqueado: ${appsList[0]}"
            count == 1 && aprovados.isNotEmpty() -> "Acesso Permitido: ${appsList[0]}"
            else -> "Sentinela: $count Apps Detectados"
        }

        val inboxStyle = NotificationCompat.InboxStyle()

        // Mostra apps bloqueados primeiro
        bloqueados.forEach { app ->
            inboxStyle.addLine("• $app (Bloqueado)")
        }

        // Depois mostra apps que conseguiram ler (apps em primeiro plano/aprovados)
        aprovados.forEach { app ->
            inboxStyle.addLine("• $app (Acesso Permitido)")
        }

        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
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
        isServiceActive = false
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

        // CORREÇÃO 2: Especificar ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        // Obrigatório para Android 14+ (S25 Ultra) para evitar crash de SecurityException
        if (Build.VERSION.SDK_INT >= 34) { // Android 14 (Upside Down Cake)
            try {
                startForeground(
                    1,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } catch (e: Exception) {
                // Fallback caso o tipo não esteja no manifesto, tenta sem tipo (pode gerar erro no log, mas tenta manter vivo)
                startForeground(1, notification)
            }
        } else {
            startForeground(1, notification)
        }
    }
}