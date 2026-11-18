package com.app.inventarisbarang.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.app.inventarisbarang.R
import com.app.inventarisbarang.model.Notifikasi
import com.app.inventarisbarang.network.ApiConfig
import com.app.inventarisbarang.ui.peminjaman.DetailPeminjamanActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

object AppNotificationManager {

    // Menggunakan channel v3 dengan konfigurasi audio & getar eksplisit agar HP fisik tidak mengabaikan suara
    private const val CHANNEL_ID = "channel_inventaris_transaksi_v3"
    private const val CHANNEL_NAME = "Transaksi & Operasional Inventaris"
    private const val PREFS_NOTIFIED = "prefs_notified_notifications"
    private const val KEY_NOTIFIED_IDS = "notified_ids"

    /**
     * Inisialisasi Notification Channel untuk Android 8.0+ (Oreo) ke atas
     * dengan tingkat kepentingan TINGGI (Heads-up banner, suara eksplisit, dan getar).
     */
    fun initChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi permohonan baru, persetujuan pinjam, dan pengingat batas waktu"
                enableLights(true)
                lightColor = Color.BLUE
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
                setSound(soundUri, audioAttributes)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Menampilkan notifikasi banner di Status Bar HP Android dengan suara & getar
     */
    fun showNotification(context: Context, notif: Notifikasi) {
        initChannel(context)

        // Cek izin notifikasi di Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val intent = if (notif.peminjaman_id != null && notif.peminjaman_id > 0) {
            Intent(context, DetailPeminjamanActivity::class.java).apply {
                putExtra("peminjaman_id", notif.peminjaman_id)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        } else {
            context.packageManager.getLaunchIntentForPackage(context.packageName)
        }

        val requestCode = notif.id.hashCode()
        val pendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val subText = when (notif.tipe) {
            "peminjaman_baru" -> "Pengajuan Baru"
            "peminjaman_disetujui" -> "Disetujui"
            "peringatan_tempo" -> "Batas Waktu"
            "terlambat" -> "Peringatan"
            else -> "Inventaris"
        }

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(notif.judul)
            .setContentText(notif.pesan)
            .setSubText(subText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notif.pesan))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setSound(soundUri)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setColor(ContextCompat.getColor(context, R.color.primary))
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 300, 200, 300))

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            val notifId = (notif.peminjaman_id ?: 0) + (notif.tipe.hashCode().let { if (it < 0) -it else it } % 10000)
            notificationManager.notify(notifId, builder.build())
        } catch (_: SecurityException) {
            // Izin belum diizinkan oleh user
        }
    }

    /**
     * Memeriksa notifikasi langsung secara suspend (bisa dipanggil oleh Coroutine / WorkManager)
     */
    suspend fun syncAndNotifyDirect(context: Context, forceNotifyFirst: Boolean = false) {
        initChannel(context)

        withContext(Dispatchers.IO) {
            try {
                val apiService = ApiConfig.getApiService(context)
                val response = apiService.getNotifikasi()

                if (response.isSuccessful) {
                    val notifList = response.body()?.data ?: emptyList()
                    val prefs = context.getSharedPreferences(PREFS_NOTIFIED, Context.MODE_PRIVATE)
                    val notifiedSet = prefs.getStringSet(KEY_NOTIFIED_IDS, mutableSetOf())?.toMutableSet() ?: mutableSetOf()

                    var hasNew = false
                    for (notif in notifList) {
                        if (forceNotifyFirst || !notifiedSet.contains(notif.id)) {
                            withContext(Dispatchers.Main) {
                                showNotification(context, notif)
                            }
                            notifiedSet.add(notif.id)
                            hasNew = true
                        }
                    }

                    if (hasNew) {
                        prefs.edit().putStringSet(KEY_NOTIFIED_IDS, notifiedSet).apply()
                    }
                }
            } catch (_: Exception) {
                // Abaikan jika offline / koneksi timeout
            }
        }
    }

    /**
     * Memeriksa notifikasi dari server secara asynchronous
     */
    fun syncAndNotify(context: Context, forceNotifyFirst: Boolean = false) {
        initChannel(context)

        val scope = CoroutineScope(Dispatchers.Main)
        scope.launch {
            syncAndNotifyDirect(context, forceNotifyFirst)
        }
    }

    /**
     * Menjalankan Background Worker berkala agar notifikasi tetap dicek
     * dan dibunyikan meskipun aplikasi sedang ditutup / di luar aplikasi.
     */
    fun scheduleBackgroundSync(context: Context) {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncWorkRequest = PeriodicWorkRequestBuilder<NotificationSyncWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "inventaris_notification_sync",
                ExistingPeriodicWorkPolicy.UPDATE,
                syncWorkRequest
            )
        } catch (_: Exception) {
            // Abaikan jika WorkManager belum siap
        }
    }
}
