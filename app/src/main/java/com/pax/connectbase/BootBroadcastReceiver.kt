package com.pax.connectbase

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootBroadcastReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "BootBroadcastReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.w(TAG, "onReceive()" + intent?.action)
        if (intent?.action == "paydroid.intent.action.BOOT_COMPLETED") {
            Log.w(TAG, "Boot broadcast receiver")
            context?.let {
                val launchIntent = Intent(it, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                it.startActivity(launchIntent)
            }
        }
    }


}