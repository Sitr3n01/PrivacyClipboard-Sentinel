package com.example.privacyclipboard

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.io.BufferedReader
import java.io.InputStreamReader

object LogcatMonitor {

    private var logJob: kotlinx.coroutines.Job? = null
    private var logProcess: Process? = null

    // OTIMIZAÇÃO 1: Regex compilados estaticamente (Mantido)
    private val DENIAL_REGEX = Regex("access to ([a-zA-Z0-9_.]+)(?:,)?")
    private val PACKAGE_REGEX = Regex("([a-zA-Z0-9_]+\\.[a-zA-Z0-9_]+\\.[a-zA-Z0-9_.]+)")

    private val ALLOW_LIST = listOf(
        "com.android.systemui",
        "com.example.privacyclipboard",
        "com.samsung.android.honeyboard",
        "com.google.android.inputmethod.latin",
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

        // CORREÇÃO CRÍTICA: Loop externo substitui a recursão.
        // Isso impede que a pilha de memória estoure se o logcat falhar repetidamente.
        while (isActive) {
            try {
                // Limpa logs antigos
                Runtime.getRuntime().exec("logcat -c").waitFor()

                val cmd = "logcat -v time -s ClipboardService SamsungClipboard SemClipboardController RestrictionPolicy"
                logProcess = Runtime.getRuntime().exec(cmd)

                // OTIMIZAÇÃO 2: Buffer mantido
                val reader = BufferedReader(InputStreamReader(logProcess!!.inputStream), 8192)

                var line: String?

                // Loop de leitura do processo atual
                while (isActive) {
                    line = reader.readLine() ?: break // Sai do loop interno se o processo morrer
                    if (line.isBlank()) continue

                    // OTIMIZAÇÃO 3: Lógica mantida
                    var detectedName: String? = null
                    var detectionType = ""

                    if (line.contains("Denying clipboard access to", ignoreCase = true)) {
                        detectedName = extractPackageNameFromDenial(line)
                        detectionType = "Bloqueado"
                    } else if (line.contains("Reading clipboard", ignoreCase = true) ||
                        line.contains("getPrimaryClip", ignoreCase = true)) {
                        detectedName = extractPackageNameGeneral(line)
                        detectionType = "Leu"
                    }

                    if (detectedName != null && !isAllowListed(detectedName)) {
                        EventBus.emitEvent(detectedName, detectionType)
                    }
                }
            } catch (e: Exception) {
                Log.e("PrivacySentinel", "Erro no monitor (Reiniciando): ${e.message}")
                // Garante limpeza antes de tentar de novo
                try { logProcess?.destroy() } catch (_: Exception) {}
            }

            // CORREÇÃO: Delay seguro antes de reiniciar o loop while
            if (isActive) {
                delay(5000)
            }
        }
    }

    private fun isAllowListed(pkg: String): Boolean {
        return ALLOW_LIST.any { allowed ->
            pkg.equals(allowed, ignoreCase = true) || pkg.startsWith(allowed, ignoreCase = true)
        }
    }

    private fun extractPackageNameFromDenial(log: String): String? {
        return DENIAL_REGEX.find(log)?.groupValues?.get(1)
    }

    private fun extractPackageNameGeneral(log: String): String? {
        if (log.contains("system_server") || log.contains("Instruction")) return null
        return PACKAGE_REGEX.find(log)?.value
    }
}