package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.MainActivity

class SecretCodeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val appContext = context ?: return
        if (intent?.action == "android.provider.Telephony.SECRET_CODE") {
            val launch = Intent(appContext, MainActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
                putExtra(EXTRA_OPEN_HIDDEN_VAULT, true)
            }
            appContext.startActivity(launch)
        }
    }

    companion object {
        const val EXTRA_OPEN_HIDDEN_VAULT = "extra_open_hidden_vault"
    }
}
