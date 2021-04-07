package com.ms8.kittydoor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FirebaseMessageService : FirebaseMessagingService() {
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        createNotificationChannel(applicationContext)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Log.d(TAG, "From: " + message.from)
        if (message.data.isNotEmpty())
            Log.d(TAG, "Message data payload: ${message.data}")
        else
            Log.d(TAG, "No message data received.").also { return }

        when (message.data["type"]) {
//            STATUS_UPDATE -> handleStatusUpdate(message.data)
//            AUTO_CLOSE_WARNING -> handleAutoCloseWarning()
        }
    }

    private fun handleStatusUpdate(data: MutableMap<String, String>) {
        val newStatus = data["status_type"] ?: ""
        Log.d(TAG, "newStatus = $newStatus")

        applicationContext?.let { context ->
//            val appWidgetManager = AppWidgetManager.getInstance(context)
//            val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, GarageWidget::class.java))
//            val updateIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
//                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
//                putExtra(GarageWidget.EXTRA_NEW_STATUS, newStatus)
//            }
//            Log.d(TAG, "Sending broadcast...")
//            context.sendBroadcast(updateIntent)
        }
    }

    private fun handleAutoCloseWarning() {
        applicationContext.let { c ->

            if (AppState.appData.appInForeground){
                Log.d(TAG, "Handling in-app")
                Handler(Looper.getMainLooper()).post {
                    //AppState.garageData.autoCloseWarning.set(FirebaseDatabaseFunctions.AutoCloseWarning(0, 0))
                }
                return
            }

            Log.d(TAG, "Sending notification")

//            val channelId = getString(R.string.default_notification_id)
//            val cancelAutoCloseIntent = Intent(c, KittyDoorBroadcastReceiver::class.java).apply {
//                action = KittyDoorBroadcastReceiver.ACTION_CANCEL_AUTO_CLOSE
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
//                    putExtra(Notification.EXTRA_NOTIFICATION_ID, channelId)
//            }
//            val cancelAutoClosePendingIntent = PendingIntent.getBroadcast(c, 0, cancelAutoCloseIntent, 0)
//            val newIntent = Intent(c, MainActivity::class.java)
//                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
//            val builder = NotificationCompat.Builder(c, channelId)
//                .setSmallIcon(R.mipmap.ic_launcher_round)
//                .setContentTitle(getString(R.string.auto_close_warning_title))
//                .setContentText(getString(R.string.auto_close_warning_desc))
//                .setStyle(NotificationCompat.BigTextStyle().bigText(getString(R.string.auto_close_warning_desc)))
//                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
//                .setContentIntent(PendingIntent.getBroadcast(c, 0, newIntent, 0))
//                .setAutoCancel(true)
//                .addAction(R.drawable.ic_clear_black_24dp, getString(R.string.cancel_auto_close), cancelAutoClosePendingIntent)
//
//
//            // Create the NotificationChannel, but only on API 26+ because
//            // the NotificationChannel class is new and not in the support library
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                val name = c.getString(R.string.channel_name)
//                val descriptionText = c.getString(R.string.channel_description)
//                val importance = NotificationManager.IMPORTANCE_DEFAULT
//                val channel = NotificationChannel(channelId, name, importance).apply {
//                    description = descriptionText
//                }
//                // Register the channel with the system
//                val notificationManager: NotificationManager = c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//                notificationManager.createNotificationChannel(channel)
//                builder.setChannelId(channelId)
//            }
//
//            NotificationManagerCompat.from(c).notify(notificationId, builder.build())
        }
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")

        FirebaseDBF.addTokenToGarage(token)
    }

    companion object {
        const val TAG = "FirebaseMessageService"

        const val NOTIFICATION_TYPE = "notification_type"

        const val TYPE_AUTO_CLOSE_WARNING = "Auto Close Warning"

        const val notificationId = 8888

        fun createNotificationChannel(context: Context) {
            // Create the NotificationChannel, but only on API 26+ because
            // the NotificationChannel class is new and not in the support library
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val name = context.getString(R.string.channel_name)
                val descriptionText = context.getString(R.string.channel_description)
                val importance = NotificationManager.IMPORTANCE_DEFAULT
                val channelId = context.getString(R.string.default_notification_id)
                val channel = NotificationChannel(channelId, name, importance).apply {
                    description = descriptionText
                }
                // Register the channel with the system
                val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }
    }
}