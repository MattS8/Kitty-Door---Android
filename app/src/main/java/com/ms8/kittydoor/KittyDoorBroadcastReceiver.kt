package com.ms8.kittydoor

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.util.Log
import java.lang.ref.WeakReference

class KittyDoorBroadcastReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive")
        val pendingResult = goAsync()
        val asyncTask = Task(pendingResult, intent, WeakReference(context))
        asyncTask.execute()
    }

    private class Task (private val pendingResult: BroadcastReceiver.PendingResult, private val intent: Intent, private val contextRef: WeakReference<Context>)
        : AsyncTask<String, Int, String>() {
        override fun doInBackground(vararg p0: String?): String {
            Log.d(TAG, "Handling Intent with action: ${intent.action}")
            when (intent.action) {
//                ACTION_CANCEL_AUTO_CLOSE -> FirebaseDatabaseFunctions
//                    .sendGarageAction(FirebaseDatabaseFunctions.ActionType.STOP_AUTO_CLOSE)
//                    .also { (contextRef.get()?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
//                        .cancel(notificationId) }
//                ACTION_CLOSE_DOOR -> FirebaseDatabaseFunctions
//                    .sendGarageAction(FirebaseDatabaseFunctions.ActionType.CLOSE)
//                ACTION_OPEN_DOOR -> FirebaseDatabaseFunctions
//                    .sendGarageAction(FirebaseDatabaseFunctions.ActionType.OPEN)
            }

            return toString()
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            pendingResult.finish()
        }
    }

    companion object {
        const val ACTION_CANCEL_AUTO_CLOSE = "com.ms8.kittydoor.actions.cancel_auto_close"
        const val ACTION_CLOSE_DOOR = "com.ms8.kittydoor.actions.close_garage"
        const val ACTION_OPEN_DOOR = "com.ms8.kittydoor.actions.open_garage"

        const val TAG = "KDBroadcastReceiver"
    }
}