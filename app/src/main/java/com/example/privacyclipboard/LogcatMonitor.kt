package com.example.privacyclipboard

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

object LogcatMonitor {

    private var logJob: kotlinx.coroutines.Job? = null
    private var logProcess: Process? = null

    // Lista de apps do sistema que realmente devem ser ignorados.
    // REMOVIDO: "android" (era muito genérico e escondia com.twitter.android, Chrome, etc)
    private val ALLOW_LIST = listOf(
        "com.android.systemui",
        "com.example.privacyclipboard",
        "com.samsung.android.honeyboard", // Teclado Samsung
        "com.google.android.inputmethod.latin", // Gboard
        "system_server",
        "com.android.server"
    )

    fun start(scope: CoroutineScope, context: Context) {
        if (logJob?.isActive == true) return

        logJob = scope.launch(Dispatchers.IO) {
            monitorLogs(context)
        }
    }

    fun stop() {
        try {
            logProcess?.destroy()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        logJob?.cancel()
    }

    private suspend fun CoroutineScope.monitorLogs(context: Context) {
        Log.d("PrivacySentinel", "Monitor Iniciado (Modo ADB Otimizado)")

        try {
            Runtime.getRuntime().exec("logcat -c").waitFor()

            // Adicionado tags 'ClipboardService' e 'RestrictionPolicy' baseadas no seu log
            val cmd = "logcat -v time -s ClipboardService SamsungClipboard SemClipboardController RestrictionPolicy"
            logProcess = Runtime.getRuntime().exec(cmd)

            val reader = BufferedReader(InputStreamReader(logProcess!!.inputStream))
            var line: String?

            while (isActive) {
                line = reader.readLine() ?: break
                val currentLine = line ?: continue

                var detectedName: String? = null
                var detectionType = ""

                // Lógica ajustada para o seu log específico:
                // "Denying clipboard access to [pacote], application..."
                if (currentLine.contains("Denying clipboard access to", ignoreCase = true)) {
                    detectedName = extractPackageNameFromDenial(currentLine)
                    detectionType = "Bloqueado"
                }
                // Lógica padrão de leitura (Se o sistema permitir, o log é diferente)
                else if (currentLine.contains("Reading clipboard", ignoreCase = true) ||
                    currentLine.contains("getPrimaryClip", ignoreCase = true)) {
                    detectedName = extractPackageNameGeneral(currentLine)
                    detectionType = "Leu"
                }

                if (detectedName != null && !isAllowListed(detectedName)) {
                    Log.i("PrivacySentinel", "CAPTUREI: $detectedName ($detectionType)")
                    EventBus.emitEvent(detectedName, detectionType)
                }
            }
        } catch (e: Exception) {
            Log.e("PrivacySentinel", "Erro fatal no monitor: ${e.message}")
            kotlinx.coroutines.delay(5000)
            if (isActive) monitorLogs(context)
        }
    }

    private fun isAllowListed(pkg: String): Boolean {
        // Verifica se o pacote COMEÇA com ou é IGUAL a algo da lista,
        // em vez de "contains" genérico que pega coisa errada no meio da string.
        return ALLOW_LIST.any { allowed ->
            pkg.equals(allowed, ignoreCase = true) || pkg.startsWith(allowed, ignoreCase = true)
        }
    }

    // Extração específica para a linha de "Denying..." que você mandou
    private fun extractPackageNameFromDenial(log: String): String? {
        // Padrão: "access to com.package.name,"
        val pattern = Regex("access to ([a-zA-Z0-9_.]+)(?:,)?")
        val match = pattern.find(log)
        return match?.groupValues?.get(1)
    }

    private fun extractPackageNameGeneral(log: String): String? {
        val packagePattern = Regex("([a-zA-Z0-9_]+\\.[a-zA-Z0-9_]+\\.[a-zA-Z0-9_.]+)")
        // Filtra ruídos comuns
        if (log.contains("system_server") || log.contains("Instruction")) return null
        val match = packagePattern.find(log)
        return match?.value
    }
}