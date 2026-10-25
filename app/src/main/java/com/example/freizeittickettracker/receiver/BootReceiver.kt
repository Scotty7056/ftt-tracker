package com.example.freizeittickettracker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val manager = AppGeofenceManager(context)
            manager.registerAllVenues()
        }
    }
}