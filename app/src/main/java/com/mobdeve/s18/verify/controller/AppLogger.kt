package com.mobdeve.s18.verify.controller

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.mobdeve.s18.verify.app.VerifiApp
import com.mobdeve.s18.verify.model.Logs
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

object AppLogger {

    private val logQueue = mutableListOf<Logs>()
    private val logScope = CoroutineScope(Dispatchers.IO)
    private var isWorkerRunning = false

    @RequiresApi(Build.VERSION_CODES.O)
    fun d(tag: String, message: String) {
        Log.d(tag, message)
        enqueueLog(tag, message)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun e(tag: String, message: String) {
        Log.e(tag, message)
        enqueueLog(tag, message)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun i(tag: String, message: String) {
        Log.i(tag, message)
        enqueueLog(tag, message)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun w(tag: String, message: String) {
        Log.w(tag, message)
        enqueueLog(tag, message)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun enqueueLog(tag: String, message: String) {
        val app = VerifiApp.instance  // Assumes VerifiApp has a static instance reference
        val companyID = app.companyID ?: return

        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS"))

        val logEntry = Logs(
            id = UUID.randomUUID().toString(),
            companyID = companyID,
            tag = tag,
            date = timestamp,
            text = message.take(500) // Limit long messages
        )

        synchronized(logQueue) {
            logQueue.add(logEntry)
        }

        if (!isWorkerRunning) {
            flushQueue()
        }
    }

    private fun flushQueue() {
        isWorkerRunning = true
        logScope.launch {
            while (true) {
                val logsToSend = synchronized(logQueue) {
                    if (logQueue.isEmpty()) {
                        isWorkerRunning = false
                        return@launch
                    }
                    val toSend = logQueue.take(5)
                    logQueue.removeAll(toSend)
                    toSend
                }

                try {
                    val supabase = VerifiApp.instance.supabase
                    logsToSend.forEach {
                        supabase.postgrest["logs"].insert(it)
                    }
                } catch (e: Exception) {
                    Log.e("AppLogger", "Failed to insert logs: ${e.message}")
                    synchronized(logQueue) {
                        logQueue.addAll(0, logsToSend) // Put back if failed
                    }
                }

                delay(300L) // Throttle: avoid hammering server
            }
        }
    }
}