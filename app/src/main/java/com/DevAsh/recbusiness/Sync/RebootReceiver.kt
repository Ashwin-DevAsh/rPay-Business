package com.DevAsh.recbusiness.Sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat.startForegroundService

class RebootReceiver :BroadcastReceiver(){
    override fun onReceive(context: Context?, intent: Intent?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(context!!,Intent(context,SocketService::class.java))
        } else {
           context?.startService(Intent(context,SocketService::class.java))
        }
    }
}