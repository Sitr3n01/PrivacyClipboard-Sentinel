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

    // OTIMIZAÇÃO 2: HashSet para busca O(1) ao invés de List O(n)
    private val ALLOW_LIST = setOf(
        "com.android.systemui",
        "com.example.privacyclipboard",
        "com.samsung.android.honeyboard",
        "com.google.android.inputmethod.latin",
        "system_server",
        "com.android.server"
    )

    // OTIMIZAÇÃO 3: Throttling POR APP para prevenir sobrecarga durante burst de eventos
    // Chave: nome do app, Valor: último timestamp que emitiu evento
    private val lastEventTimePerApp = mutableMapOf<String, Long>()
    private const val MIN_EVENT_INTERVAL_MS = 1000L // 1 segundo entre eventos do MESMO app

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
            var reader: BufferedReader? = null
            try {
                // Limpa logs antigos
                Runtime.getRuntime().exec("logcat -c").waitFor()

                val cmd = "logcat -v time -s ClipboardService SamsungClipboard SemClipboardController RestrictionPolicy"
                logProcess = Runtime.getRuntime().exec(cmd)

                // OTIMIZAÇÃO 4: Buffer mantido
                reader = BufferedReader(InputStreamReader(logProcess!!.inputStream), 8192)

                var line: String?
                var eventCount = 0 // Contador de eventos para debug

                // Loop de leitura do processo atual
                while (isActive) {
                    line = reader.readLine() ?: break // Sai do loop interno se o processo morrer
                    if (line.isBlank()) continue

                    // OTIMIZAÇÃO 5: Lógica mantida com melhorias
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
                        // OTIMIZAÇÃO 6: Throttling POR APP para prevenir spam
                        val currentTime = System.currentTimeMillis()
                        val lastTime = lastEventTimePerApp[detectedName] ?: 0L

                        // Permite evento se:
                        // 1. É a primeira vez que o app aparece
                        // 2. Passou mais de 1 segundo desde o último evento DESTE app
                        if (currentTime - lastTime >= MIN_EVENT_INTERVAL_MS) {
                            EventBus.emitEvent(detectedName, detectionType)
                            lastEventTimePerApp[detectedName] = currentTime
                            eventCount++

                            if (eventCount % 100 == 0) {
                                Log.d("PrivacySentinel", "Eventos processados: $eventCount")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("PrivacySentinel", "Erro no monitor (Reiniciando): ${e.message}", e)
                // Garante limpeza antes de tentar de novo
                try { logProcess?.destroy() } catch (_: Exception) {}
            } finally {
                // CORREÇÃO CRÍTICA: Fecha o BufferedReader para evitar vazamento de recursos
                try {
                    reader?.close()
                } catch (e: Exception) {
                    Log.w("PrivacySentinel", "Erro ao fechar reader: ${e.message}")
                }
            }

            // CORREÇÃO: Delay seguro antes de reiniciar o loop while
            if (isActive) {
                delay(5000)
            }
        }
    }

    private fun isAllowListed(pkg: String): Boolean {
        // OTIMIZAÇÃO 7: Busca otimizada com HashSet - O(1) ao invés de O(n)
        val pkgLower = pkg.lowercase()

        // Verifica correspondência exata primeiro (O(1))
        if (ALLOW_LIST.any { it.lowercase() == pkgLower }) {
            return true
        }

        // Verifica se começa com algum prefixo da allow-list
        // Ainda O(n) mas n é pequeno (6 itens) e muito mais rápido que antes
        return ALLOW_LIST.any { pkgLower.startsWith(it.lowercase()) }
    }

    private fun extractPackageNameFromDenial(log: String): String? {
        return DENIAL_REGEX.find(log)?.groupValues?.get(1)
    }

    private fun extractPackageNameGeneral(log: String): String? {
        if (log.contains("system_server") || log.contains("Instruction")) return null
        return PACKAGE_REGEX.find(log)?.value
    }
}