package com.app.inventarisbarang.utils

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class NotificationSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            AppNotificationManager.syncAndNotifyDirect(applicationContext)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
