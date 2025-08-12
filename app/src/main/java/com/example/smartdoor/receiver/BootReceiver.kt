package com.example.smartdoor.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.smartdoor.service.DoorMonitorService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val serviceIntent = Intent(context, DoorMonitorService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)  // For Android O and above
            } else {
                context.startService(serviceIntent)  // For below Android O
            }
        }
    }
}
