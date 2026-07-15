package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.TrackerDatabase
import com.example.data.TrackerLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON" || 
            intent.action == "com.htc.intent.action.QUICKBOOT_POWERON") {
            
            Log.d("BootReceiver", "Device boot completed. Checking tracking status...")
            val appContext = context.applicationContext
            
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = TrackerDatabase.getDatabase(appContext)
                    val dao = db.trackerDao()
                    val config = dao.getPairingConfigDirect()
                    
                    if (config != null && config.isPaired) {
                        Log.d("BootReceiver", "Pairing configuration is active. Restarting background service...")
                        
                        val serviceIntent = Intent(appContext, TrackingService::class.java).apply {
                            action = TrackingService.ACTION_START
                        }
                        
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            appContext.startForegroundService(serviceIntent)
                        } else {
                            appContext.startService(serviceIntent)
                        }
                        
                        dao.insertLog(
                            TrackerLog(
                                type = "SYSTEM",
                                message = "BootReceiver: Automatically restarted background tracking service upon device reboot.",
                                status = "SUCCESS"
                            )
                        )
                    } else {
                        Log.d("BootReceiver", "No active pairing found. Skipping background service restart.")
                    }
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Failed to restart background service: ${e.localizedMessage}")
                }
            }
        }
    }
}
